package it.guesstheword.server.controller;

import it.guesstheword.server.ServerMain;
import it.guesstheword.server.authentication.ServerAdminAuth;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller della schermata di login dell'amministratore.
 *
 * <p>Verifica le credenziali tramite {@link ServerAdminAuth} e, in caso di
 * successo, delega a {@link ServerMain} il passaggio alla dashboard.</p>
 */
public class AdminLoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private final ServerMain serverMain;
    private final ServerAdminAuth serverAdminAuth;

    public AdminLoginController(ServerMain serverMain, ServerAdminAuth serverAdminAuth) {
        this.serverMain = serverMain;
        this.serverAdminAuth = serverAdminAuth;
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            mostra(Alert.AlertType.WARNING, "Inserisci username e password.");
            return;
        }
        if (serverAdminAuth.login(username, password)) {
            serverMain.mostraDashboard();
        } else {
            mostra(Alert.AlertType.ERROR, "Credenziali amministratore non valide.");
        }
    }

    private void mostra(Alert.AlertType tipo, String messaggio) {
        Alert alert = new Alert(tipo, messaggio, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
