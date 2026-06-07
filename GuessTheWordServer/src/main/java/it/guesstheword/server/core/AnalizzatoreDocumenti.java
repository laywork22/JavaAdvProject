package it.guesstheword.server.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Si occupa del caricamento e dell'analisi dei documenti testuali (file TXT).
 *
 * <p>L'analisi consiste nel calcolo della <em>Term Frequency</em> tramite
 * <strong>Java Stream API</strong>. I documenti caricati ed i risultati delle
 * analisi sono mantenuti in memoria; i risultati possono essere esportati e
 * reimportati (serializzazione) tramite {@link GestoreSerializzazione}.</p>
 *
 * <p>Le collezioni interne sono {@code thread-safe} poiche' l'analisi viene
 * tipicamente eseguita su un thread in background (JavaFX {@code Task}) mentre
 * la preparazione delle sfide avviene sui thread dei {@code ClientHandler}.</p>
 */
public class AnalizzatoreDocumenti {

    /** Documenti caricati ma non ancora analizzati: nome &rarr; testo. */
    private final Map<String, String> documentiCaricati = new ConcurrentHashMap<>();

    /** Documenti analizzati: nome &rarr; risultato dell'analisi. */
    private final Map<String, DocumentoAnalizzato> documentiAnalizzati = new ConcurrentHashMap<>();

    private final Random random = new Random();

    /**
     * Legge il contenuto di un file di testo (UTF-8) e lo registra tra i
     * documenti caricati, pronti per l'analisi.
     *
     * @param file file TXT da caricare
     * @throws IOException se il file non e' leggibile o e' vuoto
     */
    public void caricaDocumento(File file) throws IOException {
        String testo = leggiFile(file);
        if (testo.trim().isEmpty()) {
            throw new IOException("Il documento '" + file.getName() + "' e' vuoto.");
        }
        documentiCaricati.put(file.getName(), testo);
    }

    /** Legge integralmente un file di testo in UTF-8. */
    public static String leggiFile(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Analizza tutti i documenti attualmente caricati calcolandone la Term
     * Frequency.
     *
     * @return la lista dei documenti analizzati in questa esecuzione
     */
    public List<DocumentoAnalizzato> analizzaTutti() {
        List<DocumentoAnalizzato> risultati = new ArrayList<>();
        for (Map.Entry<String, String> e : documentiCaricati.entrySet()) {
            risultati.add(analizza(e.getKey(), e.getValue()));
        }
        return risultati;
    }

    /**
     * Analizza un singolo documento e ne memorizza il risultato.
     *
     * @param nome  nome del documento
     * @param testo contenuto testuale
     * @return il {@link DocumentoAnalizzato} prodotto
     */
    public DocumentoAnalizzato analizza(String nome, String testo) {
        Map<String, Long> frequenze = calcolaTermFrequency(testo);
        DocumentoAnalizzato doc = new DocumentoAnalizzato(nome, testo, frequenze);
        documentiAnalizzati.put(nome, doc);
        return doc;
    }

    /**
     * Calcola la Term Frequency di un testo usando le Stream API: il testo
     * viene normalizzato in minuscolo, suddiviso in parole (sequenze di
     * lettere, anche accentate) e raggruppato per conteggio. Il risultato e'
     * ordinato per frequenza decrescente.
     *
     * @param testo testo da analizzare
     * @return mappa ordinata parola &rarr; frequenza
     */
    public static Map<String, Long> calcolaTermFrequency(String testo) {
        Map<String, Long> conteggio = Arrays.stream(testo.toLowerCase().split("[^\\p{L}]+"))
                .filter(parola -> parola.length() > 0)
                .collect(Collectors.groupingBy(parola -> parola, Collectors.counting()));

        // ordinamento per frequenza decrescente (poi alfabetico)
        return conteggio.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Long.compare(b.getValue(), a.getValue());
                    return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (x, y) -> x,
                        LinkedHashMap::new));
    }

    /** @return i documenti analizzati attualmente disponibili. */
    public Collection<DocumentoAnalizzato> getDocumentiAnalizzati() {
        return documentiAnalizzati.values();
    }

    /** @return i nomi dei documenti caricati (analizzati e non). */
    public Map<String, String> getDocumentiCaricati() {
        return documentiCaricati;
    }

    /** @return {@code true} se esiste almeno un documento analizzato. */
    public boolean haDocumentiAnalizzati() {
        return !documentiAnalizzati.isEmpty();
    }

    /**
     * Seleziona casualmente uno dei documenti analizzati (requisiti IF-5 / IF-15).
     *
     * @return un documento analizzato a caso, oppure {@code null} se non ve ne sono
     */
    public DocumentoAnalizzato selezionaCasuale() {
        if (documentiAnalizzati.isEmpty()) {
            return null;
        }
        List<DocumentoAnalizzato> lista = new ArrayList<>(documentiAnalizzati.values());
        return lista.get(random.nextInt(lista.size()));
    }

    /**
     * Sostituisce l'insieme dei risultati analizzati (usato in fase di
     * ricaricamento di un'analisi serializzata).
     */
    public void setRisultatiAnalisi(Map<String, DocumentoAnalizzato> risultati) {
        documentiAnalizzati.clear();
        if (risultati != null) {
            documentiAnalizzati.putAll(risultati);
        }
    }

    /** @return una copia dei risultati analizzati, adatta alla serializzazione. */
    public Map<String, DocumentoAnalizzato> getRisultatiAnalisi() {
        return new LinkedHashMap<>(documentiAnalizzati);
    }
}
