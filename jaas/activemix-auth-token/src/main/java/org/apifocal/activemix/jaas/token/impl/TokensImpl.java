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
package org.apifocal.activemix.jaas.token.impl;

import java.io.IOException;
import java.io.Reader;
import java.security.KeyPair;
import java.security.Security;
import java.util.Objects;

import org.apifocal.activemix.jaas.token.PasswordProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;

/**
 * 
 */
public final class TokensImpl {

    private static final Logger LOG = LoggerFactory.getLogger(TokensImpl.class);

    public static KeyPair readKeyPair(final Reader reader, PasswordProvider password) throws Exception {
        // TODO: not very clean package dependencies, could use some refactoring
        PEMParser pemParser = null;
        try {
            Security.addProvider(BouncyCastleProviderSingleton.getInstance());
            pemParser = new PEMParser(reader);
            Object pem = pemParser.readObject();
            PEMKeyPair kp = (pem instanceof PEMEncryptedKeyPair) 
                ? decryptKey((PEMEncryptedKeyPair)pem, password) : (pem instanceof PEMKeyPair) ? (PEMKeyPair)pem : null;
            return new JcaPEMKeyConverter().getKeyPair(kp);
        } finally {
            if (pemParser != null) {
                pemParser.close();
            }
            // TODO: not really a good idea to blindly remove the BC provider
            Security.removeProvider("BC");
        }
    }

    private static PEMKeyPair decryptKey(PEMEncryptedKeyPair encryptedKeyPair, PasswordProvider passwordProvider) throws IOException {
        Objects.requireNonNull(passwordProvider, "Support for encrypted keys enabled without supplying PasswordProvider.");
        PEMDecryptorProvider decryptor = new JcePEMDecryptorProviderBuilder().build(passwordProvider.getPassword().toCharArray());
        return encryptedKeyPair.decryptKeyPair(decryptor);
    }

    // utility; no instances
    private TokensImpl() {
    }

}
