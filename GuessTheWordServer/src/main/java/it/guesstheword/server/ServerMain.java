package it.guesstheword.server;

import it.guesstheword.server.authentication.ServerAdminAuth;
import it.guesstheword.server.controller.AdminDashboardController;
import it.guesstheword.server.controller.AdminLoginController;
import it.guesstheword.server.core.AnalizzatoreDocumenti;
import it.guesstheword.server.core.ConfigManager;
import it.guesstheword.server.core.GestoreAutenticazione;
import it.guesstheword.server.core.GestoreSerializzazione;
import it.guesstheword.server.core.GestoreSfida;
import it.guesstheword.server.dao.AdminDAO;
import it.guesstheword.server.dao.DatabaseManager;
import it.guesstheword.server.dao.PartitaDAO;
import it.guesstheword.server.dao.UtenteDAO;
import it.guesstheword.server.network.GameServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Punto di ingresso dell'applicazione server (interfaccia di amministrazione).
 *
 * <p>All'avvio: legge la configurazione, inizializza il database (schema +
 * account di default), istanzia i servizi applicativi ed avvia il
 * {@link GameServer}; mostra quindi la schermata di login dell'amministratore.
 * Il server accetta le connessioni dei giocatori indipendentemente dal login
 * dell'amministratore, il quale serve solo ad accedere al pannello di
 * monitoraggio e gestione.</p>
 */
public class ServerMain extends Application {

    private Stage primaryStage;

    private AnalizzatoreDocumenti analizzatore;
    private GestoreSerializzazione gestoreSerializzazione;
    private GestoreSfida gestoreSfida;
    private PartitaDAO partitaDAO;
    private GameServer gameServer;
    private ServerAdminAuth serverAdminAuth;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // 1) Configurazione
        ConfigManager config = new ConfigManager();

        // 2) Database: schema + account di default (DF-7)
        DatabaseManager databaseManager = new DatabaseManager(config.getDbUrl());
        databaseManager.inizializza(config.getAdminUsername(), config.getAdminPassword());

        // 3) DAO e servizi
        UtenteDAO utenteDAO = new UtenteDAO(databaseManager);
        AdminDAO adminDAO = new AdminDAO(databaseManager);
        partitaDAO = new PartitaDAO(databaseManager);
        GestoreAutenticazione gestoreAutenticazione = new GestoreAutenticazione(utenteDAO, adminDAO);
        serverAdminAuth = new ServerAdminAuth(gestoreAutenticazione);
        analizzatore = new AnalizzatoreDocumenti();
        gestoreSerializzazione = new GestoreSerializzazione();
        gestoreSfida = new GestoreSfida(analizzatore);
        gameServer = new GameServer(config.getPort(), gestoreAutenticazione, gestoreSfida, partitaDAO);

        // 4) Avvio del server di gioco
        try {
            gameServer.avvia();
        } catch (IOException e) {
            mostraErroreFatale("Impossibile avviare il server sulla porta "
                    + config.getPort() + ":\n" + e.getMessage());
            return;
        }

        // 5) Interfaccia amministratore
        mostraLogin();
        stage.setTitle("GuessTheWord - Server");
        stage.show();
    }

    private void mostraLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login_Register_Interface.fxml"));
            AdminLoginController controller = new AdminLoginController(this, serverAdminAuth);
            loader.setController(controller);
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            mostraErroreFatale("Errore nel caricamento dell'interfaccia di login:\n" + e.getMessage());
        }
    }

    /** Mostra la dashboard amministratore (chiamata dopo un login riuscito). */
    public void mostraDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Server_Interface.fxml"));
            Parent root = loader.load();
            AdminDashboardController controller = loader.getController();
            controller.init(gameServer, analizzatore, gestoreSerializzazione, partitaDAO);
            primaryStage.setScene(new Scene(root));
            primaryStage.setResizable(false);
            primaryStage.setTitle("GuessTheWord - Server (Amministratore)");
        } catch (IOException e) {
            e.printStackTrace();
            mostraErroreFatale("Errore nel caricamento della dashboard:\n" + e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (gameServer != null) {
            gameServer.ferma();
        }
    }

    private void mostraErroreFatale(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.ERROR, messaggio, ButtonType.OK);
        alert.setHeaderText("Errore");
        alert.showAndWait();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
