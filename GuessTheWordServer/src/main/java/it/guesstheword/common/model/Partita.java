package it.guesstheword.common.model;

import java.io.Serializable;
import java.time.LocalDate;

public class Partita implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private LocalDate data;
    private int durata;
    private String esito;

    public Partita(int id, LocalDate data, int durata, String esito) {
        this.id = id;
        this.data = data;
        this.durata = durata;
        this.esito = esito;
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
}
