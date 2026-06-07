package it.guesstheword.server.controller;

import it.guesstheword.server.concurrency.AnalisiTask;
import it.guesstheword.server.core.AnalizzatoreDocumenti;
import it.guesstheword.server.core.DocumentoAnalizzato;
import it.guesstheword.server.core.GestoreSerializzazione;
import it.guesstheword.server.dao.PartitaDAO;
import it.guesstheword.server.network.GameServer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Controller della dashboard amministratore (interfaccia di gestione del
 * server). Gestisce: monitoraggio stato e giocatori connessi, log del server,
 * caricamento/analisi documenti (UC-08/09), salvataggio/caricamento analisi
 * (UC-10) e visualizzazione delle statistiche per giocatore (UC-11).
 */
public class AdminDashboardController {

    /* menu Analisi Documento */
    @FXML private MenuItem avviaAnalisiItem;
    @FXML private MenuItem caricaDocumentoItem;
    @FXML private MenuItem caricaAnalisiItem;
    @FXML private MenuItem salvaAnalisiItem;

    /* dashboard */
    @FXML private Label serverStatusLbl;
    @FXML private Label giocatoriConnessiLbl;
    @FXML private Label docCaricatoLbl;
    @FXML private ProgressBar progressoCaricaDocumento;
    @FXML private TextArea logArea;
    @FXML private Button spegniServerBtn;

    /* statistiche */
    @FXML private Tab statisticheAdminTab;
    @FXML private TableView<RigaStatistica> statisticheAdminTable;
    @FXML private TableColumn<RigaStatistica, String> giocatoreClm;
    @FXML private TableColumn<RigaStatistica, String> numeroVittorieClm;
    @FXML private TableColumn<RigaStatistica, String> partiteClm;
    @FXML private TableColumn<RigaStatistica, String> tempoMedioClm;

    private GameServer gameServer;
    private AnalizzatoreDocumenti analizzatore;
    private GestoreSerializzazione gestoreSerializzazione;
    private PartitaDAO partitaDAO;

    @FXML
    private void initialize() {
        giocatoreClm.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGiocatore()));
        numeroVittorieClm.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getVittorie())));
        partiteClm.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getPartite())));
        tempoMedioClm.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTempoMedioFormattato()));
        
    }

    /** Inietta le dipendenze e collega i listener (chiamato da ServerMain). */
    public void init(GameServer gameServer, AnalizzatoreDocumenti analizzatore,
                     GestoreSerializzazione gestoreSerializzazione, PartitaDAO partitaDAO) {
        this.gameServer = gameServer;
        this.analizzatore = analizzatore;
        this.gestoreSerializzazione = gestoreSerializzazione;
        this.partitaDAO = partitaDAO;

        serverStatusLbl.setText("Online");
        serverStatusLbl.setStyle("-fx-text-fill: green;");
        giocatoriConnessiLbl.setText(String.valueOf(gameServer.getGiocatoriConnessi()));

        // handler delle voci di menu
        caricaDocumentoItem.setOnAction(e -> onCaricaDocumento());
        avviaAnalisiItem.setOnAction(e -> onAvviaAnalisi());
        salvaAnalisiItem.setOnAction(e -> onSalvaAnalisi());
        caricaAnalisiItem.setOnAction(e -> onCaricaAnalisi());
        if (spegniServerBtn != null) {
            spegniServerBtn.setOnAction(e -> onSpegniServer());
        }

        // collegamento ai callback del server (sempre sul thread JavaFX)
        gameServer.setLogListener(riga -> Platform.runLater(() -> appendLog(riga)));
        gameServer.setCountListener(n -> Platform.runLater(() -> giocatoriConnessiLbl.setText(String.valueOf(n))));

        // aggiornamento statistiche all'apertura del tab
        if (statisticheAdminTab != null) {
            statisticheAdminTab.setOnSelectionChanged(e -> {
                if (statisticheAdminTab.isSelected()) {
                    aggiornaStatistiche();
                }
            });
        }
        aggiornaStatistiche();
    }

    // SEZIONE DOCUMENTI E ANALISI 
    private void onCaricaDocumento() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleziona documenti TXT");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("File di testo (*.txt)", "*.txt"));
        File dir = new File("documents");
        if (dir.isDirectory()) {
            fc.setInitialDirectory(dir);
        }
        List<File> files = fc.showOpenMultipleDialog(finestra());
        if (files == null || files.isEmpty()) {
            return;
        }
        int caricati = 0;
        for (File f : files) {
            try {
                analizzatore.caricaDocumento(f);
                appendLog("Documento caricato: " + f.getName());
                caricati++;
                docCaricatoLbl.setText(caricati + " documento/i caricati");
            } catch (Exception ex) {
                appendLog("ERRORE caricando " + f.getName() + ": " + ex.getMessage());
            }
        }
        appendLog(caricati + " documento/i pronti per l'analisi.");
    }

    private void onAvviaAnalisi() {
        if (analizzatore.getDocumentiCaricati().isEmpty()) {
            avviso("Nessun documento caricato",
                    "Carica prima uno o piu' documenti TXT tramite 'Carica Documento'.");
            return;
        }
        AnalisiTask task = new AnalisiTask(analizzatore);

        progressoCaricaDocumento.progressProperty().bind(task.progressProperty());
        task.messageProperty().addListener((obs, vecchio, nuovo) -> {
            if (nuovo != null && !nuovo.isEmpty()) {
                appendLog(nuovo);
            }
        });
        task.setOnSucceeded(e -> {
            progressoCaricaDocumento.progressProperty().unbind();
            progressoCaricaDocumento.setProgress(1.0);
        });
        task.setOnFailed(e -> {
            progressoCaricaDocumento.progressProperty().unbind();
            progressoCaricaDocumento.setProgress(0);
            appendLog("Analisi fallita: " + task.getException());
        });
        Thread t = new Thread(task, "analisi-task");
        t.setDaemon(true);
        t.start();
    }

    private void onSalvaAnalisi() {
        if (!analizzatore.haDocumentiAnalizzati()) {
            avviso("Nessuna analisi", "Non ci sono analisi da salvare. Avvia prima un'analisi.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Salva analisi");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Analisi serializzata (*.dat)", "*.dat"));
        fc.setInitialFileName("analisi.dat");
        File dir = new File("data");
        if (dir.isDirectory()) {
            fc.setInitialDirectory(dir);
        }
        File file = fc.showSaveDialog(finestra());
        if (file == null) {
            return;
        }
        try {
            gestoreSerializzazione.salvaAnalisi(analizzatore.getRisultatiAnalisi(), file);
            appendLog("Analisi salvata in " + file.getName());
        } catch (Exception ex) {
            errore("Salvataggio fallito", ex.getMessage());
        }
    }

    private void onCaricaAnalisi() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Carica analisi");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Analisi serializzata (*.dat)", "*.dat"));
        File dir = new File("data");
        if (dir.isDirectory()) {
            fc.setInitialDirectory(dir);
        }
        File file = fc.showOpenDialog(finestra());
        if (file == null) {
            return;
        }
        try {
            Map<String, DocumentoAnalizzato> risultati = gestoreSerializzazione.caricaAnalisi(file);
            analizzatore.setRisultatiAnalisi(risultati);
            appendLog("Analisi caricata da " + file.getName() + " (" + risultati.size() + " documento/i).");
        } catch (Exception ex) {
            errore("Caricamento fallito", "Il file selezionato e' corrotto o non compatibile.");
        }
    }

    private void onSpegniServer() {
        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION,
                "Spegnere il server? Tutti i client verranno disconnessi.",
                ButtonType.YES, ButtonType.NO);
        conferma.setHeaderText(null);
        conferma.showAndWait().ifPresent(scelta -> {
            if (scelta == ButtonType.YES) {
                gameServer.ferma();
                serverStatusLbl.setText("Offline");
                serverStatusLbl.setStyle("-fx-text-fill: red;");
                Platform.exit();
                System.exit(0);
            }
        });
    }

    //SEZIONE STATISTICHE
    private void aggiornaStatistiche() {
        if (partitaDAO == null) {
            return;
        }
        List<Object[]> righe = partitaDAO.statisticheGiocatori();
        ObservableList<RigaStatistica> dati = FXCollections.observableArrayList();
        for (Object[] r : righe) {
            String giocatore = (String) r[0];
            long vittorie = ((Number) r[1]).longValue();
            long partite = ((Number) r[2]).longValue();
            double tempoMedio = ((Number) r[3]).doubleValue();
            dati.add(new RigaStatistica(giocatore, vittorie, partite, tempoMedio));
        }
        statisticheAdminTable.setItems(dati);
    }

    //SEZIONE UTIILiTY
    private void appendLog(String riga) {
        if (logArea != null) {
            logArea.appendText(riga + "\n");
        }
    }

    private Window finestra() {
        return logArea.getScene().getWindow();
    }

    private void avviso(String titolo, String messaggio) {
        Alert a = new Alert(Alert.AlertType.WARNING, messaggio, ButtonType.OK);
        a.setHeaderText(titolo);
        a.showAndWait();
    }

    private void errore(String titolo, String messaggio) {
        Alert a = new Alert(Alert.AlertType.ERROR, messaggio, ButtonType.OK);
        a.setHeaderText(titolo);
        a.showAndWait();
    }

    /**
     * Riga della tabella delle statistiche per giocatore (view-model locale).
     */
    public static class RigaStatistica {
        private final String giocatore;
        private final long vittorie;
        private final long partite;
        private final double tempoMedio;

        public RigaStatistica(String giocatore, long vittorie, long partite, double tempoMedio) {
            this.giocatore = giocatore;
            this.vittorie = vittorie;
            this.partite = partite;
            this.tempoMedio = tempoMedio;
        }

        public String getGiocatore() {
            return giocatore;
        }

        public long getVittorie() {
            return vittorie;
        }

        public long getPartite() {
            return partite;
        }

        public String getTempoMedioFormattato() {
            if (vittorie == 0 || tempoMedio <= 0) {
                return "-";
            }
            return String.format("%.1f s", tempoMedio);
        }
    }
}
