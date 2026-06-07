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

    public Partita() {
    }

    public Partita(int id, LocalDate data, int durata, String esito) {
        this.id = id;
        this.data = data;
        this.durata = durata;
        this.esito = esito;
    }

    public Partita(int id, LocalDate data, int durata, String esito, String avversario) {
        this.id = id;
        this.data = data;
        this.durata = durata;
        this.esito = esito;
        this.avversario = avversario;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public int getDurata() {
        return durata;
    }

    public void setDurata(int durata) {
        this.durata = durata;
    }

    public String getEsito() {
        return esito;
    }

    public void setEsito(String esito) {
        this.esito = esito;
    }

    public String getAvversario() {
        return avversario;
    }

    public void setAvversario(String avversario) {
        this.avversario = avversario;
    }
}
