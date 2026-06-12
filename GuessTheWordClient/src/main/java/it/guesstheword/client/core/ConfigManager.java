package it.guesstheword.client.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gestisce la configurazione del client letta dal file {@code client.properties}
 * (IP e porta del server).
 *
 * <p>Il file viene cercato prima sul filesystem (cartella di lavoro, anche sotto
 * {@code properties/}) e, in mancanza, tra le risorse del classpath; in assenza
 * di entrambi vengono usati valori di default ({@code 127.0.0.1:6767}), cosi'
 * che il client resti comunque avviabile.</p>
 */
public class ConfigManager {

    private static final String NOME_FILE = "client.properties";

    private final Properties properties = new Properties();

    /**
     * Costruttore di default che carica la configurazione cercando {@code client.properties} 
     * nei percorsi standard.
     */
    public ConfigManager() {
        this(NOME_FILE);
    }

    /**
     * Costruisce un gestore caricando la configurazione da un file specifico.
     *
     * @param percorso percorso (relativo) del file di properties da caricare.
     */
    public ConfigManager(String percorso) {
        carica(percorso);
    }

    private void carica(String percorso) {
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
        try (InputStream in = getClass().getResourceAsStream("/" + percorso)) {
            if (in != null) {
                properties.load(in);
                System.out.println("[ConfigManager] Configurazione caricata dal classpath (/" + percorso + ")");
                return;
            }
        } catch (IOException e) {
            System.err.println("[ConfigManager] Errore leggendo la risorsa /" + percorso + ": " + e.getMessage());
        }
        System.out.println("[ConfigManager] Nessun file di configurazione trovato: uso i valori di default.");
    }

    /**
     * Recupera l'indirizzo IP del server a cui connettersi.
     *
     * @return l'indirizzo IP del server (default "127.0.0.1").
     */
    public String getServerIp() {
        return properties.getProperty("server.ip", "127.0.0.1").trim();
    }

    /**
     * Recupera la porta su cui il server e' in ascolto.
     *
     * @return la porta di comunicazione del server (default 6767).
     */
    public int getServerPort() {
        String valore = properties.getProperty("server.port", "6767").trim();
        try {
            return Integer.parseInt(valore);
        } catch (NumberFormatException e) {
            System.err.println("[ConfigManager] Porta non valida ('" + valore + "'): uso il default 6767.");
            return 6767;
        }
    }
}