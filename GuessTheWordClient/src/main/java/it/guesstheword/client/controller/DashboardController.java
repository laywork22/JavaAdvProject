package it.guesstheword.client.controller;

import it.guesstheword.client.ClientMain;
import it.guesstheword.common.model.Partita;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

/**
 * Controller della dashboard del client (Home + Statistiche).
 *
 * <p>Coordina i due componenti di supporto {@link StoricoController} (tabella
 * dello storico) e {@link ClassificaController} (statistiche e grafici), e
 * gestisce l'avvio di una nuova partita.</p>
 */
public class DashboardController {

    @FXML private Label benvenutoLbl;
    @FXML private Button nuovaPartitaBtn;

    @FXML private TableView<Partita> storicoPartitaTbl;
    @FXML private TableColumn<Partita, String> idPartitaClm;
    @FXML private TableColumn<Partita, String> avversarioClm;
    @FXML private TableColumn<Partita, String> dataClm;
    @FXML private TableColumn<Partita, String> durataClm;
    @FXML private TableColumn<Partita, String> esitoClm;

    @FXML private AreaChart<String, Number> tempiRispostaChart;
    @FXML private BarChart<String, Number> ratioWinLoseChart;
    @FXML private Label partiteTotaliGiocateLbl;
    @FXML private Label winRateLbl;
    @FXML private Label migliorTempoAssolutoLbl;

    private final ClientMain clientMain;

    private StoricoController storicoController;
    private ClassificaController classificaController;

    public DashboardController(ClientMain clientMain) {
        this.clientMain = clientMain;
    }

    @FXML
    private void initialize() {
        storicoController = new StoricoController(storicoPartitaTbl,
                idPartitaClm, avversarioClm, dataClm, durataClm, esitoClm);
        classificaController = new ClassificaController(
                partiteTotaliGiocateLbl, winRateLbl, migliorTempoAssolutoLbl,
                tempiRispostaChart, ratioWinLoseChart);

        if (benvenutoLbl != null && clientMain.getGiocatoreCorrente() != null) {
            benvenutoLbl.setText("Benvenuto, " + clientMain.getGiocatoreCorrente().getUsername() + "!");
        }
    }

    /** Richiede al server i dati da visualizzare (storico + statistiche). */
    public void caricaDati() {
        clientMain.richiediStorico();
    }

    /** Pulsante "Nuova partita" (definito nell'FXML come onAction). */
    @FXML
    private void iniziaNuovaPartita() {
        clientMain.cercaPartita();
    }

    /**
     * Aggiorna storico e statistiche con i dati ricevuti dal server.
     *
     * @param storico storico delle partite del giocatore
     */
    public void aggiornaStorico(List<Partita> storico) {
        storicoController.mostra(storico);
        classificaController.aggiorna(storico);
    }
}
