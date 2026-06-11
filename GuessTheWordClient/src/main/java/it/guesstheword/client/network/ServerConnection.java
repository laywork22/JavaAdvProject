package it.guesstheword.client.network;

import it.guesstheword.common.network.Messaggio;
import it.guesstheword.common.network.TipoMessaggio;
import javafx.application.Platform;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Gestisce la connessione socket del client verso il server ed il relativo
 * scambio di {@link Messaggio} serializzati (requisito IS-2).
 *
 * <p>I messaggi in arrivo vengono letti da un thread di ascolto dedicato e
 * consegnati all'handler registrato sul thread dell'interfaccia JavaFX
 * (tramite {@link Platform#runLater}), in modo che i controller possano
 * aggiornare la UI in sicurezza.</p>
 */
public class ServerConnection {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private volatile boolean connesso;
    /** Distingue una chiusura voluta dall'utente da una disconnessione anomala. */
    private volatile boolean chiusuraRichiesta;
    private Consumer<Messaggio> messaggioHandler;
    private Runnable onDisconnessione;

    /**
     * Apre la connessione verso il server ed avvia il thread di ascolto.
     *
     * @param ip   indirizzo del server
     * @param port porta del server
     * @throws IOException se la connessione non puo' essere stabilita
     */
    public void connetti(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        // ObjectOutputStream creato e flushato prima dell'ObjectInputStream.
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        connesso = true;

        Thread ascolto = new Thread(this::cicloAscolto, "server-listener");
        ascolto.setDaemon(true);
        ascolto.start();
    }

    private void cicloAscolto() {
        try {
            while (connesso) {
                Object obj = in.readObject();
                if (obj instanceof Messaggio) {
                    final Messaggio messaggio = (Messaggio) obj;
                    Platform.runLater(() -> {
                        if (messaggioHandler != null) {
                            messaggioHandler.accept(messaggio);
                        }
                    });
                }
            }
        } catch (EOFException eof) {
            // server chiuso
        } catch (IOException | ClassNotFoundException e) {
            if (connesso) {
                System.out.println("[ServerConnection] Connessione interrotta: " + e.getMessage());
            }
        } finally {
            connesso = false;
            // Notifica la disconnessione solo se non e' stata richiesta
            // dall'utente (chiusura della finestra): in quel caso l'app sta
            // gia' terminando e non vogliamo mostrare il messaggio d'errore.
            if (!chiusuraRichiesta && onDisconnessione != null) {
                Platform.runLater(onDisconnessione);
            }
        }
    }

    /**
     * Invia un messaggio al server in modo thread-safe.
     *
     * @param messaggio messaggio da inviare
     */
    public synchronized void invia(Messaggio messaggio) {
        if (!connesso) {
            return;
        }
        try {
            out.writeObject(messaggio);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.out.println("[ServerConnection] Errore di invio: " + e.getMessage());
            connesso = false;
            if (!chiusuraRichiesta && onDisconnessione != null) {
                Platform.runLater(onDisconnessione);
            }
        }
    }

    /** Chiude la connessione, notificando al server la disconnessione. */
    public void chiudi() {
        chiusuraRichiesta = true;
        if (connesso) {
            invia(new Messaggio(TipoMessaggio.DISCONNESSIONE));
        }
        connesso = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Registra il callback da eseguire quando arriva un nuovo messaggio dal server.
     *
     * @param messaggioHandler la funzione che gestirà il messaggio in arrivo
     */
    public void setMessaggioHandler(Consumer<Messaggio> messaggioHandler) {
        this.messaggioHandler = messaggioHandler;
    }

    /**
     * Registra il callback da eseguire in caso di disconnessione anomala.
     *
     * @param onDisconnessione l'azione da eseguire alla caduta della connessione
     */
    public void setOnDisconnessione(Runnable onDisconnessione) {
        this.onDisconnessione = onDisconnessione;
    }

    /** @return true se il socket verso il server è attualmente aperto e connesso. */
    public boolean isConnesso() {
        return connesso;
    }
}
