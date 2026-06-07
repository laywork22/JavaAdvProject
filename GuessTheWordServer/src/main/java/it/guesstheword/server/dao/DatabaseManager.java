package it.guesstheword.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestisce la connessione al database SQLite e l'inizializzazione dello schema.
 *
 * <p>Crea (se non esistono) le tabelle previste dal modello logico e pre-popola
 * il database con gli account di default necessari al testing: un
 * amministratore e due giocatori (requisito DF-7).</p>
 *
 * <p>Schema logico (derivato dall'E-R, con la generalizzazione di {@code Utente}
 * accorpata tramite la colonna {@code ruolo}):</p>
 * <pre>
 *   utenti(id, username, password, ruolo)
 *   partite(id, id_avversario1, id_avversario2, data, ora, durata, esito, id_vincitore)
 * </pre>
 */
public class DatabaseManager {

    /** Username dei due giocatori di default pre-popolati per il testing. */
    public static final String GIOCATORE1_DEFAULT = "player1";
    public static final String GIOCATORE2_DEFAULT = "player2";

    private final String dbUrl;

    public DatabaseManager(String dbUrl) {
        this.dbUrl = dbUrl;
        try {
            // Per driver/ambienti che non supportano l'auto-discovery JDBC 4.
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
            // Con sqlite-jdbc moderno il driver si registra automaticamente.
        }
    }

    /**
     * Apre una nuova connessione al database.
     *
     * @return una {@link Connection} aperta
     * @throws SQLException in caso di errore di connessione
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Crea lo schema (se assente) ed inserisce gli account di default.
     *
     * @param adminUsername username dell'amministratore di default
     * @param adminPassword password dell'amministratore di default
     */
    public void inizializza(String adminUsername, String adminPassword) {
        creaTabelle();
        seedAccountDefault(adminUsername, adminPassword);
    }

    private void creaTabelle() {
        String creaUtenti =
                "CREATE TABLE IF NOT EXISTS utenti (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  username TEXT NOT NULL UNIQUE," +
                "  password TEXT NOT NULL," +
                "  ruolo TEXT NOT NULL DEFAULT 'GIOCATORE'" +
                ")";

        String creaPartite =
                "CREATE TABLE IF NOT EXISTS partite (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  id_avversario1 INTEGER NOT NULL," +
                "  id_avversario2 INTEGER NOT NULL," +
                "  data TEXT NOT NULL," +
                "  ora TEXT," +
                "  durata INTEGER NOT NULL DEFAULT 0," +
                "  esito TEXT," +
                "  id_vincitore INTEGER," +
                "  FOREIGN KEY (id_avversario1) REFERENCES utenti(id)," +
                "  FOREIGN KEY (id_avversario2) REFERENCES utenti(id)," +
                "  FOREIGN KEY (id_vincitore) REFERENCES utenti(id)" +
                ")";

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.execute(creaUtenti);
            st.execute(creaPartite);
        } catch (SQLException e) {
            throw new RuntimeException("Errore nella creazione dello schema del database", e);
        }
    }

    private void seedAccountDefault(String adminUsername, String adminPassword) {
        // INSERT OR IGNORE: grazie al vincolo UNIQUE su username, gli account
        // gia' presenti non vengono duplicati.
        String sql = "INSERT OR IGNORE INTO utenti (username, password, ruolo) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            inserisciDefault(ps, adminUsername, adminPassword, "ADMIN");
            inserisciDefault(ps, GIOCATORE1_DEFAULT, GIOCATORE1_DEFAULT, "GIOCATORE");
            inserisciDefault(ps, GIOCATORE2_DEFAULT, GIOCATORE2_DEFAULT, "GIOCATORE");

        } catch (SQLException e) {
            throw new RuntimeException("Errore nel pre-popolamento degli account di default", e);
        }
    }

    private void inserisciDefault(java.sql.PreparedStatement ps, String username,
                                  String password, String ruolo) throws SQLException {
        ps.setString(1, username);
        ps.setString(2, password);
        ps.setString(3, ruolo);
        ps.executeUpdate();
    }
}
