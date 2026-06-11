package it.guesstheword.common.model;

public class Giocatore extends Utente {
    private static final long serialVersionUID = 1L;

    private boolean inPartita;

    /**
     * 
     * @param id        id del giocatore
     * @param username  username del giocatore
     * @param password  password del giocatore
     */
    public Giocatore(int id, String username, String password) {
        super(id, username,password);
        this.inPartita = false;
    }

    /** Costruttore vuoto */
    public Giocatore() {super();}

    /**
     * 
     * @return lo stato del Giocatore (in partita o nella dashboard)
     */
    public boolean isInPartita() {
        return inPartita;
    }

    /**
     * 
     * @param inPartita il nuovo stato del giocatore
     */
    public void setInPartita(boolean inPartita) {
        this.inPartita = inPartita;
    }
}
