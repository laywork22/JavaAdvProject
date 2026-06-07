package it.guesstheword.common.model;

public class Giocatore extends Utente {
    private static final long serialVersionUID = 1L;

    private boolean inPartita;

    public Giocatore(int id, String username, String password) {
        super(id, username,password);
        this.inPartita = false;
    }

    public Giocatore() {super();}

    public boolean isInPartita() {
        return inPartita;
    }

    public void setInPartita(boolean inPartita) {
        this.inPartita = inPartita;
    }
}
