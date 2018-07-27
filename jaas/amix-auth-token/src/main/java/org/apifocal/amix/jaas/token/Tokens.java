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
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apifocal.amix.jaas.token.impl.TokensImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.nimbusds.jose.Header;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * 
 */
public final class Tokens {

    private static final Logger LOG = LoggerFactory.getLogger(Tokens.class);

    public static KeyPair readKeyPair(String key) throws Exception {
        return TokensImpl.readKeyPair(new InputStreamReader(new ByteArrayInputStream(key.getBytes(StandardCharsets.UTF_8))));
    }

    public static KeyPair readKeyPair(Path path) throws Exception {
        return TokensImpl.readKeyPair(Files.newBufferedReader(path));
    }

    public static void subject(final JWTClaimsSet.Builder builder, String value) {
        Optional.ofNullable(value).map(s -> builder.subject(s)).orElseThrow(IllegalArgumentException::new);
    }

    public static void issuer(final JWTClaimsSet.Builder builder, String value) {
        Optional.ofNullable(value).map(s -> builder.issuer(s)).orElseThrow(IllegalArgumentException::new);
    }

    public static String createToken(final JWTClaimsSet claims, String privkey) {
        SignedJWT signedJWT = null;
        try {
            KeyPair kp = Tokens.readKeyPair(privkey);
            if (!"RSA".equals(kp.getPublic().getAlgorithm())) {
                // TODO: LOG, complain...
                return null;
            }
            RSAPrivateKey rsaKey = kp.getPrivate() instanceof RSAPrivateKey ? (RSAPrivateKey)kp.getPrivate() : null;
            signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            signedJWT.sign(new RSASSASigner(rsaKey));
            return signedJWT.serialize();
        } catch (Exception e) {
            // LOG e.getLocalizedMessage()
        }
        return null;
    }

    public static Map<String, Object> processToken(TokenValidationContext context, String token) {
        // TODO: we can do better than hardocoding...
        
        try {
            SignedJWT jwso = SignedJWT.parse(token);
            validateSigned(jwso);
            validateHeader(jwso);

            JWTClaimsSet claims = jwso.getJWTClaimsSet();
            validateUser(context.getUser(), claims);
            
            String owner = Strings.nullToEmpty(claims.getSubject());
            validateSignature(context, jwso);
            return claims.getClaims();
        } catch (Exception e) {
            // ignore; exception should be logged already and will return null anyway...
        }
        
        return null;
    }

    public static String fingerprint(PublicKey key) {
        return "";
    }

    private static String tokenIssuer(JWTClaimsSet claims) {
        return Strings.nullToEmpty(claims.getIssuer());
    }

    // TODO: turn this into custom validator chain
    private static void validateSigned(SignedJWT token) throws Exception {
        if (JWSObject.State.SIGNED != token.getState()) {
            throw new IllegalArgumentException("Only signed tokens allowed");
        }
    }
    private static void validateHeader(SignedJWT token) throws Exception {
        Header header = token.getHeader();
        // TODO: enforce algorithms and signing key charactristics
    }
    private static void validateSignature(TokenValidationContext context, SignedJWT token) throws Exception {
        Set<String> keys = Settings.loadKeys(context.getKeysLocation());
        Optional<String> result = keys.stream()
            .filter(k -> {
                PublicKey pk = SshKeyCodec.parse(k);
                if (pk instanceof RSAPublicKey) {
                    // TODO: can get smarter about using Predicates to enforce key usage
                    try {
                        return token.verify(new RSASSAVerifier((RSAPublicKey)pk));
                    } catch (JOSEException e) {
                    }
                }
                return false;
            })
            .findFirst();
        if (!result.isPresent()) {
            throw new Exception("Invalid token signature");
        }
    }
    // TODO above: like below...
    private static TokenHandler checkSigned(SignedJWT token) throws Exception {
        return new TokenHandler() {
            public void validate(TokenValidationContext context, SignedJWT token) {
                if (JWSObject.State.SIGNED != token.getState()) {
                    throw new IllegalArgumentException("Only signed tokens allowed");
                }
            }
        };
    }

    private static void validateUser(String user, JWTClaimsSet claims) throws Exception {
        if (!user.equals(claims.getSubject())) {
            throw new IllegalArgumentException("Invalid token: wrong user");
        }
    }

    // utility; no instances
    private Tokens() {
    }

}
