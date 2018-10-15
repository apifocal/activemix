/*
 * Copyright (c) 2017-2018 apifocal LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apifocal.amix.jaas.token;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apifocal.amix.jaas.token.impl.TokensImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 *
 */
public final class Tokens {

    private static final Logger LOG = LoggerFactory.getLogger(Tokens.class);

    public static void subject(final JWTClaimsSet.Builder builder, String value) {
        Optional.ofNullable(value).map(s -> builder.subject(s)).orElseThrow(IllegalArgumentException::new);
    }

    public static void issuer(final JWTClaimsSet.Builder builder, String value) {
        Optional.ofNullable(value).map(s -> builder.issuer(s)).orElseThrow(IllegalArgumentException::new);
    }

    public static void audience(final JWTClaimsSet.Builder builder, String value) {
        Optional.ofNullable(value).map(s -> builder.audience(s)).orElseThrow(IllegalArgumentException::new);
    }

    public static void audience(final JWTClaimsSet.Builder builder, List<String> value) {
        Optional.ofNullable(value).map(s -> builder.audience(s)).orElseThrow(IllegalArgumentException::new);
    }

    public static void issueTime(final JWTClaimsSet.Builder builder, Date value) {
        Optional.ofNullable(value).map(s -> builder.issueTime(s)).orElseThrow(IllegalArgumentException::new);
    }

    public static void expiration(final JWTClaimsSet.Builder builder, Date value) {
        Optional.ofNullable(value).map(s -> builder.expirationTime(s)).orElseThrow(IllegalArgumentException::new);
    }


    public static void claim(final JWTClaimsSet.Builder builder, String claim, String value) {
        Optional.ofNullable(value).map(s -> builder.claim(claim, s)).orElseThrow(IllegalArgumentException::new);
    }

    public static String createToken(final JWTClaimsSet claims, String privkey, PasswordProvider password) {
        SignedJWT signedJWT = null;
        try {
            KeyPair kp = Keys.readKeyPair(privkey, password);
            if (!"RSA".equals(kp.getPublic().getAlgorithm())) {
                // TODO: LOG, complain...
                return null;
            }
            RSAPrivateKey rsaKey = kp.getPrivate() instanceof RSAPrivateKey ? (RSAPrivateKey)kp.getPrivate() : null;
            signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            signedJWT.sign(new RSASSASigner(rsaKey));
            return signedJWT.serialize();
        } catch (Exception e) {
            LOG.warn(e.getLocalizedMessage());
        }
        return null;
    }

    // Convenient helpers for handling expiration time; functions convert milliseconds to the specified seconds/minutes/hours/days
    public static long seconds(int sec) {
        return 1000L * sec;
    }

    public static long minutes(int min) {
        return seconds(60) * min;
    }

    public static long hours(int hr) {
        return minutes(60) * hr;
    }

    public static long days(int d) {
        return hours(24) * d;
    }

    // utility; no instances
    private Tokens() {
    }

}
