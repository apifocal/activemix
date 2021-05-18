/*
 * Copyright (c) 2020-2021 apifocal LLC. All rights reserved.
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
package org.apifocal.activemix.jaas.escalate;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apifocal.activemix.jaas.commons.IssuerPrincipal;
import org.apifocal.activemix.commons.Settings;
import org.apifocal.activemix.commons.SettingsBuilder;
import org.apifocal.activemix.jaas.commons.Tokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * TODO: Doc
 * 
 */
public class EscalatedLoginModule implements LoginModule {

    private static final Logger LOG = LoggerFactory.getLogger(EscalatedLoginModule.class);
    private static final String[] LOCAL_DOMAINS = new String[]{"local", "example"};

    public static final String SETTING_BROKER = "broker";
    public static final String SETTING_AUTHORITY_NAME = "authority.name";
    public static final String SETTING_AUTHORITY_LINK = "authority.link.class";

    private CallbackHandler callbackHandler;
    private Subject subject;
    private final Set<Principal> principals = Sets.newConcurrentHashSet();

    private Settings settings;
    private boolean verbose;
    private boolean acceptAnonymous = false;

    private String broker;
    private String authority;
    private AuthorityLink link;


    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;

        settings = new Settings(options);

        verbose = settings.booleanOption("debug");
        broker = settings.stringOption(SETTING_BROKER, "");
        authority = settings.stringOption(SETTING_AUTHORITY_NAME, "");
        String authorityLink = settings.stringOption(SETTING_AUTHORITY_LINK, "");

        LOG.debug("Creating AuthorityLink of type '{}'", authorityLink);
        link = SettingsBuilder.create(SettingsBuilder.load("", authorityLink, AuthorityLink.class, getClass().getClassLoader()).get(), settings);
        if (link == null) {
            throw new IllegalStateException("Failed to create AuthorityLink of type " + authorityLink);
        }
        LOG.debug("Initializing AuthorityLink to broker='{}', authority='{}'", broker, authority);
        link.initialize(broker, authority);
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

        String user = ncb.getName();
        char[] password = pcb.getPassword();

        return verifyCredential(user, new String(password));
    }

    public boolean commit() throws LoginException {
        principals.add(new IssuerPrincipal("alice"));
        subject.getPrincipals().addAll(principals);
        return true;
    }

    public boolean abort() throws LoginException {
        return true;
    }

    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        // if (verbose)...LOG.debug("logout");
        return true;
    }

    protected boolean verifyCredential(String user, String credential) {
    	// FIXME: avoid the need to generate proper tokens for now, just assume all credentials are this dummy token
    	//  that gets us over the 'no text passwords allowed' exception
    	credential = 
            "eyJhbGciOiJSUzI1NiJ9." +
            "eyJzdWIiOiJmb28iLCJpc3MiOiJiYXIifQ." +
            "WQl3XKlooF-wIYK3ibYyT5AueKN9TSulBLoIdyj90sXmU9boa5yUCVHrdRI5BgC1Ep0RHbAlxGO1-e_5Z-yY81Li-wvf0MIg6jbQgQOJ1IDrDcfLS8VvnHqI5bpk5BhFaRkIQsyCvz7zbKGLqzTuI3VFvjUT6CJwSGhWdt19aJei2FiIZ6iPasVBfdZyJNmCxcKAZKdLlG2GWmXMYomVjSkitxM1SsjWGtu68ANKkkkUjdOoU-Q7v9hLb9Pa9VMIZoAQV4l__lvA-1lD2d11ezXa0I7nnoGri193Lvg1gBUtw7zzxr3Gmy0vSyjN4hegwXqvyBSIWW9sESaPYVyY2PIgMiFxJRhylqERcKOcT8Y8E43DYYkX5SdOsmwoOmScMZH7qoZfkWtMFc2rV72JyyCbjy16U-rjVFU-7hW8x3aaNEfMiXpJWaT9fU7yQYWmUO7w9TvzpH2YW3zX3qR-b9_pZaUBQvppzJmqY-_JTSR375gI3rMNS6mPHMEkMDORE1CuN7A138tXOypV3JvB3lV6AQeYMMBgepefxPwakj8A5LDDFpsiYbBRun3MHRvh8oAlr6xKzhogtbiUYo2-RG8LSEcToNpdbPqwJHCV7BtGSnfCHzI3ZsdvC9-Q4W0UwAxUpNEsgRkd178sMLuF4Ir1XwGzH05VXYKBKY0r2uY";

        return Tokens.isJwtToken(credential)
            ? verifyJwtCredential(user, credential) 
            : verifyTextCredential(user, credential);
    }

    protected boolean verifyTextCredential(String user, String credential) {
        LOG.warn("Plain text passwords not accepted for user='{}'", user);
        return false;
    }

    protected boolean verifyJwtCredential(String user, String credential) {
        boolean local = authenticateLocally(user);
        return local ? localTokenVerification(user, credential) : escalateTokenVerification(user, credential);
    }

    protected boolean authenticateLocally(String user) {
    	// Local users use an email format model. Predefined 'domains' MUST be handled locally...
    	String[] parts = user.split("@");
    	if (parts.length == 2 && Arrays.stream(LOCAL_DOMAINS).anyMatch(parts[1]::equals)) {
            LOG.warn("Credential verification for user='{}' must be done locally.", user);
    		return true;
    	}
    	return false;
    }

    protected boolean localTokenVerification(String user, String credential) {
    	// FIXME: for now assume all local tokens are ok; defer to local token checks
        return true;
    }

    protected boolean escalateTokenVerification(String user, String credential) {
        return link.verify(user, credential);
    }

}
