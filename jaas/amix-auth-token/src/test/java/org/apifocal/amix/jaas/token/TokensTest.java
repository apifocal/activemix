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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.function.Predicate;

import javax.xml.bind.DatatypeConverter;

import org.apifocal.amix.jaas.token.SshKeyCodec;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;

/**
 * TODO: Doc
 */
public class TokensTest {
    private static final Logger LOG = LoggerFactory.getLogger(TokensTest.class);
    
    
    @Test
    public void testGenerateToken() throws Exception {
         String s = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaXNzIjoidGVuYW50IiwianRpIjoiNzJlOTY2MzEtMGVjNC00NjFiLWIyNTEtYWM1NTNjMjFkMTIzIiwiaWF0IjoxNTMxMjU2NDA5LCJleHAiOjE1MzEyNjAwMDl9.Y2enE5VN46XNGQYE5tVRaK0nElve8ri5NkTb_IhZPU0";
        SignedJWT jwso = null;
        try {
            jwso = SignedJWT.parse(s);
            LOG.info("Token: {}", jwso.getJWTClaimsSet().toString());
            LOG.info("Token: {}", jwso.getJWTClaimsSet().getClaim("iss").toString());
            LOG.info("Token: {}", jwso.getJWTClaimsSet().getClaim("sub").toString());
        }
        catch (ParseException e) {
            LOG.error("Couldn't parse JWS object: {}", e.getMessage());
            return;
        }        
    }

    @Test
    public void testReadSshKey() throws Exception {
        String pubkey = Resources.toString(Resources.getResource("./ssh/id_dsa-bob.pub"), Charsets.UTF_8);
        PublicKey pk = SshKeyCodec.parse(pubkey);
        if (pk == null) {
            Assert.fail("what?");
        }

        String privkey = Resources.toString(Resources.getResource("./ssh/id_dsa-bob"), Charsets.UTF_8);
        KeyPair keyPair = getKeyPair(privkey);

        Assert.assertNotNull(keyPair);
        Assert.assertNotNull(keyPair.getPrivate());
        Assert.assertNotNull(keyPair.getPublic());

        LOG.info("TYPE: {}", keyPair.getPublic().getAlgorithm());

        RSAPublicKey rsakey = (keyPair.getPublic() instanceof RSAPublicKey) ? (RSAPublicKey)keyPair.getPublic() : null;
        if (rsakey != null) {
            Assert.assertNotNull(rsakey);
            LOG.info("SIZE: {}", rsakey.getModulus().bitLength());
        }
        
        String fingerprint = Hex.toHexString(MessageDigest.getInstance("SHA-1").digest(keyPair.getPublic().getEncoded()));
        LOG.info("FINGER: {} {}", fingerprint.length(), fingerprint);
    }
    

    @Test
    public void testSignToken() throws Exception {
        String privkey = Resources.toString(Resources.getResource("./ssh/id_rsa-bob"), Charsets.UTF_8);
        KeyPair kp = getKeyPair(privkey);
        LOG.info("KP ALGO: {}", kp.getPublic().getAlgorithm());
        LOG.info("KP TYPE: {}", kp.getPrivate().getClass().getName());
        RSAPrivateKey privateKey = (RSAPrivateKey)kp.getPrivate();

        // TODO: more work is needed on defining the issuer
        JWTClaimsSet claimsSetOne = new JWTClaimsSet.Builder()
            .subject("user")
            // .audience("project")
            .issueTime(new Date())
            .expirationTime(Date.from(ZonedDateTime.now().plusMonths(1).toInstant()))
            .issuer("urn:bob")
            .build();
        LOG.info("CLAIMS: {}", claimsSetOne.toJSONObject());

        JWSSigner signer = new RSASSASigner(privateKey);
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSetOne);

        Assert.assertEquals(JWSObject.State.UNSIGNED, signedJWT.getState());
        signedJWT.sign(signer);
        Assert.assertEquals(JWSObject.State.SIGNED, signedJWT.getState());

        String orderOne = signedJWT.serialize();
        LOG.info("TOKEN: {}", orderOne);
        LOG.info("TOKEN size: {}", orderOne.length());

        String pubkey = Resources.toString(Resources.getResource("./ssh/id_rsa-bob.pub"), Charsets.UTF_8);
        PublicKey publix = SshKeyCodec.parse(pubkey);

        JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey)publix);
        Assert.assertTrue(signedJWT.verify(verifier));

    }

    private KeyPair getKeyPair(String path) throws Exception {
        Security.addProvider(BouncyCastleProviderSingleton.getInstance());
        PEMParser pemParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(path.getBytes(StandardCharsets.UTF_8))));
        PEMKeyPair pemKeyPair = (PEMKeyPair)pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        KeyPair keyPair = converter.getKeyPair(pemKeyPair);
        pemParser.close();
        Security.removeProvider("BC");

        return keyPair;
    }
}
