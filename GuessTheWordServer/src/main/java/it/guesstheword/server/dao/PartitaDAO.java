package it.guesstheword.server.dao;

import it.guesstheword.common.model.Partita;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**Requisiti (IF-13, DF-3, DF-4, DF-5, UC-07, UC-11, UC-13).
 * 
 * Data Access Object per la gestione delle Partite e dello Storico.
 * NOTA ARCHITETTURALE: Questa classe implementa il pattern DAO ma non 
 * estende l'interfaccia generica DAO<T> in quanto le entità Partita 
 * richiedono operazioni di lettura context-dependent (es. l'esito 
 * dipende dall'utente che interroga lo storico) e query di aggregazione 
 * per le statistiche che non si sposano con le firme CRUD standard.
 */
public class PartitaDAO {

    private final DatabaseManager db;

    public PartitaDAO(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Registra una partita conclusa, con riferimento temporale (data e ora).
     *
     * @param idAvversario1 id del primo giocatore
     * @param idAvversario2 id del secondo giocatore
     * @param idVincitore   id del vincitore, oppure {@code null} in caso di
     *                      pareggio/timeout
     * @param durata        tempo di risposta del vincitore in secondi
     * @param esito         descrizione testuale dell'esito
     * @return l'id generato per la partita, oppure -1 in caso di errore
     */
    public int registraPartita(int idAvversario1, int idAvversario2, Integer idVincitore,
                               int durata, String esito) {
        String sql = "INSERT INTO partite " +
                "(id_avversario1, id_avversario2, data, ora, durata, esito, id_vincitore) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idAvversario1);
            ps.setInt(2, idAvversario2);
            ps.setString(3, LocalDate.now().toString());
            ps.setString(4, LocalTime.now().withNano(0).toString());
            ps.setInt(5, durata);
            ps.setString(6, esito);
            if (idVincitore != null) {
                ps.setInt(7, idVincitore);
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[PartitaDAO] registraPartita: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Restituisce lo storico delle partite di un utente, dal suo punto di vista:
     * l'esito ("Vittoria"/"Sconfitta"/"Pareggio") e l'avversario sono calcolati
     * rispetto all'utente richiedente (UC-07).
     *
     * @param idUtente id dell'utente
     * @return lista di partite (piu' recenti per prime)
     */
    public List<Partita> findStoricoByUtente(int idUtente) {
        String sql =
                "SELECT p.id, p.id_avversario1, p.id_avversario2, p.data, p.durata, p.id_vincitore, " +
                "       u1.username AS avv1, u2.username AS avv2 " +
                "FROM partite p " +
                "JOIN utenti u1 ON u1.id = p.id_avversario1 " +
                "JOIN utenti u2 ON u2.id = p.id_avversario2 " +
                "WHERE p.id_avversario1 = ? OR p.id_avversario2 = ? " +
                "ORDER BY p.id DESC";

        List<Partita> storico = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtente);
            ps.setInt(2, idUtente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idAvv1 = rs.getInt("id_avversario1");
                    int idVincitore = rs.getInt("id_vincitore");
                    boolean senzaVincitore = rs.wasNull();

                    String avversario = (idUtente == idAvv1) ? rs.getString("avv2") : rs.getString("avv1");

                    String esito;
                    if (senzaVincitore) {
                        esito = "Pareggio";
                    } else if (idVincitore == idUtente) {
                        esito = "Vittoria";
                    } else {
                        esito = "Sconfitta";
                    }

                    LocalDate data = parseData(rs.getString("data"));
                    Partita partita = new Partita(rs.getInt("id"), data, rs.getInt("durata"), esito, avversario);
                    storico.add(partita);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PartitaDAO] findStoricoByUtente: " + e.getMessage());
        }
        return storico;
    }

    /**
     * Calcola le statistiche per ciascun giocatore (UC-11): numero di vittorie,
     * numero di partite disputate e tempo medio di risposta (media delle durate
     * delle partite vinte).
     *
     * @return una lista di righe nel formato
     *         {@code [username:String, vittorie:long, partite:long, tempoMedio:double]},
     *         ordinata per numero di vittorie decrescente
     */
    public List<Object[]> statisticheGiocatori() {
        String sql =
                "SELECT u.username AS username, " +
                "  (SELECT COUNT(*) FROM partite p WHERE p.id_avversario1 = u.id OR p.id_avversario2 = u.id) AS partite, " +
                "  (SELECT COUNT(*) FROM partite p WHERE p.id_vincitore = u.id) AS vittorie, " +
                "  (SELECT AVG(p.durata) FROM partite p WHERE p.id_vincitore = u.id) AS tempo_medio " +
                "FROM utenti u WHERE u.ruolo = 'GIOCATORE' " +
                "ORDER BY vittorie DESC, partite DESC, username ASC";

        List<Object[]> righe = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String username = rs.getString("username");
                long partite = rs.getLong("partite");
                long vittorie = rs.getLong("vittorie");
                double tempoMedio = rs.getDouble("tempo_medio"); // 0.0 se NULL
                righe.add(new Object[]{username, vittorie, partite, tempoMedio});
            }
        } catch (SQLException e) {
            System.err.println("[PartitaDAO] statisticheGiocatori: " + e.getMessage());
        }
        return righe;
    }

    private LocalDate parseData(String iso) {
        try {
            return iso != null ? LocalDate.parse(iso) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
