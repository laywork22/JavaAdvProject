package it.guesstheword.server.authentication;

/**
 * Astrazione del servizio di autenticazione lato server.
 *
 * <p>Definisce il contratto comune per la verifica delle credenziali; viene
 * implementata, ad esempio, da {@link ServerAdminAuth} per l'autenticazione
 * dell'amministratore.</p>
 */
public interface LoginAuth {

    /**
     * Verifica le credenziali fornite.
     *
     * @param username username
     * @param password password
     * @return {@code true} se le credenziali sono valide
     */
    boolean login(String username, String password);
}
