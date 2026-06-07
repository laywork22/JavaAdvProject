package it.guesstheword.common.model;

public class Admin extends Utente{
    private static final long serialVersionUID=1L;

    public Admin(int id, String username, String password) {
        super(id, username, password);
    }

    public Admin() {
        super();
    }
}
