package com.nlove.iota.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class IotaSeedGenerator {
    static final String TRYTE_ALPHABET = "9ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final int SEED_LEN = 81;

    public static String generateSeed() {
        // our secure randomness source
        SecureRandom sr;
        try {
            sr = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            // this should not happen!
            e.printStackTrace();
            return null;
        }

        // the resulting seed
        StringBuilder sb = new StringBuilder(SEED_LEN);

        for (int i = 0; i < SEED_LEN; i++) {
            int n = sr.nextInt(27);
            char c = TRYTE_ALPHABET.charAt(n);

            sb.append(c);
        }

        return sb.toString();
    }
}
