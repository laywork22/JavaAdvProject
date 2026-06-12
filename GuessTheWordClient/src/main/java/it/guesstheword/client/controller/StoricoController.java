package it.guesstheword.client.controller;

import it.guesstheword.common.model.Partita;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

/**
 * Gestisce la tabella dello storico delle partite del giocatore (UC-07).
 *
 * <p>Non e' un controller FXML: viene istanziato da {@link DashboardController}
 * passando la {@link TableView} e le relative colonne, di cui configura le
 * {@code cellValueFactory} e che popola con i dati ricevuti dal server.</p>
 */
public class StoricoController {

    private final TableView<Partita> tabella;
    private final TableColumn<Partita, String> idClm;
    private final TableColumn<Partita, String> avversarioClm;
    private final TableColumn<Partita, String> dataClm;
    private final TableColumn<Partita, String> durataClm;
    private final TableColumn<Partita, String> esitoClm;

    public StoricoController(TableView<Partita> tabella,
                            TableColumn<Partita, String> idClm,
                            TableColumn<Partita, String> avversarioClm,
                            TableColumn<Partita, String> dataClm,
                            TableColumn<Partita, String> durataClm,
                            TableColumn<Partita, String> esitoClm) {
        this.tabella = tabella;
        this.idClm = idClm;
        this.avversarioClm = avversarioClm;
        this.dataClm = dataClm;
        this.durataClm = durataClm;
        this.esitoClm = esitoClm;
        configura();
    }

    private void configura() {
        idClm.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        avversarioClm.setCellValueFactory(c -> new SimpleStringProperty(testo(c.getValue().getAvversario())));
        dataClm.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getData() != null ? c.getValue().getData().toString() : ""));
        durataClm.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDurata() > 0 ? c.getValue().getDurata() + " s" : "-"));
        esitoClm.setCellValueFactory(c -> new SimpleStringProperty(testo(c.getValue().getEsito())));
        tabella.setPlaceholder(new Label("Nessuna partita giocata finora"));
    }

    /**
     * Popola la tabella con lo storico fornito.
     *
     * @param partite lista delle partite del giocatore
     */
    public void mostra(List<Partita> partite) {
        tabella.setItems(FXCollections.observableArrayList(partite));
    }

    private String testo(String s) {
        return s != null ? s : "";
    }
}
