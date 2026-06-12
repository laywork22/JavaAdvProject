package it.guesstheword.server.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gestisce la configurazione del server letta dal file {@code server.properties}.
 *
 * <p>Per soddisfare il vincolo dei percorsi relativi, il file viene cercato
 * prima sul filesystem (nella cartella di lavoro, eventualmente sotto
 * {@code properties/}) e, in mancanza, tra le risorse del classpath. Se nessuna
 * fonte e' disponibile vengono usati valori di default ragionevoli, in modo che
 * l'applicazione resti comunque avviabile.</p>
 *
 * <p>Chiavi gestite:</p>
 * <ul>
 * <li>{@code server.port} &ndash; porta di ascolto del {@code ServerSocket};</li>
 * <li>{@code db.url} &ndash; URL JDBC del database SQLite;</li>
 * <li>{@code admin.default.username} / {@code admin.default.password} &ndash;
 * credenziali dell'amministratore di default pre-popolato.</li>
 * </ul>
 */
public class ConfigManager {

    private static final String NOME_FILE = "server.properties";

    private final Properties properties = new Properties();

    /** Carica la configurazione cercando {@code server.properties} nei percorsi standard. */
    public ConfigManager() {
        this(NOME_FILE);
    }

    /**
     * Costruisce un gestore caricando la configurazione da un file specifico.
     * @param percorso percorso (relativo) del file di properties da caricare.
     */
    public ConfigManager(String percorso) {
        carica(percorso);
    }

    private void carica(String percorso) {
        // 1) file esterno relativo alla working directory
        File[] candidati = {
                new File(percorso),
                new File("properties" + File.separator + percorso)
        };
        for (File f : candidati) {
            if (f.isFile()) {
                try (InputStream in = new FileInputStream(f)) {
                    properties.load(in);
                    System.out.println("[ConfigManager] Configurazione caricata da " + f.getPath());
                    return;
                } catch (IOException e) {
                    System.err.println("[ConfigManager] Errore leggendo " + f.getPath() + ": " + e.getMessage());
                }
            }
        }
        // 2) risorsa nel classpath
        try (InputStream in = getClass().getResourceAsStream("/" + percorso)) {
            if (in != null) {
                properties.load(in);
                System.out.println("[ConfigManager] Configurazione caricata dal classpath (/" + percorso + ")");
                return;
            }
        } catch (IOException e) {
            System.err.println("[ConfigManager] Errore leggendo la risorsa /" + percorso + ": " + e.getMessage());
        }
        // 3) default
        System.out.println("[ConfigManager] Nessun file di configurazione trovato: uso i valori di default.");
    }

    /**
     * Recupera la porta di ascolto del server.
     * @return la porta su cui avviene la comunicazione (default 6767).
     */
    public int getPort() {
        String valore = properties.getProperty("server.port", "6767").trim();
        try {
            return Integer.parseInt(valore);
        } catch (NumberFormatException e) {
            System.err.println("[ConfigManager] Porta non valida ('" + valore + "'): uso il default 6767.");
            return 6767;
        }
    }

    /**
     * Recupera l'URL di connessione al database.
     * @return l'URL JDBC del database SQLite (default "jdbc:sqlite:database.db").
     */
    public String getDbUrl() {
        return properties.getProperty("db.url", "jdbc:sqlite:database.db").trim();
    }

    /**
     * Recupera lo username di default dell'amministratore.
     * @return lo username dell'admin (default "admin").
     */
    public String getAdminUsername() {
        return properties.getProperty("admin.default.username", "admin").trim();
    }

    /**
     * Recupera la password di default dell'amministratore.
     * @return la password dell'admin (default "admin").
     */
    public String getAdminPassword() {
        return properties.getProperty("admin.default.password", "admin").trim();
    }

    /**
     * Recupera il valore di una generica proprieta' della configurazione.
     * @param chiave la chiave della proprieta' da cercare nel file
     * @param def    il valore di default da restituire se la chiave non viene trovata
     * @return il valore associato alla chiave, oppure il valore di default specificato
     */
    public String get(String chiave, String def) {
        return properties.getProperty(chiave, def);
    }
}