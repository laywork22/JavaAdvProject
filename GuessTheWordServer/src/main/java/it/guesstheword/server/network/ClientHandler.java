package it.guesstheword.server.network;

import it.guesstheword.common.model.Giocatore;
import it.guesstheword.common.network.Messaggio;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Gestisce la comunicazione con un singolo client connesso, su un thread
 * dedicato (requisito IF-1: connessioni simultanee).
 *
 * <p>Legge in ciclo i {@link Messaggio} inviati dal client e delega la logica
 * applicativa al {@link GameServer}, verso il quale puo' anche inviare messaggi
 * (comunicazione bidirezionale, requisito IS-2).</p>
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final GameServer server;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    private volatile boolean attivo = true;
    private Giocatore giocatore; // null finche' non autenticato

    public ClientHandler(Socket socket, GameServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        // L'ObjectOutputStream va creato e "flushato" prima dell'ObjectInputStream
        // per evitare deadlock nello scambio degli header di stream.
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            while (attivo) {
                Object obj = in.readObject();
                if (obj instanceof Messaggio) {
                    gestisciMessaggio((Messaggio) obj);
                }
            }
        } catch (EOFException eof) {
            // client chiuso normalmente
        } catch (IOException | ClassNotFoundException e) {
            if (attivo) {
                System.out.println("[ClientHandler] Connessione interrotta: " + e.getMessage());
            }
        } finally {
            chiudi();
            server.clientDisconnesso(this);
        }
    }

    private void gestisciMessaggio(Messaggio messaggio) {
        switch (messaggio.getTipo()) {
            case LOGIN: {
                String[] cred = (String[]) messaggio.getContenuto();
                server.gestisciLogin(this, cred[0], cred[1]);
                break;
            }
            case REGISTRAZIONE: {
                String[] cred = (String[]) messaggio.getContenuto();
                server.gestisciRegistrazione(this, cred[0], cred[1]);
                break;
            }
            case CERCA_PARTITA:
                server.aggiungiInAttesa(this);
                break;
            case ANNULLA_ATTESA:
                server.rimuoviDaAttesa(this);
                break;
            case RISPOSTA:
                server.rispostaRicevuta(this, (String) messaggio.getContenuto());
                break;
            case RICHIESTA_STORICO:
                server.inviaStorico(this);
                break;
            case DISCONNESSIONE:
                attivo = false;
                break;
            default:
                // tipi non previsti in ingresso lato server: ignorati
                break;
        }
    }

    /**
     * Invia un messaggio al client in modo thread-safe.
     *
     * @param messaggio messaggio da inviare
     */
    public synchronized void invia(Messaggio messaggio) {
        try {
            out.writeObject(messaggio);
            out.flush();
            // reset per evitare che la cache dell'ObjectOutputStream impedisca
            // l'aggiornamento di oggetti gia' inviati in precedenza.
            out.reset();
        } catch (IOException e) {
            System.out.println("[ClientHandler] Errore di invio: " + e.getMessage());
            chiudi();
        }
    }

    /** Chiude socket e stream, terminando il ciclo di lettura. */
    public void chiudi() {
        attivo = false;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public Giocatore getGiocatore() {
        return giocatore;
    }

    public void setGiocatore(Giocatore giocatore) {
        this.giocatore = giocatore;
    }

    public boolean isAutenticato() {
        return giocatore != null;
    }
}
