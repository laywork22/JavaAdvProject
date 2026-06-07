package it.guesstheword.server.core;

import it.guesstheword.common.model.Admin;
import it.guesstheword.common.model.Giocatore;
import it.guesstheword.server.dao.AdminDAO;
import it.guesstheword.server.dao.UtenteDAO;

/**
 * Servizio applicativo che incapsula la logica di autenticazione e
 * registrazione, appoggiandosi ai DAO per l'accesso al database.
 *
 * <p>Gestisce sia i giocatori (login/registrazione) sia l'amministratore
 * (login). Le credenziali sono confrontate con quelle persistite su database
 * (requisiti IF-2 / DF-2).</p>
 */
public class GestoreAutenticazione {

    private final UtenteDAO utenteDAO;
    private final AdminDAO adminDAO;

    public GestoreAutenticazione(UtenteDAO utenteDAO, AdminDAO adminDAO) {
        this.utenteDAO = utenteDAO;
        this.adminDAO = adminDAO;
    }

    /**
     * Autentica un giocatore.
     *
     * @param username username
     * @param password password
     * @return il {@link Giocatore} autenticato, oppure {@code null} se le
     *         credenziali non sono valide
     */
    public Giocatore login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        Giocatore g = utenteDAO.findGiocatoreByUsername(username.trim());
        if (g != null && g.getPassword().equals(password)) {
            return g;
        }
        return null;
    }

    /**
     * Registra un nuovo giocatore, se lo username non e' gia' in uso.
     *
     * @param username username scelto
     * @param password password scelta
     * @return {@code true} se la registrazione e' andata a buon fine
     */
    public boolean registra(String username, String password) {
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            return false;
        }
        String u = username.trim();
        if (utenteDAO.esisteUsername(u)) {
            return false;
        }
        return utenteDAO.inserisci(new Giocatore(0, u, password));
    }

    /**
     * Verifica se uno username e' gia' presente nel sistema.
     */
    public boolean esisteUsername(String username) {
        return username != null && utenteDAO.esisteUsername(username.trim());
    }

    /**
     * Autentica l'amministratore.
     *
     * @param username username admin
     * @param password password admin
     * @return l'{@link Admin} autenticato, oppure {@code null} se le credenziali
     *         non sono valide
     */
    public Admin loginAdmin(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        Admin a = adminDAO.findByUsername(username.trim());
        if (a != null && a.getPassword().equals(password)) {
            return a;
        }
        return null;
    }
}
