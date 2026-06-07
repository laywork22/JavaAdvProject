package it.guesstheword.server.core;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Risultato dell'analisi di un singolo documento testuale.
 *
 * <p>Conserva il nome del documento, il testo integrale (utilizzato per
 * estrarre gli estratti durante la preparazione delle sfide) e la
 * <em>Term Frequency</em>, ovvero la mappa parola &rarr; numero di occorrenze.
 * La classe e' {@link Serializable} per consentire il salvataggio ed il
 * ricaricamento delle analisi (requisiti IF-4 / DF-6).</p>
 */
public class DocumentoAnalizzato implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String nome;
    private final String testo;
    private final Map<String, Long> frequenze;

    public DocumentoAnalizzato(String nome, String testo, Map<String, Long> frequenze) {
        this.nome = nome;
        this.testo = testo;
        this.frequenze = frequenze;
    }

    public String getNome() {
        return nome;
    }

    public String getTesto() {
        return testo;
    }

    /** @return la mappa (non modificabile) parola &rarr; frequenza. */
    public Map<String, Long> getFrequenze() {
        return Collections.unmodifiableMap(frequenze);
    }

    /** @return numero di parole distinte nel documento. */
    public int getNumeroParoleDistinte() {
        return frequenze.size();
    }

    @Override
    public String toString() {
        return nome + " (" + frequenze.size() + " parole distinte)";
    }
}
