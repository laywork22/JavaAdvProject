package it.guesstheword.server.authentication;

import it.guesstheword.common.model.Admin;
import it.guesstheword.server.core.GestoreAutenticazione;

/**
 * Autenticazione dell'amministratore per l'accesso all'interfaccia di gestione
 * del server. Si appoggia a {@link GestoreAutenticazione} (e quindi al
 * database) per la verifica delle credenziali.
 */
public class ServerAdminAuth implements LoginAuth {

    private final GestoreAutenticazione gestoreAutenticazione;
    private Admin adminAutenticato;

    /**
     * 
     * @param gestoreAutenticazione il gestore autenticazione di quell'admin
     */
    public ServerAdminAuth(GestoreAutenticazione gestoreAutenticazione) {
        this.gestoreAutenticazione = gestoreAutenticazione;
    }

    /** {@inheritDoc} */
    @Override
    public boolean login(String username, String password) {
        Admin admin = gestoreAutenticazione.loginAdmin(username, password);
        this.adminAutenticato = admin;
        return admin != null;
    }

    /**
     * @return l'amministratore autenticato dall'ultimo login riuscito, oppure
     *         {@code null}
     */
    public Admin getAdminAutenticato() {
        return adminAutenticato;
    }
}
