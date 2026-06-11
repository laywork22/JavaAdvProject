package it.guesstheword.common.network;

/**
 * Enumerazione dei tipi di messaggio scambiati tra client e server attraverso
 * il protocollo applicativo basato su socket (oggetti {@link Messaggio}
 * serializzati su {@code ObjectInputStream}/{@code ObjectOutputStream}).
 *
 * <p>I valori sono divisi logicamente in messaggi inviati dal client verso il
 * server e messaggi inviati dal server verso il client.</p>
 */
public enum TipoMessaggio {

    /** Richiesta di autenticazione. Contenuto: {@code String[]{username, password}}. */
    LOGIN,
    /** Richiesta di registrazione. Contenuto: {@code String[]{username, password}}. */
    REGISTRAZIONE,
    /** Il giocatore chiede di entrare in matchmaking ("Nuova partita"). */
    CERCA_PARTITA,
    /** Il giocatore annulla la ricerca dell'avversario. */
    ANNULLA_ATTESA,
    /** Risposta del giocatore alla sfida. Contenuto: {@code String} (parola proposta). */
    RISPOSTA,
    /** Richiesta dello storico delle proprie partite. */
    RICHIESTA_STORICO,
    /** Il client comunica la propria disconnessione. */
    DISCONNESSIONE,

    
    /** Login riuscito. Contenuto: {@code Giocatore} autenticato. */
    LOGIN_OK,
    /** Login fallito. Contenuto: {@code String} (motivo). */
    LOGIN_FALLITO,
    /** Registrazione riuscita. */
    REGISTRAZIONE_OK,
    /** Registrazione fallita. Contenuto: {@code String} (motivo). */
    REGISTRAZIONE_FALLITA,
    /** Il client e' stato inserito nella coda di attesa dell'avversario. */
    IN_ATTESA,
    /** Inizio partita. Contenuto: {@link Sfida}. */
    INIZIO_PARTITA,
    /** Esito della sfida. Contenuto: {@link EsitoSfida}. */
    ESITO_PARTITA,
    /** Storico partite. Contenuto: {@code ArrayList<Partita>}. */
    STORICO,
    /** Errore generico. Contenuto: {@code String} (descrizione). */
    ERRORE
}
