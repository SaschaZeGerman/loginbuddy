/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.oauth.util;

public class PkcePair {

    private String verifier, challenge;

    public PkcePair(String verifier, String challenge) {
        this.verifier = verifier;
        this.challenge = challenge;
    }

    public String getVerifier() {
        return verifier;
    }

    public String getChallenge() {
        return challenge;
    }
}