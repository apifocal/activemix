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
package org.apifocal.amix.jaas.token.impl;

import java.io.Reader;
import java.security.KeyPair;
import java.security.Security;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;

/**
 * 
 */
public final class TokensImpl {

    private static final Logger LOG = LoggerFactory.getLogger(TokensImpl.class);

    public static KeyPair readKeyPair(final Reader reader) throws Exception {
        // TODO: not very clean package dependencies, could use some refactoring
        PEMParser pemParser = null;
        try {
            Security.addProvider(BouncyCastleProviderSingleton.getInstance());
            pemParser = new PEMParser(reader);
            PEMKeyPair pemKeyPair = (PEMKeyPair)pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            return converter.getKeyPair(pemKeyPair);
        } finally {
            if (pemParser != null) {
                pemParser.close();
            }
            Security.removeProvider("BC");
        }
    }

    // utility; no instances
    private TokensImpl() {
    }

}
