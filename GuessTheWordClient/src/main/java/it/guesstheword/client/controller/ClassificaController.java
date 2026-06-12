package it.guesstheword.client.controller;

import it.guesstheword.common.model.Partita;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.util.List;
import java.util.OptionalInt;

/**
 * Calcola e visualizza le statistiche personali del giocatore (UC-07,
 * lato client): partite totali, win rate, miglior tempo, andamento dei tempi di
 * risposta (AreaChart) e rapporto vittorie/sconfitte (BarChart).
 *
 * <p>Non e' un controller FXML: viene istanziato da {@link DashboardController}
 * passando le etichette ed i grafici da popolare a partire dallo storico
 * partite ricevuto dal server.</p>
 */
public class ClassificaController {

    private static final String VITTORIA = "Vittoria";
    private static final String SCONFITTA = "Sconfitta";
    private static final String PAREGGIO = "Pareggio";

    private final Label partiteTotaliLbl;
    private final Label winRateLbl;
    private final Label migliorTempoLbl;
    private final AreaChart<String, Number> tempiChart;
    private final BarChart<String, Number> ratioChart;

    public ClassificaController(Label partiteTotaliLbl, Label winRateLbl, Label migliorTempoLbl,
                                AreaChart<String, Number> tempiChart, BarChart<String, Number> ratioChart) {
        this.partiteTotaliLbl = partiteTotaliLbl;
        this.winRateLbl = winRateLbl;
        this.migliorTempoLbl = migliorTempoLbl;
        this.tempiChart = tempiChart;
        this.ratioChart = ratioChart;
    }

    /**
     * Aggiorna etichette e grafici in base allo storico fornito.
     *
     * @param partite storico delle partite del giocatore
     */
    public void aggiorna(List<Partita> partite) {
        int totali = partite.size();
        long vittorie = partite.stream().filter(p -> VITTORIA.equals(p.getEsito())).count();
        long sconfitte = partite.stream().filter(p -> SCONFITTA.equals(p.getEsito())).count();
        long pareggi = partite.stream().filter(p -> PAREGGIO.equals(p.getEsito())).count();

        double winRate = totali > 0 ? (vittorie * 100.0 / totali) : 0.0;
        OptionalInt migliorTempo = partite.stream()
                .filter(p -> VITTORIA.equals(p.getEsito()))
                .mapToInt(Partita::getDurata)
                .filter(d -> d > 0)
                .min();

        partiteTotaliLbl.setText(String.valueOf(totali));
        winRateLbl.setText(String.format("%.0f%%", winRate));
        migliorTempoLbl.setText(migliorTempo.isPresent() ? migliorTempo.getAsInt() + " s" : "-");

        aggiornaGraficoTempi(partite);
        aggiornaGraficoRapporto(vittorie, sconfitte, pareggi);
    }

    private void aggiornaGraficoTempi(List<Partita> partite) {
        tempiChart.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Tempo di risposta (s)");
        // i tempi di risposta noti sono quelli delle partite vinte dal giocatore
        // (durata 0 = vittoria per abbandono, senza tempo di risposta)
        partite.stream()
                .filter(p -> VITTORIA.equals(p.getEsito()) && p.getDurata() > 0)
                .sorted((a, b) -> Integer.compare(a.getId(), b.getId()))
                .forEach(p -> serie.getData().add(
                        new XYChart.Data<>("#" + p.getId(), p.getDurata())));
        tempiChart.getData().add(serie);
    }

    private void aggiornaGraficoRapporto(long vittorie, long sconfitte, long pareggi) {
        ratioChart.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Partite");
        serie.getData().add(new XYChart.Data<>("Vittorie", vittorie));
        serie.getData().add(new XYChart.Data<>("Sconfitte", sconfitte));
        serie.getData().add(new XYChart.Data<>("Pareggi", pareggi));
        ratioChart.getData().add(serie);
    }
}
