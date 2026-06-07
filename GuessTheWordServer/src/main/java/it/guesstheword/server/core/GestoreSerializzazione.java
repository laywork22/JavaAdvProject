package it.guesstheword.server.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gestisce la persistenza dei risultati delle analisi tramite
 * <strong>serializzazione</strong> Java (requisiti IF-4 / DF-6 / UC-10).
 *
 * <p>I risultati (mappa nome documento &rarr; {@link DocumentoAnalizzato})
 * vengono salvati su file con {@link ObjectOutputStream} e ricaricati con
 * {@link ObjectInputStream}, evitando di ripetere l'analisi ad ogni avvio.</p>
 */
public class GestoreSerializzazione {

    /**
     * Serializza un oggetto su file.
     *
     * @param oggetto oggetto serializzabile
     * @param file    file di destinazione
     * @throws IOException in caso di errore di scrittura
     */
    public void salva(Serializable oggetto, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            oos.writeObject(oggetto);
        }
    }

    /**
     * Deserializza un oggetto da file.
     *
     * @param file file sorgente
     * @param <T>  tipo atteso
     * @return l'oggetto deserializzato
     * @throws IOException            in caso di errore di lettura
     * @throws ClassNotFoundException se la classe serializzata non e' disponibile
     */
    @SuppressWarnings("unchecked")
    public <T> T carica(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            return (T) ois.readObject();
        }
    }

    /**
     * Salva i risultati di un'analisi su file.
     *
     * @param risultati mappa nome &rarr; documento analizzato
     * @param file      file di destinazione
     * @throws IOException in caso di errore di scrittura
     */
    public void salvaAnalisi(Map<String, DocumentoAnalizzato> risultati, File file) throws IOException {
        salva(new LinkedHashMap<>(risultati), file);
    }

    /**
     * Carica da file i risultati di un'analisi precedentemente salvata.
     *
     * @param file file sorgente
     * @return mappa nome &rarr; documento analizzato
     * @throws IOException            in caso di errore di lettura
     * @throws ClassNotFoundException se il file e' incompatibile/corrotto
     */
    public Map<String, DocumentoAnalizzato> caricaAnalisi(File file)
            throws IOException, ClassNotFoundException {
        Object obj = carica(file);
        if (!(obj instanceof Map)) {
            throw new IOException("Il file selezionato non contiene un'analisi valida.");
        }
        @SuppressWarnings("unchecked")
        Map<String, DocumentoAnalizzato> risultati = (Map<String, DocumentoAnalizzato>) obj;
        return risultati;
    }
}
