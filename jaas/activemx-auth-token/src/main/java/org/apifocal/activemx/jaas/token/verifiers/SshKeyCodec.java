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
package org.apifocal.activemx.jaas.token.verifiers;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

import com.nimbusds.jose.util.Base64;

/**
 * 
 * TODO: important stuff; add javadoc and comments
 *          see: https://tools.ietf.org/html/rfc4253#section-6.6
 */
public class SshKeyCodec {
    
    public static PublicKey parse(String encodedKey) {
        Optional<String> keyType = Arrays.stream(encodedKey.split(" ")).filter(keyPart()).findFirst();
        Blob blob = keyType.map(keyString -> new Blob(new Base64(keyString).decode())).orElse(null);

        if (blob != null) {
            try {
                String type = blob.readString();
                return "ssh-rsa".equals(type) ? parseRsaKey(blob) : "ssh-dss".equals(type) ? parseDsaKey(blob) : null;
            } catch (GeneralSecurityException e) {
                // TODO: log?
            }
        }
        return null;
    }
    
    private static PublicKey parseRsaKey(Blob blob) throws GeneralSecurityException {
        BigInteger e = blob.readBigInteger();
        BigInteger m = blob.readBigInteger();
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(m, e));
    }

    private static PublicKey parseDsaKey(Blob blob) throws GeneralSecurityException {
        BigInteger p = blob.readBigInteger();
        BigInteger q = blob.readBigInteger();
        BigInteger g = blob.readBigInteger();
        BigInteger y = blob.readBigInteger();
        return KeyFactory.getInstance("DSA").generatePublic(new DSAPublicKeySpec(y, p, q, g));
    }

    private static Predicate<String> keyPart() {
        // both RSA and DSA keys encoding start with 'AAAA'
        return keyString -> keyString.startsWith("AAAA");
    }

    private static class Blob {
        private final byte[] b;
        private int o = 0;
        
        public Blob(byte[] data) {
            this(data, 0);
        }

        public Blob(byte[] data, int offset) {
            this.b = data;
            this.o = offset;
        }

        public int readInt() {
            return ((b[o++] & 0xFF) << 24) | ((b[o++] & 0xFF) << 16) | ((b[o++] & 0xFF) << 8) | (b[o++] & 0xFF);
        }

        public BigInteger readBigInteger() {
            int l = readInt();
            byte[] result = new byte[l];
            System.arraycopy(b, o, result, 0, l);
            o += l;
            return new BigInteger(result);
        }
        
        public String readString() {
            int l = readInt();
            String result = new String(b, o, l);
            o += l;
            return result;
        }
    }

}
