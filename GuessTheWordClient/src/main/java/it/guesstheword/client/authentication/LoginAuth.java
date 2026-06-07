package it.guesstheword.client.authentication;

/**
 * Contratto per l'autenticazione lato client.
 *
 * <p>Le operazioni sono asincrone: inviano una richiesta al server tramite la
 * connessione socket; l'esito (successo/errore) viene ricevuto in seguito sotto
 * forma di {@link it.guesstheword.common.network.Messaggio} e gestito dal
 * dispatcher dell'applicazione.</p>
 */
public interface LoginAuth {

    /**
     * Invia una richiesta di login.
     *
     * @param username username
     * @param password password
     */
    void login(String username, String password);

    /**
     * Invia una richiesta di registrazione.
     *
     * @param username username
     * @param password password
     */
    void registra(String username, String password);
}
