package it.guesstheword.client.controller;

import it.guesstheword.client.ClientMain;
import it.guesstheword.client.authentication.LoginAuth;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Controller della schermata di login/registrazione del client (UC-01, UC-02).
 *
 * <p>Gestisce il passaggio tra il form di login e quello di registrazione ed
 * inoltra le richieste tramite {@link LoginAuth}. L'esito viene ricevuto in
 * modo asincrono dal dispatcher di {@link ClientMain}, che richiama i metodi
 * {@link #mostraErrore(String)} e {@link #registrazioneRiuscita()}.</p>
 */
public class LoginController {

    @FXML private VBox loginPane;
    @FXML private VBox registrationPane;

    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;

    @FXML private TextField registerUsernameField;
    @FXML private PasswordField registerPasswordField;

    private final ClientMain clientMain;
    private final LoginAuth loginAuth;

    /**
     * 
     * @param clientMain    Il main dell'interfaccia dell'utente
     * @param loginAuth     Il gestore dell'autenticazione
     */
    public LoginController(ClientMain clientMain, LoginAuth loginAuth) {
        this.clientMain = clientMain;
        this.loginAuth = loginAuth;
    }

    /** Invia la richiesta di login. */
    @FXML
    private void onLogin() {
        String username = testo(loginUsernameField);
        String password = loginPasswordField.getText() == null ? "" : loginPasswordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            mostraErrore("Inserisci username e password.");
            return;
        }
        loginAuth.login(username, password);
    }

    /** Mostra il form di registrazione. */
    @FXML
    private void onMostraRegistrazione() {
        loginPane.setVisible(false);
        registrationPane.setVisible(true);
    }

    /** Torna al form di login dal form di registrazione. */
    @FXML
    private void onMostraLogin() {
        registrationPane.setVisible(false);
        loginPane.setVisible(true);
    }

    /** Invia la richiesta di registrazione. */
    @FXML
    private void onRegistra() {
        String username = testo(registerUsernameField);
        String password = registerPasswordField.getText() == null ? "" : registerPasswordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            mostraErrore("Inserisci username e password per registrarti.");
            return;
        }
        loginAuth.registra(username, password);
    }

    /** Mostra un messaggio di errore (chiamato dal dispatcher). */
    public void mostraErrore(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.ERROR,
                messaggio != null ? messaggio : "Operazione non riuscita.", ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /** Notifica la registrazione avvenuta e riporta al form di login (UC-01). */
    public void registrazioneRiuscita() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Registrazione completata! Ora puoi effettuare il login.", ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
        if (registerUsernameField != null) registerUsernameField.clear();
        if (registerPasswordField != null) registerPasswordField.clear();
        registrationPane.setVisible(false);
        loginPane.setVisible(true);
    }

    private String testo(TextField campo) {
        return campo.getText() == null ? "" : campo.getText().trim();
    }
}
