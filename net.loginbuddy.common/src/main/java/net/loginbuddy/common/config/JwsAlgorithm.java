package net.loginbuddy.common.config;

import java.util.logging.Logger;

public enum JwsAlgorithm {

    RS256,
    ES256;

    private final static Logger LOGGER = Logger.getLogger(JwsAlgorithm.class.getName());

    public static JwsAlgorithm findMatchingAlg(String alg) {
        try {
            return valueOf(alg);
        } catch (Exception e) {
            LOGGER.warning("Unsupported alg was given (%s), falling back to RS256");
            return RS256;
        }
    }
}
