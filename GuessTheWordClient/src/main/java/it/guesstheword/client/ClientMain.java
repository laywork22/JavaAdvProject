package it.guesstheword.client;

import it.guesstheword.client.authentication.ClientAuth;
import it.guesstheword.client.controller.AttesaController;
import it.guesstheword.client.controller.DashboardController;
import it.guesstheword.client.controller.LoginController;
import it.guesstheword.client.controller.PartitaController;
import it.guesstheword.client.core.ConfigManager;
import it.guesstheword.client.network.ServerConnection;
import it.guesstheword.common.model.Giocatore;
import it.guesstheword.common.model.Partita;
import it.guesstheword.common.network.EsitoSfida;
import it.guesstheword.common.network.Messaggio;
import it.guesstheword.common.network.Sfida;
import it.guesstheword.common.network.TipoMessaggio;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Punto di ingresso del client JavaFX.
 *
 * <p>Svolge tre ruoli: <em>bootstrap</em> (lettura configurazione e connessione
 * al server), <em>coordinatore delle schermate</em> (login, dashboard, attesa,
 * partita) e <em>dispatcher</em> centrale dei messaggi ricevuti dal server, che
 * instrada verso il controller competente.</p>
 */
public class ClientMain extends Application {

    /** Schermate gestite dal client. */
    private enum Schermata { LOGIN, DASHBOARD, ATTESA, PARTITA }

    private Stage primaryStage;
    private ServerConnection connection;
    private ClientAuth clientAuth;
    private Giocatore giocatoreCorrente;

    private Schermata corrente = Schermata.LOGIN;
    private LoginController loginController;
    private DashboardController dashboardController;
    private AttesaController attesaController;
    private PartitaController partitaController;

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("GuessTheWord");

        ConfigManager config = new ConfigManager();
        connection = new ServerConnection();
        try {
            connection.connetti(config.getServerIp(), config.getServerPort());
        } catch (IOException e) {
            mostraErroreFatale("Impossibile connettersi al server "
                    + config.getServerIp() + ":" + config.getServerPort()
                    + "\nVerifica che il server sia avviato.");
            return;
        }
        connection.setMessaggioHandler(this::gestisciMessaggio);
        connection.setOnDisconnessione(this::gestisciDisconnessione);
        clientAuth = new ClientAuth(connection);

        mostraLogin();
        stage.setOnCloseRequest(e -> {
            // Chiusura voluta dall'utente: chiude la socket (chiudi() notifica
            // gia' la DISCONNESSIONE al server) e termina l'applicazione senza
            // mostrare il messaggio di errore (filtrato in ServerConnection).
            connection.chiudi();
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

    /** Carica e mostra l'interfaccia di Login e Registrazione. */
    public void mostraLogin() {
        loginController = new LoginController(this, clientAuth);
        caricaScena("/Login_Register_Interface.fxml", loginController);
        corrente = Schermata.LOGIN;
    }

    /** Carica e mostra l'interfaccia della Dashboard principale. */
    public void mostraDashboard() {
        dashboardController = new DashboardController(this);
        caricaScena("/Client_Interface.fxml", dashboardController);

        corrente = Schermata.DASHBOARD;
        dashboardController.caricaDati(); // richiede lo storico al server
    }

    /** Carica e mostra la schermata di attesa durante il matchmaking. */
    public void mostraAttesa() {
        attesaController = new AttesaController(this);
        caricaScena("/Waiting_Screen.fxml", attesaController);
        corrente = Schermata.ATTESA;
    }

    /**
     * Carica e mostra l'interfaccia di gioco.
     *
     * @param sfida i dati della sfida da mostrare (parola cifrata, ecc.)
     */
    public void mostraPartita(Sfida sfida) {
        partitaController = new PartitaController(this, sfida);
        caricaScena("/Game_Interface.fxml", partitaController);
        corrente = Schermata.PARTITA;
    }

    private void caricaScena(String fxml, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setController(controller);
            Parent root = loader.load();
            Scene nuovaScena = new Scene(root);
            String cssPath = getClass().getResource("/style.css").toExternalForm();
            nuovaScena.getStylesheets().add(cssPath);

            primaryStage.setScene(nuovaScena);

            primaryStage.setResizable(true);

            Platform.runLater(() -> {
                primaryStage.sizeToScene();
                primaryStage.centerOnScreen();
                primaryStage.setResizable(false);
            });
        } catch (IOException e) {
            e.printStackTrace();
            mostraErroreFatale("Errore nel caricamento dell'interfaccia (" + fxml + "):\n" + e.getMessage());
        }
    }

    /*  azioni verso il server  */

    /** "Nuova partita": entra in matchmaking e mostra la schermata di attesa. */
    public void cercaPartita() {
        connection.invia(new Messaggio(TipoMessaggio.CERCA_PARTITA));
        mostraAttesa();
    }

    /** Annulla la ricerca dell'avversario e torna alla dashboard. */
    public void annullaAttesa() {
        connection.invia(new Messaggio(TipoMessaggio.ANNULLA_ATTESA));
        mostraDashboard();
    }

    /** Invia al server la risposta proposta dal giocatore.
     * 
     * @param risposta  Il messaggio di risposta del client
     */
    public void inviaRisposta(String risposta) {
        connection.invia(new Messaggio(TipoMessaggio.RISPOSTA, risposta));
    }

    /** Richiede al server lo storico delle partite del giocatore. */
    public void richiediStorico() {
        connection.invia(new Messaggio(TipoMessaggio.RICHIESTA_STORICO));
    }

    /** Riporta l'utente alla schermata della Dashboard. */
    public void tornaAllaDashboard() {
        mostraDashboard();
    }

    /*  dispatcher messaggi  */

    //@SuppressWarnings("unchecked")
    private void gestisciMessaggio(Messaggio messaggio) {
        switch (messaggio.getTipo()) {
            case LOGIN_OK:
                giocatoreCorrente = estraiCome(messaggio, Giocatore.class);
                mostraDashboard();
                break;
            case LOGIN_FALLITO:
                if (loginController != null) {
                    loginController.mostraErrore(estraiCome(messaggio, String.class));
                }
                break;
            case REGISTRAZIONE_OK:
                if (loginController != null) {
                    loginController.registrazioneRiuscita();
                }
                break;
            case REGISTRAZIONE_FALLITA:
                if (loginController != null) {
                    loginController.mostraErrore(estraiCome(messaggio, String.class));
                }
                break;
            case IN_ATTESA:
                // conferma di essere in coda: la schermata di attesa e' gia' mostrata
                break;
            case INIZIO_PARTITA:
                mostraPartita(estraiCome(messaggio,Sfida.class));
                break;
            case ESITO_PARTITA:
                gestisciEsito(estraiCome(messaggio, EsitoSfida.class));
                break;
            case STORICO:
                if (dashboardController != null && corrente == Schermata.DASHBOARD) {
                    List<Partita> storico = estraiLista(messaggio, Partita.class);
                    dashboardController.aggiornaStorico(storico);
                }
                break;
            case ERRORE:
                mostraAvviso("Avviso dal server", estraiCome(messaggio, String.class));
                if (corrente == Schermata.ATTESA) {
                    mostraDashboard();
                }
                break;
            default:
                break;
        }
    }

    private void gestisciEsito(EsitoSfida esito) {
        if (partitaController != null && corrente == Schermata.PARTITA) {
            partitaController.mostraEsito(esito);
        } else {
            // fallback: mostra comunque l'esito e torna alla dashboard
            mostraAvviso("Esito partita", esito.getMessaggio());
            mostraDashboard();
        }
    }

    private void gestisciDisconnessione() {
        // mostrato anche alla schermata di login: senza avviso l'utente
        // resterebbe davanti ad un'interfaccia che non risponde piu'
        mostraErroreFatale("Connessione al server persa.");
    }

    /*  utilita'  */

    private void mostraAvviso(String titolo, String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, messaggio, ButtonType.OK);
        alert.setHeaderText(titolo);
        alert.showAndWait();
    }

    private void mostraErroreFatale(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.ERROR, messaggio, ButtonType.OK);
        alert.setHeaderText("Errore");
        alert.showAndWait();
        Platform.exit();
    }


    /** @return l'oggetto responsabile della connessione di rete col server. */
    public ServerConnection getConnection() {
        return connection;
    }

    /** @return i dati del giocatore attualmente loggato. */
    public Giocatore getGiocatoreCorrente() {
        return giocatoreCorrente;
    }

    /**
     * Estrae in modo sicuro il contenuto di un messaggio e ne fa il cast al tipo atteso.
     *
     * @param messaggio  il messaggio ricevuto dal server
     * @param tipoAtteso la classe del tipo che ci si aspetta di trovare nel payload
     * @param <T>        il tipo generico atteso
     * @return l'oggetto estratto e tipizzato
     * @throws IllegalArgumentException se il contenuto non corrisponde al tipo atteso
     */
    public static <T> T estraiCome(Messaggio messaggio, Class<T> tipoAtteso) {
        Object contenuto = messaggio.getContenuto();

        if (tipoAtteso.isInstance(contenuto)) {
            return tipoAtteso.cast(contenuto);
        }

        throw new IllegalArgumentException("Errore di protocollo: " + tipoAtteso.getSimpleName());
    }

    /**
     * Estrae in modo sicuro una lista dal payload di un messaggio.
     *
     * @param messaggio    il messaggio ricevuto dal server
     * @param tipoElemento la classe del tipo degli elementi contenuti nella lista
     * @param <T>          il tipo generico degli elementi della lista
     * @return la lista estratta
     * @throws IllegalArgumentException se il contenuto non è una lista
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> estraiLista(Messaggio messaggio, Class<T> tipoElemento) {
        Object contenuto = messaggio.getContenuto();

        if (contenuto instanceof List<?>) {
            return (List<T>) contenuto;
        }

        throw new IllegalArgumentException("Errore: il payload non è una lista.");
    }

    /**
     * Metodo main di avvio dell'applicazione.
     *
     * @param args argomenti passati da riga di comando
     */
    public static void main(String[] args) {
        launch(args);
    }
}
