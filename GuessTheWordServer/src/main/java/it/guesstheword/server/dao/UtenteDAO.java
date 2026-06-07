package it.guesstheword.server.dao;

import it.guesstheword.common.model.Giocatore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO per i giocatori, mappati sulle righe della tabella {@code utenti} aventi
 * ruolo {@code GIOCATORE}.
 */
public class UtenteDAO implements DAO<Giocatore> {

    private static final String RUOLO = "GIOCATORE";

    private final DatabaseManager db;

    public UtenteDAO(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Giocatore findById(int id) {
        String sql = "SELECT id, username, password FROM utenti WHERE id = ? AND ruolo = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, RUOLO);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mappa(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UtenteDAO] findById: " + e.getMessage());
        }
        return null;
    }

    /**
     * @param username username da cercare
     * @return il giocatore con quello username, oppure {@code null}
     */
    public Giocatore findGiocatoreByUsername(String username) {
        String sql = "SELECT id, username, password FROM utenti WHERE username = ? AND ruolo = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, RUOLO);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mappa(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UtenteDAO] findGiocatoreByUsername: " + e.getMessage());
        }
        return null;
    }

    /**
     * Verifica l'esistenza di uno username, indipendentemente dal ruolo (per
     * evitare collisioni tra account amministratore e giocatori).
     */
    public boolean esisteUsername(String username) {
        String sql = "SELECT 1 FROM utenti WHERE username = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[UtenteDAO] esisteUsername: " + e.getMessage());
            return true; // in caso di dubbio non consentire la registrazione
        }
    }

    @Override
    public boolean inserisci(Giocatore giocatore) {
        String sql = "INSERT INTO utenti (username, password, ruolo) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, giocatore.getUsername());
            ps.setString(2, giocatore.getPassword());
            ps.setString(3, RUOLO);
            int righe = ps.executeUpdate();
            if (righe > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        giocatore.setId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[UtenteDAO] inserisci: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<Giocatore> findAll() {
        String sql = "SELECT id, username, password FROM utenti WHERE ruolo = ? ORDER BY username";
        List<Giocatore> lista = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, RUOLO);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mappa(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[UtenteDAO] findAll: " + e.getMessage());
        }
        return lista;
    }

    private Giocatore mappa(ResultSet rs) throws SQLException {
        return new Giocatore(rs.getInt("id"), rs.getString("username"), rs.getString("password"));
    }
}
