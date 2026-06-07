package it.guesstheword.common.network;

import java.io.Serializable;

/**
 * Esito di una sfida, notificato ad entrambi i client al termine della partita.
 *
 * <p>L'oggetto e' "neutro": viene inviato identico ai due giocatori. Ciascun
 * client confronta {@link #getVincitore()} con il proprio username per
 * determinare se ha vinto o perso. Se {@link #isTempoScaduto()} e' {@code true}
 * la partita e' terminata per scadenza del timer senza vincitore.</p>
 */
public class EsitoSfida implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Username del vincitore, {@code null} se nessun vincitore (timeout). */
    private String vincitore;
    /** Soluzione rivelata a fine partita. */
    private String parolaOriginale;
    /** {@code true} se la partita e' finita per scadenza del tempo. */
    private boolean tempoScaduto;
    /** Messaggio descrittivo leggibile dall'utente. */
    private String messaggio;

    public EsitoSfida() {
    }

    public EsitoSfida(String vincitore, String parolaOriginale, boolean tempoScaduto, String messaggio) {
        this.vincitore = vincitore;
        this.parolaOriginale = parolaOriginale;
        this.tempoScaduto = tempoScaduto;
        this.messaggio = messaggio;
    }

    public String getVincitore() {
        return vincitore;
    }

    public void setVincitore(String vincitore) {
        this.vincitore = vincitore;
    }

    public String getParolaOriginale() {
        return parolaOriginale;
    }

    public void setParolaOriginale(String parolaOriginale) {
        this.parolaOriginale = parolaOriginale;
    }

    public boolean isTempoScaduto() {
        return tempoScaduto;
    }

    public void setTempoScaduto(boolean tempoScaduto) {
        this.tempoScaduto = tempoScaduto;
    }

    public String getMessaggio() {
        return messaggio;
    }

    public void setMessaggio(String messaggio) {
        this.messaggio = messaggio;
    }
}
