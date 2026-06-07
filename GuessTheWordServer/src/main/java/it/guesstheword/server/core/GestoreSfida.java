package it.guesstheword.server.core;

import it.guesstheword.common.network.Sfida;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prepara le sfide e valida le risposte (requisiti IF-6..IF-11, UC-12).
 *
 * <p>Il flusso di preparazione e':</p>
 * <ol>
 *   <li>selezione casuale di un documento analizzato;</li>
 *   <li>estrazione di un breve estratto testuale (una frase);</li>
 *   <li>scelta di una parola "significativa" da nascondere;</li>
 *   <li>generazione di uno shift casuale per il cifrario di Cesare;</li>
 *   <li>sostituzione della parola con la sua versione cifrata.</li>
 * </ol>
 *
 * <p>La soluzione viene conservata nel campo {@code transient} della
 * {@link Sfida}: resta quindi lato server e non viene inviata ai client.</p>
 */
public class GestoreSfida {

    /** Durata di default del conto alla rovescia, in secondi. */
    public static final int DURATA_DEFAULT_SECONDI = 60;

    /** Lunghezza minima di una parola candidata ad essere nascosta. */
    private static final int LUNGHEZZA_MINIMA_PAROLA = 4;

    /** Numero minimo di parole che deve avere una frase per essere usata come estratto. */
    private static final int MIN_PAROLE_FRASE = 5;

    /** Pattern di una parola candidata (sequenza di sole lettere, anche accentate). */
    private static final Pattern PAROLA_CANDIDATA =
            Pattern.compile("\\p{L}{" + LUNGHEZZA_MINIMA_PAROLA + ",}");

    private final AnalizzatoreDocumenti analizzatore;
    private final Random random = new Random();
    private final int durataSecondi;

    public GestoreSfida(AnalizzatoreDocumenti analizzatore) {
        this(analizzatore, DURATA_DEFAULT_SECONDI);
    }

    public GestoreSfida(AnalizzatoreDocumenti analizzatore, int durataSecondi) {
        this.analizzatore = analizzatore;
        this.durataSecondi = durataSecondi;
    }

    /**
     * Prepara una nuova sfida selezionando casualmente un documento analizzato.
     *
     * @return la {@link Sfida} pronta per essere inviata ai client (con
     *         soluzione e shift valorizzati lato server)
     * @throws IllegalStateException se non e' disponibile alcun documento
     *         analizzato dal quale estrarre un testo (UC-12, flusso alt. 1a)
     */
    public Sfida preparaSfida() {
        if (!analizzatore.haDocumentiAnalizzati()) {
            throw new IllegalStateException("Nessun documento analizzato disponibile per preparare la sfida.");
        }
        // qualche tentativo: alcuni documenti potrebbero non contenere frasi adatte
        for (int tentativo = 0; tentativo < 12; tentativo++) {
            DocumentoAnalizzato doc = analizzatore.selezionaCasuale();
            if (doc == null) {
                break;
            }
            Sfida sfida = costruisciSfida(doc.getTesto());
            if (sfida != null) {
                return sfida;
            }
        }
        throw new IllegalStateException("Impossibile estrarre un estratto valido dai documenti analizzati.");
    }

    /**
     * Tenta di costruire una sfida a partire dal testo di un documento.
     *
     * @return la sfida costruita, oppure {@code null} se il testo non contiene
     *         una frase adatta
     */
    private Sfida costruisciSfida(String testo) {
        List<String> frasi = spezzaInFrasi(testo);
        Collections.shuffle(frasi, random);

        for (String fraseGrezza : frasi) {
            String frase = fraseGrezza.trim().replaceAll("\\s+", " ");
            if (frase.split("\\s+").length < MIN_PAROLE_FRASE) {
                continue;
            }
            List<String> candidate = paroleCandidate(frase);
            if (candidate.isEmpty()) {
                continue;
            }
            String parola = candidate.get(random.nextInt(candidate.size()));
            int shift = 1 + random.nextInt(25); // 1..25, mai 0 (testo invariato)
            String cifrata = CifrarioCesare.cifra(parola, shift);
            String testoCifrato = sostituisciParola(frase, parola, cifrata);

            Sfida sfida = new Sfida(testoCifrato, durataSecondi, 1);
            sfida.setParolaOriginale(parola);
            sfida.setShift(shift);
            return sfida;
        }
        return null;
    }

    /**
     * Valida la risposta di un giocatore confrontandola (senza distinzione tra
     * maiuscole e minuscole) con la parola originale della sfida.
     *
     * @param sfida    sfida in corso (lato server, con soluzione valorizzata)
     * @param risposta parola proposta dal giocatore
     * @return {@code true} se la risposta e' corretta
     */
    public boolean validaRisposta(Sfida sfida, String risposta) {
        if (sfida == null || risposta == null || sfida.getParolaOriginale() == null) {
            return false;
        }
        return sfida.getParolaOriginale().equalsIgnoreCase(risposta.trim());
    }

    /* ----------------------- metodi di supporto ----------------------- */

    /** Suddivide un testo in frasi usando la punteggiatura forte come separatore. */
    private List<String> spezzaInFrasi(String testo) {
        String[] parti = testo.split("(?<=[.!?])\\s+");
        List<String> frasi = new ArrayList<>(parti.length);
        for (String p : parti) {
            String frase = p.trim();
            if (!frase.isEmpty()) {
                frasi.add(frase);
            }
        }
        return frasi;
    }

    /** Estrae le parole candidate (sole lettere, lunghezza minima) da una frase. */
    private List<String> paroleCandidate(String frase) {
        List<String> candidate = new ArrayList<>();
        Matcher m = PAROLA_CANDIDATA.matcher(frase);
        while (m.find()) {
            candidate.add(m.group());
        }
        return candidate;
    }

    /** Sostituisce l'occorrenza (parola intera) di {@code parola} con {@code sostituto}. */
    private String sostituisciParola(String frase, String parola, String sostituto) {
        Pattern p = Pattern.compile("\\b" + Pattern.quote(parola) + "\\b");
        Matcher m = p.matcher(frase);
        if (m.find()) {
            return new StringBuilder(frase)
                    .replace(m.start(), m.end(), sostituto)
                    .toString();
        }
        // fallback: non dovrebbe accadere poiche' la parola proviene dalla frase
        return frase.replace(parola, sostituto);
    }
}
