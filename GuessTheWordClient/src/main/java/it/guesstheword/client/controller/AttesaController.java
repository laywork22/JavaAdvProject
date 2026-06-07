package it.guesstheword.client.controller;

import it.guesstheword.client.ClientMain;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controller della schermata di attesa dell'avversario (UC-03).
 *
 * <p>Mostra l'indicatore di attesa; il pulsante "Annulla" permette di uscire
 * dalla coda di matchmaking e tornare alla dashboard.</p>
 */
public class AttesaController {

    @FXML private Button annullaRicercaPartitaBtn;

    private final ClientMain clientMain;

    public AttesaController(ClientMain clientMain) {
        this.clientMain = clientMain;
    }

    /** Gestisce il click sul pulsante "Annulla" (nome definito nell'FXML). */
    @FXML
    private void onAnullaClicked() {
        clientMain.annullaAttesa();
    }
}
