package it.guesstheword.server.concurrency;

import it.guesstheword.server.core.AnalizzatoreDocumenti;
import it.guesstheword.server.core.DocumentoAnalizzato;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link Task} JavaFX che esegue l'analisi dei documenti caricati su un thread
 * in background, evitando di bloccare l'interfaccia grafica (requisito FC-2,
 * UC-09).
 *
 * <p>Per ogni documento gia' caricato nell'{@link AnalizzatoreDocumenti}
 * calcola la Term Frequency aggiornando avanzamento ({@code progress}) e
 * messaggio ({@code message}), ai quali l'interfaccia amministratore puo' fare
 * binding. Un documento problematico non interrompe l'elaborazione degli altri
 * (UC-09, flusso alternativo 4a).</p>
 */
public class AnalisiTask extends Task<List<DocumentoAnalizzato>> {

    private final AnalizzatoreDocumenti analizzatore;

    public AnalisiTask(AnalizzatoreDocumenti analizzatore) {
        this.analizzatore = analizzatore;
    }

    @Override
    protected List<DocumentoAnalizzato> call() {
        List<Map.Entry<String, String>> documenti =
                new ArrayList<>(analizzatore.getDocumentiCaricati().entrySet());
        int totale = Math.max(1, documenti.size());
        int elaborati = 0;

        List<DocumentoAnalizzato> risultati = new ArrayList<>();
        updateProgress(0, totale);

        for (Map.Entry<String, String> doc : documenti) {
            if (isCancelled()) {
                break;
            }
            updateMessage("Analisi di " + doc.getKey() + " in corso...");
            try {
                DocumentoAnalizzato analizzato = analizzatore.analizza(doc.getKey(), doc.getValue());
                risultati.add(analizzato);
                updateMessage("Analizzato " + analizzato.getNome()
                        + " (" + analizzato.getNumeroParoleDistinte() + " parole distinte)");
            } catch (Exception e) {
                updateMessage("ERRORE: impossibile analizzare " + doc.getKey()
                        + " (" + e.getMessage() + ")");
            }
            elaborati++;
            updateProgress(elaborati, totale);
        }

        updateMessage("Analisi completata: " + risultati.size() + " documento/i analizzati.");
        return risultati;
    }
}
