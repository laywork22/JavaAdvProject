package it.guesstheword.client.authentication;

import it.guesstheword.client.network.ServerConnection;
import it.guesstheword.common.network.Messaggio;
import it.guesstheword.common.network.TipoMessaggio;

/**
 * Implementazione di {@link LoginAuth} che inoltra le richieste di
 * autenticazione al server tramite la {@link ServerConnection}.
 */
public class ClientAuth implements LoginAuth {

    private final ServerConnection connection;

    public ClientAuth(ServerConnection connection) {
        this.connection = connection;
    }

    @Override
    public void login(String username, String password) {
        connection.invia(new Messaggio(TipoMessaggio.LOGIN, new String[]{username, password}));
    }

    @Override
    public void registra(String username, String password) {
        connection.invia(new Messaggio(TipoMessaggio.REGISTRAZIONE, new String[]{username, password}));
    }
}
