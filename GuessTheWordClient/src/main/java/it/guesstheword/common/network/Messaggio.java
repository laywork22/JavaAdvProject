package it.guesstheword.common.network;

import java.io.Serializable;

/**
 * Messaggio generico scambiato tra client e server.
 *
 * <p>Incapsula un {@link TipoMessaggio} ed un eventuale contenuto
 * serializzabile (credenziali, {@link Sfida}, {@link EsitoSfida}, liste di
 * partite, stringhe di errore, ecc.). Costituisce l'unita' di base del
 * protocollo applicativo.</p>
 */
public class Messaggio implements Serializable {

    private static final long serialVersionUID = 1L;

    private TipoMessaggio tipo;
    private Object contenuto;

    public Messaggio() {
    }

    public Messaggio(TipoMessaggio tipo) {
        this.tipo = tipo;
    }

    public Messaggio(TipoMessaggio tipo, Object contenuto) {
        this.tipo = tipo;
        this.contenuto = contenuto;
    }

    public TipoMessaggio getTipo() {
        return tipo;
    }

    public void setTipo(TipoMessaggio tipo) {
        this.tipo = tipo;
    }

    public Object getContenuto() {
        return contenuto;
    }

    public void setContenuto(Object contenuto) {
        this.contenuto = contenuto;
    }

    @Override
    public String toString() {
        return "Messaggio{tipo=" + tipo + ", contenuto=" + contenuto + '}';
    }
}
