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
package org.apifocal.activemx.jaas.token;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import org.apifocal.activemx.jaas.token.impl.TokensImpl;
import org.bouncycastle.util.encoders.Hex;


public final class Keys {
    private static final String MD5_ALGO = "MD5";
    private static final String DEFAULT_ALGO = "SHA-256";

    //First item in each array refers to digest algorithm, while second item refers to digest name
    private static final String[][] DIGEST_ALGO = {
        {"MD5", "MD5"},
        {"SHA-1", "SHA1"},
        {"SHA-256", "SHA256"},
        {"SHA-512", "SHA512"},
    };

    public static KeyPair readKeyPair(String key, PasswordProvider password) throws Exception {
        return TokensImpl.readKeyPair(new InputStreamReader(new ByteArrayInputStream(key.getBytes(StandardCharsets.UTF_8))), password);
    }

    public static KeyPair readKeyPair(Path path, PasswordProvider password) throws Exception {
        return TokensImpl.readKeyPair(Files.newBufferedReader(path), password);
    }

    /**
     * @return {@link String} representing the default algorithm (Ex: "SHA-256")
     */
    public static String defaultAlgorithm() {
        return DEFAULT_ALGO;
    }

    /**
     * Builds a fingerprint from a key and algorithm
     * @param key - the key for which a fingerprint should be generated
     * @param algorithm - represents the algorithm used
     * @return a {@link String} with digest key appended to digest name
     * @throws NoSuchAlgorithmException - if the algorithm provided is invalid
     */
    public static String fingerprint(PublicKey key, String algorithm) throws NoSuchAlgorithmException {
        String digest = Hex.toHexString(MessageDigest.getInstance(algorithm).digest(key.getEncoded()));
        StringBuilder fingerprint = new StringBuilder();
        fingerprint.append(digestAlgorithmToName(algorithm)).append(":")
            .append(MD5_ALGO.equals(algorithm) ? md5ToText(digest) : digest);
        return fingerprint.toString();
    }

    /**
     * Gets the digest name from the entire digest fingerprint
     * @param digest - {@link String} representing the entire digest fingerprint
     * @return a {@link String} representing the name of the digest (Ex: "SHA256")
     */
    public static String getDigestName(String digest) {
        String[] parts = digest != null ? digest.split(":") : null;
        return parts != null && parts.length > 1 ? parts[0] : null;
    }

    /**
     * Extracts the digest key from the entire  digest fingerprint
     * @param digest - {@link String} representing the entire digest fingerprint
     * @return a {@link String} representing the digest key
     */
    public static String getDigestText(String digest) {
        String[] parts = digest != null ? digest.split(":") : null;
        return parts != null && parts.length > 1 ? digest.substring(parts[0].length() + 1) : null;
    }

    /**
     * Gets the digest algorithm (Ex: "SHA-256") from the digest name (Ex: "SHA256")
     * @param digestName - name of the digest
     * @return a {@link String} representing the digest algorithm (Ex: "SHA-256")
     */
    public static String digestAlgorithmFromName(String digestName) {
        for (String[] p: DIGEST_ALGO) {
            if (p[1].equals(digestName)) return p[0];
        }
        return null;
    }

    /**
     * Gets the digest name (Ex: "SHA256") from the digest algorithm (Ex: "SHA-256")
     * @param digestAlgo - algorithm of the digest
     * @return a {@link String} representing the digest name (Ex: "SHA256")
     */
    public static String digestAlgorithmToName(String digestAlgo) {
        for (String[] p: DIGEST_ALGO) {
            if (p[0].equals(digestAlgo)) return p[1];
        }
        return null;
    }

    /**
     * Helper method for {@link #fingerprint(PublicKey, String)} <br>
     * Appends colon (":") character every two characters to build the text representation of an md5 digest
     */
    private static String md5ToText(String digest) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length(); i++) {
            if (i > 0 && i % 2 == 0) sb.append(":");
            sb.append(digest.charAt(i));
        }
        return sb.toString();
    }

}
