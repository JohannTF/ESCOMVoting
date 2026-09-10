package com.github.escom.escomvoting.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generates random temporary passwords for newly created accounts.
 *
 * Guarantees at least one lowercase, one uppercase, one digit and one symbol
 * so the result satisfies common complexity policies. Ambiguous characters
 * (0/O, 1/l/I) are excluded so the password is easy to read and re-type from
 * a welcome email.
 */
public final class PasswordGenerator {

    private static final Random RNG = new Random();

    private static final String LOWER   = "abcdefghijkmnpqrstuvwxyz";
    private static final String UPPER   = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS  = "23456789";
    private static final String SYMBOLS = "!@#$%*?";
    private static final String ALL     = LOWER + UPPER + DIGITS + SYMBOLS;

    private static final int DEFAULT_LENGTH = 12;

    private PasswordGenerator() {}

    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public static String generate(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("Password length must be at least 4");
        }

        List<Character> chars = new ArrayList<>(length);
        // Guarantee one of each required category
        chars.add(randomChar(LOWER));
        chars.add(randomChar(UPPER));
        chars.add(randomChar(DIGITS));
        chars.add(randomChar(SYMBOLS));
        // Fill the rest from the full alphabet
        for (int i = chars.size(); i < length; i++) {
            chars.add(randomChar(ALL));
        }
        // Shuffle so the guaranteed characters are not always in the same positions
        Collections.shuffle(chars, RNG);

        StringBuilder sb = new StringBuilder(length);
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static char randomChar(String alphabet) {
        return alphabet.charAt(RNG.nextInt(alphabet.length()));
    }
}
