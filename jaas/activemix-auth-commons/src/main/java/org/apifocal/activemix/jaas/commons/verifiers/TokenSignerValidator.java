/*
 * Copyright (c) 2017-2020 apifocal LLC. All rights reserved.
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
package org.apifocal.activemix.jaas.commons.verifiers;

import java.io.File;

import org.apifocal.activemix.commons.Settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * Validator of token signer information.
 */
public class TokenSignerValidator extends AbstractSignerValidator {

    private static final Logger LOG = LoggerFactory.getLogger(TokenSignerValidator.class);
    private final File keys;

    public TokenSignerValidator(Settings settings) {
        this.keys = settings.stringOption("keys")
            .map(File::new)
            .filter(File::exists)
            .orElseThrow(() -> new IllegalStateException("TokenSignerValidator requires 'keys' property pointing to authorized keys directory"));
    }

    protected JWKSource<SecurityContext> getJwkSource() {
        if (jwkSource == null) {
            jwkSource = keys.isDirectory() ? new DirectoryJWKSource<>(keys) : new FileJWKSource<>(keys);
            LOG.info("Using JWKSource from {}", keys.getAbsolutePath());
        }
        return jwkSource;
    }

}
