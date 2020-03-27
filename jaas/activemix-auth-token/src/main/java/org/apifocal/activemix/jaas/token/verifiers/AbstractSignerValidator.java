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
package org.apifocal.activemix.jaas.token.verifiers;

import java.security.Key;
import java.util.List;

import org.apifocal.activemix.jaas.token.TokenValidationException;
import org.apifocal.activemix.jaas.token.TokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;

/**
 * Abstract Validator of token signer using a variety of JWKSource(s) information.
 */
public abstract class AbstractSignerValidator implements TokenValidator<SignedJWT, SecurityContext> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSignerValidator.class);
    protected JWKSource<SecurityContext> jwkSource;

    @Override
    public void validate(SignedJWT token, SecurityContext securityContext) throws TokenValidationException {
        JWSHeader header = token.getHeader();
        JWSAlgorithm algorithm = header.getAlgorithm();

        try {
            List<Key> keys = new JWSVerificationKeySelector<>(algorithm, getJwkSource())
                .selectJWSKeys(header, securityContext);

            DefaultJWSVerifierFactory verifierFactory = new DefaultJWSVerifierFactory();
            boolean success = false;
            for (Key key : keys) {
                try {
                    JWSVerifier jwsVerifier = verifierFactory.createJWSVerifier(header, key);
                    jwsVerifier.verify(header, token.getSigningInput(), token.getSignature());
                    LOG.debug("Signature verification successful with key {}", key.getFormat());
                    success = true;
                    break;
                } catch (JOSEException e) {
                    LOG.info("Invalid signature found", e);
                }
            }

            if (!success) {
                throw new TokenValidationException("Could not verify signature. No matching keys found.");
            }
        } catch (KeySourceException e) {
            throw new TokenValidationException("Invalid token signature", e);
        }
    }

    protected abstract JWKSource<SecurityContext> getJwkSource();
}
