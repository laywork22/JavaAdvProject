package it.guesstheword.common.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Modella una partita disputata tra due giocatori.
 *
 * <p>Lato server l'entita' viene persistita con i riferimenti ad entrambi gli
 * avversari ed all'eventuale vincitore. Lato client, quando viene costruito lo
 * storico personale di un utente, il campo {@link #esito} assume il punto di
 * vista di quell'utente ("Vittoria" / "Sconfitta" / "Pareggio") ed il campo
 * {@link #avversario} contiene lo username dell'altro giocatore (cosi' da
 * popolare la relativa colonna della tabella dello storico).</p>
 */
public class Partita implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private LocalDate data;
    private int durata;
    private String esito;
    private String avversario;

    /**
     * Costruttore di default vuoto.
     */
    public Partita() {
    }

    /**
     * Costruisce una nuova partita senza specificare l'avversario.
     * @param id     l'identificativo univoco della partita
     * @param data   la data di inizio e conclusione della partita 
     * @param durata la durata della partita (es. in secondi)
     * @param esito  l'esito della partita ("Vittoria", "Sconfitta", "Pareggio")
     */
    public Partita(int id, LocalDate data, int durata, String esito) {
        this.id = id;
        this.data = data;
        this.durata = durata;
        this.esito = esito;
    }

    /**
     * Costruisce una nuova partita completa di tutti i dettagli, incluso l'avversario.
     * @param id          l'identificativo univoco della partita
     * @param data        la data di inizio e conclusione della partita 
     * @param durata      la durata della partita (es. in secondi)
     * @param esito       l'esito della partita ("Vittoria", "Sconfitta", "Pareggio")
     * @param avversario  lo username dell'avversario affrontato
     */
    public Partita(int id, LocalDate data, int durata, String esito, String avversario) {
        this.id = id;
        this.data = data;
        this.durata = durata;
        this.esito = esito;
        this.avversario = avversario;
    }

    /** * @return l'identificativo univoco della partita. 
     */
    public int getId() {
        return id;
    }

    /** * @param id il nuovo identificativo da impostare per la partita. 
     */
    public void setId(int id) {
        this.id = id;
    }

    /** * @return la data in cui si è disputata la partita. 
     */
    public LocalDate getData() {
        return data;
    }

    /** * @param data la data di svolgimento da impostare. 
     */
    public void setData(LocalDate data) {
        this.data = data;
    }

    /** * @return la durata complessiva della partita. 
     */
    public int getDurata() {
        return durata;
    }

    /** * @param durata la durata da impostare per la partita. 
     */
    public void setDurata(int durata) {
        this.durata = durata;
    }

    /** * @return l'esito della partita dal punto di vista dell'utente che la visualizza. 
     */
    public String getEsito() {
        return esito;
    }

    /** * @param esito l'esito da impostare (es. "Vittoria", "Sconfitta"). 
     */
    public void setEsito(String esito) {
        this.esito = esito;
    }

    /** * @return lo username del giocatore avversario. 
     */
    public String getAvversario() {
        return avversario;
    }

    /** * @param avversario lo username dell'avversario da impostare. 
     */
    public void setAvversario(String avversario) {
        this.avversario = avversario;
    }
}