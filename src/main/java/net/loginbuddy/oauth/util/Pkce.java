/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.oauth.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * This class supports PKCE (RFC 7636). It creates and validates code_verifiers, code_challenges. It does not support code_challenge_method=plain though!
 */
public class Pkce {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Pkce.class));

    public static final String CODE_CHALLENGE_METHOD_PLAIN = "plain";
    public static final String CODE_CHALLENGE_METHOD_S256 = "S256";

    /**
     * @param code_challenge
     * @param code_challenge_method
     * @param code_verifier
     * @return
     * @throws UnsupportedEncodingException
     */
    public static boolean validate(String code_challenge, String code_challenge_method, String code_verifier) {
        if (!CODE_CHALLENGE_METHOD_S256.equalsIgnoreCase(code_challenge_method)) {
            LOGGER.warning("Currently only 'S256' is supported as code_challenge_method");
            return false;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); // the only algorithm this fake provider supports
            byte[] encodedhash = digest.digest(code_verifier.getBytes(StandardCharsets.UTF_8));
            if (!code_challenge.equals(new String(Base64.getUrlEncoder().encode(encodedhash)).replace("=", ""))) {
                LOGGER.warning("The given code_verifier is invalid");
            } else return true;
        } catch (NoSuchAlgorithmException e) {
            // this should never ever happen!
            e.printStackTrace();
            LOGGER.severe("This should never happen, must be the java implementation!");
        }
        return false;
    }

    /**
     *
     * @param code_challenge_method
     * @return
     * @throws UnsupportedEncodingException
     */
    public static PkcePair create(String code_challenge_method) {
        if (!CODE_CHALLENGE_METHOD_S256.equalsIgnoreCase(code_challenge_method)) {
            LOGGER.warning("Currently only 'S256' is supported as code_challenge_method");
            return null;
        }
        try {
            String code_verifier = UUID.randomUUID().toString().replace("-", "");
            code_verifier = new String(Base64.getEncoder().encode(code_verifier.getBytes())).replace("=", "");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(code_verifier.getBytes(StandardCharsets.UTF_8));
            return new PkcePair(code_verifier, new String(Base64.getUrlEncoder().encode(encodedhash)).replace("=", ""));
        } catch (NoSuchAlgorithmException e) {
            // this should never ever happen!
            e.printStackTrace();
            LOGGER.severe("This should never happen, must be the java implementation!");
        }
        return null;
    }

    public static boolean verifyChallenge(String code_challenge) {
        return code_challenge != null && code_challenge.length() >= 43 && code_challenge.length() <= 128 && !code_challenge.contains("=");
    }
}
