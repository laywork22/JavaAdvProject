package it.guesstheword.server.dao;

import java.util.List;

/**
 * Interfaccia generica per i Data Access Object.
 *
 * <p>Definisce le operazioni di base di accesso ad un'entita' persistita su
 * database, indipendentemente dal tipo concreto.</p>
 *
 * @param <T> tipo dell'entita' gestita
 */
public interface DAO<T> {

    /**
     * @param id identificatore dell'entita'
     * @return l'entita' corrispondente, oppure {@code null} se non esiste
     */
    T findById(int id);

    /**
     * @return tutte le entita' presenti
     */
    List<T> findAll();

    /**
     * Persiste una nuova entita'.
     *
     * @param entita entita' da inserire
     * @return {@code true} se l'inserimento e' andato a buon fine
     */
    boolean inserisci(T entita);
}
