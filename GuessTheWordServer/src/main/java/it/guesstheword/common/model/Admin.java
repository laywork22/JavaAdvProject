package it.guesstheword.common.model;

public class Admin extends Utente{
    private static final long serialVersionUID=1L;

    /**
     * 
     * @param id        id admin
     * @param username  username admin  
     * @param password  password dell'admin
     */
    public Admin(int id, String username, String password) {
        super(id, username, password);
    }

    /** Costruttore vuoto */
    public Admin() {
        super();
    }
}
