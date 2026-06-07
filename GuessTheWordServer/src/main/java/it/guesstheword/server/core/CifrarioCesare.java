package it.guesstheword.server.core;

/**
 * Implementazione del cifrario di Cesare.
 *
 * <p>Ogni lettera (sia minuscola sia maiuscola) viene traslata di un numero
 * fisso di posizioni ({@code shift}) all'interno dell'alfabeto, con
 * scorrimento circolare; i caratteri non alfabetici restano invariati. Lo
 * shift viene normalizzato modulo 26, quindi sono accettati anche valori
 * negativi o maggiori di 26.</p>
 */
public final class CifrarioCesare {

    private CifrarioCesare() {
        // classe di utilita': non istanziabile
    }

    /**
     * Cifra il testo applicando lo shift indicato.
     *
     * @param testo testo in chiaro (puo' essere {@code null})
     * @param shift numero di posizioni di traslazione
     * @return testo cifrato, oppure {@code null} se {@code testo} era {@code null}
     */
    public static String cifra(String testo, int shift) {
        if (testo == null) {
            return null;
        }
        int s = ((shift % 26) + 26) % 26;
        StringBuilder sb = new StringBuilder(testo.length());
        for (int i = 0; i < testo.length(); i++) {
            char c = testo.charAt(i);
            if (c >= 'a' && c <= 'z') {
                sb.append((char) ('a' + (c - 'a' + s) % 26));
            } else if (c >= 'A' && c <= 'Z') {
                sb.append((char) ('A' + (c - 'A' + s) % 26));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Decifra un testo cifrato con lo shift indicato.
     *
     * @param testo testo cifrato
     * @param shift shift usato in fase di cifratura
     * @return testo in chiaro
     */
    public static String decifra(String testo, int shift) {
        return cifra(testo, -shift);
    }
}
