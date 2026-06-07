package it.guesstheword.common.network;

import java.io.Serializable;

/**
 * Rappresenta una sfida (round di gioco) inviata dal server ai due client.
 *
 * <p>Verso i client vengono trasmessi il testo cifrato e la durata del timer.
 * La soluzione ({@link #parolaOriginale}) e lo {@link #shift} del cifrario di
 * Cesare sono dichiarati {@code transient}: restano quindi disponibili solo
 * lato server (che mantiene in memoria l'istanza originale) e non vengono
 * serializzati verso i client. In questo modo non e' possibile "barare"
 * leggendo la risposta dal traffico di rete.</p>
 */
public class Sfida implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Testo dell'estratto con la/e parola/e sostituita/e dalla versione cifrata. */
    private String testoCifrato;
    /** Durata del conto alla rovescia in secondi. */
    private int durataSecondi;
    /** Numero di parole nascoste (cifrate) all'interno del testo. */
    private int numeroParoleCifrate;

    /** Soluzione: parola originale. Solo lato server (non serializzata). */
    private transient String parolaOriginale;
    /** Shift del cifrario di Cesare utilizzato. Solo lato server. */
    private transient int shift;

    public Sfida() {
    }

    public Sfida(String testoCifrato, int durataSecondi, int numeroParoleCifrate) {
        this.testoCifrato = testoCifrato;
        this.durataSecondi = durataSecondi;
        this.numeroParoleCifrate = numeroParoleCifrate;
    }

    public String getTestoCifrato() {
        return testoCifrato;
    }

    public void setTestoCifrato(String testoCifrato) {
        this.testoCifrato = testoCifrato;
    }

    public int getDurataSecondi() {
        return durataSecondi;
    }

    public void setDurataSecondi(int durataSecondi) {
        this.durataSecondi = durataSecondi;
    }

    public int getNumeroParoleCifrate() {
        return numeroParoleCifrate;
    }

    public void setNumeroParoleCifrate(int numeroParoleCifrate) {
        this.numeroParoleCifrate = numeroParoleCifrate;
    }

    public String getParolaOriginale() {
        return parolaOriginale;
    }

    public void setParolaOriginale(String parolaOriginale) {
        this.parolaOriginale = parolaOriginale;
    }

    public int getShift() {
        return shift;
    }

    public void setShift(int shift) {
        this.shift = shift;
    }
}
