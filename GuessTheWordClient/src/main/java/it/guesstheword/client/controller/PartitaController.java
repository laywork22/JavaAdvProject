package it.guesstheword.client.controller;

import it.guesstheword.client.ClientMain;
import it.guesstheword.common.network.EsitoSfida;
import it.guesstheword.common.network.Sfida;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * Controller della finestra di gioco (UC-04, UC-05, UC-06).
 *
 * <p>Visualizza il testo con la parola cifrata, gestisce il conto alla rovescia
 * (timer visibile tramite {@link ProgressBar}), invia la risposta del giocatore
 * e mostra l'esito finale.</p>
 */
public class PartitaController {

    @FXML private TextArea testoSfidaArea;
    @FXML private TextField answerFld;
    @FXML private ProgressBar tempoRimastoBar;
    @FXML private Label numeroDomandaLbl;

    private final ClientMain clientMain;
    private final Sfida sfida;

    private Timeline timeline;
    private double secondiRimasti;
    private boolean conclusa;

    public PartitaController(ClientMain clientMain, Sfida sfida) {
        this.clientMain = clientMain;
        this.sfida = sfida;
    }

    @FXML
    private void initialize() {
        testoSfidaArea.setText(sfida.getTestoCifrato());
        testoSfidaArea.setEditable(false);
        testoSfidaArea.setWrapText(true);
        numeroDomandaLbl.setText("Indovina la parola nascosta!");
        tempoRimastoBar.setProgress(1.0);
        answerFld.requestFocus();
        avviaTimer();
    }

    private void avviaTimer() {
        final int durataTotale = Math.max(1, sfida.getDurataSecondi());
        secondiRimasti = durataTotale;
        timeline = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            secondiRimasti -= 0.1;
            double frazione = Math.max(0.0, secondiRimasti / durataTotale);
            tempoRimastoBar.setProgress(frazione);
            if (secondiRimasti <= 0) {
                scadeTempo();
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    /** Invio della risposta (Enter sul campo di testo, UC-05). */
    @FXML
    private void onInviaRisposta() {
        if (conclusa) {
            return;
        }
        String risposta = answerFld.getText();
        if (risposta == null || risposta.trim().isEmpty()) {
            return;
        }
        clientMain.inviaRisposta(risposta.trim());
        answerFld.clear();
    }

    /** Scadenza del timer lato client: disabilita l'input (UC-05 alt. 4a). */
    private void scadeTempo() {
        if (timeline != null) {
            timeline.stop();
        }
        tempoRimastoBar.setProgress(0);
        answerFld.setDisable(true);
        numeroDomandaLbl.setText("Tempo scaduto, in attesa dell'esito...");
    }

    /**
     * Mostra l'esito della partita e torna alla dashboard (UC-06).
     *
     * @param esito esito ricevuto dal server
     */
    public void mostraEsito(EsitoSfida esito) {
        conclusa = true;
        if (timeline != null) {
            timeline.stop();
        }
        answerFld.setDisable(true);

        String mioUsername = clientMain.getGiocatoreCorrente() != null
                ? clientMain.getGiocatoreCorrente().getUsername() : null;

        String titolo;
        String testo;
        if (esito.isTempoScaduto() || esito.getVincitore() == null) {
            titolo = "Tempo scaduto";
            testo = "Nessun vincitore.\nLa parola era: " + esito.getParolaOriginale();
        } else if (esito.getVincitore().equals(mioUsername)) {
            titolo = "Hai vinto!";
            testo = "Complimenti, hai indovinato!\nLa parola era: " + esito.getParolaOriginale();
        } else {
            titolo = "Hai perso";
            testo = "Ha vinto " + esito.getVincitore() + ".\nLa parola era: " + esito.getParolaOriginale();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, testo, ButtonType.OK);
        alert.setHeaderText(titolo);
        alert.showAndWait();

        clientMain.tornaAllaDashboard();
    }
}
