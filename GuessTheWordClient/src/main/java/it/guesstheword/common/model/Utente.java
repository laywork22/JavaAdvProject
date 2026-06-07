package it.guesstheword.common.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Classe base astratta che modella un utente registrato nel sistema.
 * Le sottoclassi concrete sono {@link Giocatore} e {@code Admin}, distinte in
 * base al ruolo. Le credenziali vengono conservate in modo persistente sul
 * database lato server.
 */
public abstract class Utente implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String password;

    public Utente() {}

    public Utente(int id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Utente utente = (Utente) o;
        return id == utente.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Utente{" +
                "id=" + id +
                ", username='" + username + '\'' +
                '}';
    }
}
