package it.guesstheword.server.dao;

import it.guesstheword.common.model.Admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO per l'amministratore, mappato sulle righe della tabella {@code utenti}
 * aventi ruolo {@code ADMIN}.
 */
public class AdminDAO implements DAO<Admin> {

    private static final String RUOLO = "ADMIN";

    private final DatabaseManager db;

    public AdminDAO(DatabaseManager db) {
        this.db = db;
    }

    /** {@inheritDoc} */
    @Override
    public Admin findById(int id) {
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
            System.err.println("[AdminDAO] findById: " + e.getMessage());
        }
        return null;
    }

    /**
     * @param username username dell'amministratore
     * @return l'amministratore corrispondente, oppure {@code null}
     */
    public Admin findByUsername(String username) {
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
            System.err.println("[AdminDAO] findByUsername: " + e.getMessage());
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean inserisci(Admin admin) {
        String sql = "INSERT INTO utenti (username, password, ruolo) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, admin.getUsername());
            ps.setString(2, admin.getPassword());
            ps.setString(3, RUOLO);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AdminDAO] inserisci: " + e.getMessage());
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Admin> findAll() {
        String sql = "SELECT id, username, password FROM utenti WHERE ruolo = ? ORDER BY username";
        List<Admin> lista = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, RUOLO);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mappa(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AdminDAO] findAll: " + e.getMessage());
        }
        return lista;
    }

    private Admin mappa(ResultSet rs) throws SQLException {
        return new Admin(rs.getInt("id"), rs.getString("username"), rs.getString("password"));
    }
}
