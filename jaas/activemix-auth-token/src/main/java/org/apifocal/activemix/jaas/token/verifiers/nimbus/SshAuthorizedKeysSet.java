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
package org.apifocal.activemix.jaas.token.verifiers.nimbus;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EllipticCurve;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apifocal.activemix.jaas.token.verifiers.SshKeyCodec;

/**
 * A key set which is backed by ssh authorized keys file.
 */
class SshAuthorizedKeysSet extends JWKSet {

    SshAuthorizedKeysSet(File file) throws IOException {
        super(read(file));
    }

    public static List<JWK> createKeySet(File file, JWKSelector jwkSelector) throws KeySourceException {
        try {
            SshAuthorizedKeysSet fileJWKSet = new SshAuthorizedKeysSet(file);
            return jwkSelector.select(fileJWKSet);
        } catch (IOException e) {
            throw new KeySourceException("Could not read file " + file, e);
        }
    }

    private static List<JWK> read(File file) throws IOException {
        List<JWK> keys = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Optional.ofNullable(SshKeyCodec.parse(line))
                    .map(SshAuthorizedKeysSet::toNimbusKey)
                    .ifPresent(keys::add);
            }
        }

        return keys;
    }

    private static JWK toNimbusKey(PublicKey publicKey) {
        if (publicKey instanceof RSAPublicKey) {
            return new RSAKey.Builder((RSAPublicKey) publicKey).build();
        } else if (publicKey instanceof ECPublicKey) {
            ECPublicKey publicKey1 = (ECPublicKey) publicKey;
            EllipticCurve curve = publicKey1.getParams().getCurve();
            return new ECKey.Builder(new Curve(curve.toString()), publicKey1).build();
        }

        return null;
    }

}
