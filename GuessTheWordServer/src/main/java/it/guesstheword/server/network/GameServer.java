package it.guesstheword.server.network;

import it.guesstheword.common.model.Giocatore;
import it.guesstheword.common.model.Partita;
import it.guesstheword.common.network.EsitoSfida;
import it.guesstheword.common.network.Messaggio;
import it.guesstheword.common.network.Sfida;
import it.guesstheword.common.network.TipoMessaggio;
import it.guesstheword.server.core.GestoreAutenticazione;
import it.guesstheword.server.core.GestoreSfida;
import it.guesstheword.server.dao.PartitaDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Server di gioco: accetta le connessioni dei client, coordina
 * l'autenticazione, il matchmaking e l'intero ciclo di vita di una partita
 * (preparazione sfida, raccolta risposte, determinazione del vincitore,
 * notifica e registrazione dell'esito).
 *
 * <p>Vincolo del modello: in qualsiasi istante puo' esserci una sola partita in
 * corso. Le transizioni di stato della partita sono serializzate tramite la
 * sincronizzazione sui metodi dell'istanza.</p>
 */
public class GameServer {

    private final int porta;
    private final GestoreAutenticazione gestoreAutenticazione;
    private final GestoreSfida gestoreSfida;
    private final PartitaDAO partitaDAO;

    private ServerSocket serverSocket;
    private volatile boolean inEsecuzione;
    private Thread acceptThread;

    private final ExecutorService poolHandler = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** Tutti i client connessi (anche non autenticati). */
    private final Set<ClientHandler> connessi = ConcurrentHashMap.newKeySet();
    /** Sessioni autenticate: username &rarr; handler (per evitare doppi login). */
    private final Map<String, ClientHandler> sessioniAttive = new ConcurrentHashMap<>();
    /** Giocatori in attesa di un avversario. */
    private final Queue<ClientHandler> inAttesa = new LinkedList<>();

    /* ---- stato della partita corrente (protetto da synchronized) ---- */
    private boolean partitaInCorso;
    private ClientHandler giocatore1;
    private ClientHandler giocatore2;
    private Sfida sfidaCorrente;
    private long inizioPartitaNanos;
    private ScheduledFuture<?> timeoutFuture;

    /* ---- callback verso l'interfaccia amministratore ---- */
    private Consumer<String> logListener;
    private IntConsumer countListener;
    private final List<String> bufferLog = new ArrayList<>();

    private static final DateTimeFormatter ORA_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public GameServer(int porta, GestoreAutenticazione gestoreAutenticazione,
                      GestoreSfida gestoreSfida, PartitaDAO partitaDAO) {
        this.porta = porta;
        this.gestoreAutenticazione = gestoreAutenticazione;
        this.gestoreSfida = gestoreSfida;
        this.partitaDAO = partitaDAO;
    }

    /* ============================ avvio / stop ============================ */

    /**
     * Avvia il server: apre il {@link ServerSocket} ed inizia ad accettare
     * connessioni su un thread dedicato.
     *
     * @throws IOException se la porta non e' disponibile
     */
    public void avvia() throws IOException {
        serverSocket = new ServerSocket(porta);
        inEsecuzione = true;
        acceptThread = new Thread(this::cicloAccept, "accept-loop");
        acceptThread.setDaemon(true);
        acceptThread.start();
        log("Server avviato sulla porta " + porta + ".");
    }

    private void cicloAccept() {
        while (inEsecuzione) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                connessi.add(handler);
                poolHandler.submit(handler);
                log("Nuova connessione da " + socket.getInetAddress().getHostAddress() + ".");
            } catch (IOException e) {
                if (inEsecuzione) {
                    log("Errore nell'accettare una connessione: " + e.getMessage());
                }
                // se !inEsecuzione il socket e' stato chiuso volontariamente
            }
        }
    }

    /** Ferma il server e libera tutte le risorse. */
    public void ferma() {
        inEsecuzione = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        for (ClientHandler h : connessi) {
            h.chiudi();
        }
        poolHandler.shutdownNow();
        scheduler.shutdownNow();
        log("Server arrestato.");
    }

    /* ======================= autenticazione ======================= */

    /** Gestisce la richiesta di login di un client (UC-02). */
    public void gestisciLogin(ClientHandler handler, String username, String password) {
        Giocatore g = gestoreAutenticazione.login(username, password);
        if (g == null) {
            handler.invia(new Messaggio(TipoMessaggio.LOGIN_FALLITO, "Credenziali non valide"));
            return;
        }
        synchronized (this) {
            if (sessioniAttive.containsKey(g.getUsername())) {
                handler.invia(new Messaggio(TipoMessaggio.LOGIN_FALLITO, "Utente gia' connesso"));
                return;
            }
            sessioniAttive.put(g.getUsername(), handler);
            handler.setGiocatore(g);
            aggiornaConteggio();
        }
        handler.invia(new Messaggio(TipoMessaggio.LOGIN_OK, g));
        log("Login di '" + g.getUsername() + "'.");
    }

    /** Gestisce la richiesta di registrazione di un client (UC-01). */
    public void gestisciRegistrazione(ClientHandler handler, String username, String password) {
        boolean ok = gestoreAutenticazione.registra(username, password);
        if (ok) {
            handler.invia(new Messaggio(TipoMessaggio.REGISTRAZIONE_OK));
            log("Nuovo giocatore registrato: '" + username + "'.");
        } else {
            handler.invia(new Messaggio(TipoMessaggio.REGISTRAZIONE_FALLITA, "Username gia' in uso o non valido"));
        }
    }

    private synchronized void rimuoviSessione(ClientHandler handler) {
        if (handler.getGiocatore() != null) {
            sessioniAttive.remove(handler.getGiocatore().getUsername(), handler);
            aggiornaConteggio();
        }
    }

    /* ========================== matchmaking ========================== */

    /** Inserisce un client nella coda di attesa ed eventualmente avvia una partita (UC-03). */
    public synchronized void aggiungiInAttesa(ClientHandler handler) {
        if (!handler.isAutenticato()) {
            return;
        }
        if (handler == giocatore1 || handler == giocatore2 || inAttesa.contains(handler)) {
            return; // gia' in gioco o gia' in coda
        }
        inAttesa.add(handler);
        handler.invia(new Messaggio(TipoMessaggio.IN_ATTESA));
        log("'" + handler.getGiocatore().getUsername() + "' e' in attesa di un avversario.");
        verificaCodaAttesa();
    }

    /** Rimuove un client dalla coda di attesa (annullamento ricerca). */
    public synchronized void rimuoviDaAttesa(ClientHandler handler) {
        inAttesa.remove(handler);
    }

    private synchronized void verificaCodaAttesa() {
        if (!partitaInCorso && inAttesa.size() >= 2) {
            ClientHandler h1 = inAttesa.poll();
            ClientHandler h2 = inAttesa.poll();
            avviaPartita(h1, h2);
        }
    }

    /* ============================ partita ============================ */

    private synchronized void avviaPartita(ClientHandler h1, ClientHandler h2) {
        partitaInCorso = true;
        giocatore1 = h1;
        giocatore2 = h2;

        try {
            sfidaCorrente = gestoreSfida.preparaSfida();
        } catch (IllegalStateException e) {
            // Nessun documento analizzato disponibile (UC-12, alt. 1a)
            Messaggio errore = new Messaggio(TipoMessaggio.ERRORE,
                    "Impossibile avviare la partita: nessun documento analizzato sul server.");
            h1.invia(errore);
            h2.invia(errore);
            log("Partita non avviata: " + e.getMessage());
            azzeraStatoPartita();
            return;
        }

        if (h1.getGiocatore() != null) h1.getGiocatore().setInPartita(true);
        if (h2.getGiocatore() != null) h2.getGiocatore().setInPartita(true);

        inizioPartitaNanos = System.nanoTime();
        Messaggio inizio = new Messaggio(TipoMessaggio.INIZIO_PARTITA, sfidaCorrente);
        h1.invia(inizio);
        h2.invia(inizio);

        // timeout autorevole lato server
        int durata = sfidaCorrente.getDurataSecondi();
        timeoutFuture = scheduler.schedule(this::timeoutPartita, durata, TimeUnit.SECONDS);

        log("Partita avviata tra '" + nome(h1) + "' e '" + nome(h2)
                + "' (soluzione: " + sfidaCorrente.getParolaOriginale()
                + ", shift: " + sfidaCorrente.getShift() + ").");
    }

    /** Gestisce una risposta ricevuta da un client durante la partita (UC-05). */
    public synchronized void rispostaRicevuta(ClientHandler handler, String risposta) {
        if (!partitaInCorso || (handler != giocatore1 && handler != giocatore2)) {
            return;
        }
        if (gestoreSfida.validaRisposta(sfidaCorrente, risposta)) {
            long durataSec = Math.max(1, Math.round((System.nanoTime() - inizioPartitaNanos) / 1_000_000_000.0));
            log("'" + nome(handler) + "' ha risposto correttamente (" + risposta + ").");
            concludiPartita(handler, false, (int) durataSec);
        } else {
            // Risposta errata: nessuna notifica, l'altro giocatore puo' ancora rispondere.
            log("'" + nome(handler) + "' ha risposto in modo errato (" + risposta + ").");
        }
    }

    /** Invocato dallo scheduler alla scadenza del timer (UC-05 alt. 4a). */
    private synchronized void timeoutPartita() {
        if (partitaInCorso) {
            log("Tempo scaduto: nessun vincitore.");
            concludiPartita(null, true, sfidaCorrente != null ? sfidaCorrente.getDurataSecondi() : 0);
        }
    }

    /**
     * Conclude la partita: notifica l'esito ad entrambi i client, registra il
     * risultato nel database e ripristina lo stato (UC-06, UC-13).
     */
    private synchronized void concludiPartita(ClientHandler vincitore, boolean tempoScaduto, int durata) {
        if (!partitaInCorso) {
            return;
        }
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }

        String parolaOriginale = sfidaCorrente != null ? sfidaCorrente.getParolaOriginale() : null;
        String usernameVincitore = vincitore != null ? nome(vincitore) : null;

        String messaggioEsito;
        if (tempoScaduto || vincitore == null) {
            messaggioEsito = "Tempo scaduto: nessun vincitore. La parola era '" + parolaOriginale + "'.";
        } else {
            messaggioEsito = "Ha vinto " + usernameVincitore + "! La parola era '" + parolaOriginale + "'.";
        }

        EsitoSfida esito = new EsitoSfida(usernameVincitore, parolaOriginale, tempoScaduto, messaggioEsito);
        Messaggio messaggio = new Messaggio(TipoMessaggio.ESITO_PARTITA, esito);
        ClientHandler h1 = giocatore1;
        ClientHandler h2 = giocatore2;
        if (h1 != null) h1.invia(messaggio);
        if (h2 != null) h2.invia(messaggio);

        registraEsito(h1, h2, vincitore, durata, tempoScaduto);

        azzeraStatoPartita();
        log("Partita conclusa. " + messaggioEsito);

        // un'altra coppia potrebbe essere in attesa
        verificaCodaAttesa();
    }

    private void registraEsito(ClientHandler h1, ClientHandler h2, ClientHandler vincitore,
                               int durata, boolean tempoScaduto) {
        if (h1 == null || h2 == null || h1.getGiocatore() == null || h2.getGiocatore() == null) {
            return;
        }
        Integer idVincitore = (vincitore != null && vincitore.getGiocatore() != null)
                ? vincitore.getGiocatore().getId() : null;
        String esitoTxt = tempoScaduto || vincitore == null
                ? "Pareggio (tempo scaduto)"
                : "Vittoria di " + vincitore.getGiocatore().getUsername();
        try {
            partitaDAO.registraPartita(h1.getGiocatore().getId(), h2.getGiocatore().getId(),
                    idVincitore, durata, esitoTxt);
        } catch (Exception e) {
            // UC-13 alt. 4a: errore di scrittura nel db -> log, ma i client sono comunque notificati
            log("ERRORE nella registrazione della partita: " + e.getMessage());
        }
    }

    private void azzeraStatoPartita() {
        if (giocatore1 != null && giocatore1.getGiocatore() != null) {
            giocatore1.getGiocatore().setInPartita(false);
        }
        if (giocatore2 != null && giocatore2.getGiocatore() != null) {
            giocatore2.getGiocatore().setInPartita(false);
        }
        partitaInCorso = false;
        giocatore1 = null;
        giocatore2 = null;
        sfidaCorrente = null;
    }

    /* ========================= storico / disconnessione ========================= */

    /** Invia ad un client lo storico delle proprie partite (UC-07). */
    public void inviaStorico(ClientHandler handler) {
        if (!handler.isAutenticato()) {
            return;
        }
        List<Partita> storico = partitaDAO.findStoricoByUtente(handler.getGiocatore().getId());
        handler.invia(new Messaggio(TipoMessaggio.STORICO, new ArrayList<>(storico)));
    }

    /** Gestisce la disconnessione (volontaria o accidentale) di un client. */
    public synchronized void clientDisconnesso(ClientHandler handler) {
        connessi.remove(handler);
        inAttesa.remove(handler);

        boolean eraInPartita = partitaInCorso && (handler == giocatore1 || handler == giocatore2);
        if (eraInPartita) {
            ClientHandler avversario = (handler == giocatore1) ? giocatore2 : giocatore1;
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
                timeoutFuture = null;
            }
            String parola = sfidaCorrente != null ? sfidaCorrente.getParolaOriginale() : null;
            if (avversario != null && avversario.getGiocatore() != null) {
                long durataSec = Math.max(1, Math.round((System.nanoTime() - inizioPartitaNanos) / 1_000_000_000.0));
                String vinc = avversario.getGiocatore().getUsername();
                EsitoSfida esito = new EsitoSfida(vinc, parola, false,
                        "Hai vinto: l'avversario ha abbandonato la partita.");
                avversario.invia(new Messaggio(TipoMessaggio.ESITO_PARTITA, esito));
                registraEsito(giocatore1, giocatore2, avversario, (int) durataSec, false);
            }
            azzeraStatoPartita();
            log("Un giocatore ha abbandonato: partita interrotta.");
            verificaCodaAttesa();
        }

        if (handler.getGiocatore() != null) {
            log("'" + handler.getGiocatore().getUsername() + "' si e' disconnesso.");
        }
        rimuoviSessione(handler);
    }

    /* ============================ UI callbacks ============================ */

    /** Registra il listener per i messaggi di log e gli invia il backlog. */
    public synchronized void setLogListener(Consumer<String> logListener) {
        this.logListener = logListener;
        if (logListener != null) {
            for (String riga : bufferLog) {
                logListener.accept(riga);
            }
            bufferLog.clear();
        }
    }

    /** Registra il listener per il conteggio dei giocatori connessi. */
    public void setCountListener(IntConsumer countListener) {
        this.countListener = countListener;
        aggiornaConteggio();
    }

    private void aggiornaConteggio() {
        if (countListener != null) {
            countListener.accept(sessioniAttive.size());
        }
    }

    /** Aggiunge una riga al log del server (con timestamp). */
    public synchronized void log(String messaggio) {
        String riga = LocalTime.now().format(ORA_FMT) + " - " + messaggio;
        System.out.println("[GameServer] " + riga);
        if (logListener != null) {
            logListener.accept(riga);
        } else {
            bufferLog.add(riga);
        }
    }

    private String nome(ClientHandler h) {
        return (h != null && h.getGiocatore() != null) ? h.getGiocatore().getUsername() : "?";
    }

    /* ============================ getter ============================ */

    public int getPorta() {
        return porta;
    }

    public int getGiocatoriConnessi() {
        return sessioniAttive.size();
    }

    public boolean isInEsecuzione() {
        return inEsecuzione;
    }
}
