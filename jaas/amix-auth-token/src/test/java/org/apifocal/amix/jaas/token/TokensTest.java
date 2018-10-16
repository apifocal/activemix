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

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Date;

import org.apifocal.amix.jaas.token.verifiers.SshKeyCodec;
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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * TODO: Doc
 */
public class TokensTest {
    private static final Logger LOG = LoggerFactory.getLogger(TokensTest.class);

    @Test
    public void testGetTokenFromResource() throws Exception {
        String token = Tokens.fromResource("./tokens/sample.token");
        Assert.assertNotNull(token);
        Assert.assertNotEquals(0, token.length());
    }

    @Test
    public void testGetTokenFromFile() throws Exception {
        String token = Tokens.fromFile("./src/test/resources/tokens/sample.token");
        Assert.assertNotNull(token);
        Assert.assertNotEquals(0, token.length());
    }

    @Test
    public void testGenerateToken() throws Exception {
        String token = Tokens.fromResource("./tokens/sample.token");
        SignedJWT jwso = null;
        try {
            jwso = SignedJWT.parse(token);
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
        String pubkey = Resources.toString(Resources.getResource("./ssh/bob/id_dsa-bob.pub"), Charsets.UTF_8);
        PublicKey pk = SshKeyCodec.parse(pubkey);
        if (pk == null) {
            Assert.fail("what?");
        }

        String privkey = Resources.toString(Resources.getResource("./ssh/bob/id_dsa-bob"), Charsets.UTF_8);
        KeyPair keyPair = Keys.readKeyPair(privkey, null);

        Assert.assertNotNull(keyPair);
        Assert.assertNotNull(keyPair.getPrivate());
        Assert.assertNotNull(keyPair.getPublic());

        // TODO: next part needs some refactoring; different test maybe?
        LOG.info("TYPE: {}", keyPair.getPublic().getAlgorithm());

        RSAPublicKey rsakey = (keyPair.getPublic() instanceof RSAPublicKey) ? (RSAPublicKey)keyPair.getPublic() : null;
        if (rsakey != null) {
            Assert.assertNotNull(rsakey);
            LOG.info("SIZE: {}", rsakey.getModulus().bitLength());
        }
        
        String fingerprint = Keys.fingerprint(keyPair.getPublic(), Keys.defaultAlgorithm());
        LOG.info("FINGER PUBLIC: {} {}", fingerprint.length(), fingerprint);
    }
    
    @Test
    public void testSignToken() throws Exception {
        String privkey = Resources.toString(Resources.getResource("./ssh/bob/id_rsa-bob"), Charsets.UTF_8);
        KeyPair kp = Keys.readKeyPair(privkey, null);
        LOG.info("KP ALGO: {}", kp.getPublic().getAlgorithm());
        LOG.info("KP TYPE: {}", kp.getPrivate().getClass().getName());
        RSAPrivateKey privateKey = (RSAPrivateKey)kp.getPrivate();

        // TODO: more work is needed on defining the issuer
        JWTClaimsSet claimsSetOne = new JWTClaimsSet.Builder()
            .subject("user")
            // .audience("project")
            .issueTime(new Date())
            .expirationTime(Date.from(ZonedDateTime.now().plusMonths(3).toInstant()))
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

        String pubkey = Resources.toString(Resources.getResource("./ssh/bob/id_rsa-bob.pub"), Charsets.UTF_8);
        PublicKey publix = SshKeyCodec.parse(pubkey);

        JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey)publix);
        Assert.assertTrue(signedJWT.verify(verifier));
    }

    @Test
    public void testCreateToken() throws Exception {
        String user = "bob";
        String app = "hello-world";
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder();
        Tokens.subject(claims, user);
        Tokens.issuer(claims, user);

        String privkey = Resources.toString(Resources.getResource("./ssh/bob/id_rsa-bob"), Charsets.UTF_8);
        String token = Tokens.createToken(claims.build(), privkey, null);
        Assert.assertNotNull(token);
        LOG.info("TOKEN [{}]: {}", token.length(), token);
    }

    @Test
    public void testCreateTokenEncryptedKey() throws Exception {
        String user = "carol";
        String app = "hello-world";
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder();
        Tokens.subject(claims, user);
        Tokens.issuer(claims, user);

        String privkey = Resources.toString(Resources.getResource("./ssh/carol/id_rsa-carol"), Charsets.UTF_8);
        String token = Tokens.createToken(claims.build(), privkey, new InsecurePasswordProvider());
        Assert.assertNotNull(token);
        LOG.info("TOKEN [{}]: {}", token.length(), token);
    }

}
