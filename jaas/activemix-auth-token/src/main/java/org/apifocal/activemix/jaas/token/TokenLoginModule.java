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
package org.apifocal.activemix.jaas.token;

import com.google.common.collect.Sets;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.apifocal.activemix.jaas.commons.ClaimMapper;
import org.apifocal.activemix.commons.Settings;
import org.apifocal.activemix.commons.SettingsBuilder;
import org.apifocal.activemix.jaas.commons.TokenValidationException;
import org.apifocal.activemix.jaas.commons.TokenValidator;
import org.apifocal.activemix.jaas.commons.verifiers.TokenSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.security.Principal;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * TODO: Doc
 * 
 * 
 *
{
 "typ": "JWT",
 "alg": "HS256"
}
{"sub":"1234567890","name": "John Doe","iss": "tenant"}

eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaXNzIjoidGVuYW50IiwianRpIjoiNzJlOTY2MzEtMGVjNC00NjFiLWIyNTEtYWM1NTNjMjFkMTIzIiwiaWF0IjoxNTMxMjU2NDA5LCJleHAiOjE1MzEyNjAwMDl9.Y2enE5VN46XNGQYE5tVRaK0nElve8ri5NkTb_IhZPU0

 */
public class TokenLoginModule implements LoginModule {

    public static final String VERIFIERS_PREFIX = "verifiers";
    public static final String VERIFIERS_CLASSES = VERIFIERS_PREFIX + ".classes";
    public static final String VERIFIERS_PACKAGE = VERIFIERS_PREFIX + ".package";

    public static final String CLAIM_MAPPERS_PREFIX = "claimMappers";
    public static final String CLAIM_MAPPERS_PACKAGE = CLAIM_MAPPERS_PREFIX + ".package";
    public static final String CLAIM_MAPPERS_CLASSES = CLAIM_MAPPERS_PREFIX + ".classes";

    private static final Logger LOG = LoggerFactory.getLogger(TokenLoginModule.class);

    private CallbackHandler callbackHandler;
    private Subject subject;
    private final Set<Principal> principals = Sets.newConcurrentHashSet();

    private String user;
    private Settings settings;
    private boolean verbose;
    private boolean userAsTenant;
    @SuppressWarnings("rawtypes")
    protected List<TokenValidator> validators;
    protected List<ClaimMapper> claimMappers;
    private JWTClaimsSet claims;


    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;

        settings = new Settings(options);

        verbose = settings.booleanOption("debug");
        userAsTenant = settings.booleanOption("userAsTenant", false);

        String verifiersPackage = settings.stringOption(VERIFIERS_PACKAGE, "org.apifocal.amx.jaas.token.verifiers");
        Optional<String> verifiersClasses = settings.stringOption(VERIFIERS_CLASSES);

        String verifierNames = verifiersClasses.orElseThrow(SettingsBuilder.requiredPropertyMissing(VERIFIERS_CLASSES));
        this.validators = SettingsBuilder.createObjects(getClass().getClassLoader(), settings, verifiersPackage, verifierNames, TokenValidator.class, VERIFIERS_PREFIX);

        String mappersPackage = settings.stringOption(CLAIM_MAPPERS_PACKAGE, "org.apifocal.amx.jaas.token.mappers");
        Optional<String> mappersClasses = settings.stringOption(CLAIM_MAPPERS_CLASSES);

        String mapperNames = mappersClasses.orElseThrow(SettingsBuilder.requiredPropertyMissing(CLAIM_MAPPERS_CLASSES));
        this.claimMappers = SettingsBuilder.createObjects(getClass().getClassLoader(), settings, mappersPackage, mapperNames, ClaimMapper.class, CLAIM_MAPPERS_PREFIX);

        if (validators.isEmpty() || claimMappers.isEmpty()) {
            throw new IllegalStateException("Both validators and claim mappers must be set");
        }

        user = null;
    }

    public boolean login() throws LoginException {
        NameCallback ncb = new NameCallback("Username: "); 
        PasswordCallback pcb = new PasswordCallback("Password: ", false);
        Callback[] callbacks = {ncb, pcb};
        try {
            callbackHandler.handle(callbacks);
        } catch (IOException ioe) {
            throw new LoginException(ioe.getMessage());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException("Cannot obtain information from user: " + uce.getMessage());
        }

        user = ncb.getName();
        char[] token = pcb.getPassword();
        token = token != null ? token : new char[0];

        try {
            return (this.claims = processToken(new String(token))) != null;
        } catch (ParseException e) {
            if (verbose) {
                LOG.warn("Failed to process token", e);
            }
            throw new LoginException("Invalid token");
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private JWTClaimsSet processToken(String token) throws ParseException, LoginException {
        SignedJWT parsedToken = SignedJWT.parse(token);

        SecurityContext securityContext;
        // TODO: further refactoring; there is no need for 'userAsTenant', the token should have user == issuer
        securityContext = new TokenSecurityContext(userAsTenant ? user : parsedToken.getJWTClaimsSet().getIssuer());

        for (TokenValidator validator : validators) {
            try {
                validator.validate(parsedToken, securityContext);
            } catch (TokenValidationException e) {
                if (verbose) {
                    LOG.warn("Detected invalid token", e);
                }
                throw new LoginException("Token didn't pass " + validator.getClass().getName() + " validation");
            }
        }

        return parsedToken.getJWTClaimsSet();
    }

    public boolean commit() throws LoginException {
        boolean success = claims != null;
        if (success) {
            for (ClaimMapper mapper : claimMappers) {
                principals.addAll(mapper.map(claims));
            }

            subject.getPrincipals().addAll(principals);
            return true;
        }
        return false;
    }

    public boolean abort() throws LoginException {
        user = null;
        claims = null;
        return true;
    }

    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        user = null;
        claims = null;
        // if (verbose)...LOG.debug("logout");
        return true;
    }

}
