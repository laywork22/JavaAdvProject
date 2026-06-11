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

    /**
     * Costruttore di default vuoto.
     */
    public Utente() {}


    /**
     * Costruisce un nuovo utente con i dati specificati
     * 
     * @param id        l'identificatore univoco dell'utente
     * @param username  il nome utente per l'accesso
     * @param password  la password dell'utente
     */
    public Utente(int id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    /**
     * 
     * @return l'username dell'utente
     */
    public String getUsername() {
        return username;
    }

    /**
     * 
     * @param username il nuovo username da impostare
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 
     * @return password dell'utente
     */
    public String getPassword() {
        return password;
    }

    /**
     * 
     * @param password la nuova password dell'utente
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 
     * @return id dell'utente
     */
    public int getId() {
        return id;
    }

    /**
     * 
     * @param id il nuovo id dell'utente
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Utente utente = (Utente) o;
        return id == utente.id;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Utente{" +
                "id=" + id +
                ", username='" + username + '\'' +
                '}';
    }
}
