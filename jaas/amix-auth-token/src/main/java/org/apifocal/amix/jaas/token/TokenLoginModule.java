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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.activemq.jaas.GroupPrincipal;
import org.apache.activemq.jaas.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

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
public class TokenLoginModule implements LoginModule, TokenValidationContext {
    private static final Logger LOG = LoggerFactory.getLogger(TokenLoginModule.class);

    private static final String GROUPS_PROP_FILENAME = "org.apifocal.amix.jaas.properties.groups";
    private static final String USER_KEYS_DIRNAME = "org.apifocal.amix.jaas.properties.keys";

    private CallbackHandler callbackHandler;
    private Subject subject;
    private final Set<Principal> principals = Sets.newConcurrentHashSet();

    private String user;
    private Supplier<Map<String, Set<String>>> groups;
    private Path keys;
    private Map<String, Object> options;
    private boolean verbose;


    @SuppressWarnings("unchecked")
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.options = ImmutableMap.copyOf((Map<String, Object>)options);

        verbose = Settings.booleanOption(options.get("debug"), false);
        String groupsProperties = Strings.nullToEmpty((String)options.get(GROUPS_PROP_FILENAME));
        Path keys = Paths.get(Strings.nullToEmpty((String)options.get(USER_KEYS_DIRNAME)));

        try {
            groups = Settings.groups(Paths.get(Resources.getResource(groupsProperties).toURI()));
        } catch (Exception e) {
            throw new IllegalStateException(e.getLocalizedMessage());
        }
        if (!Files.exists(keys) || !Files.isDirectory(keys)) {
            throw new IllegalStateException("Invalid provisioning of user keys. Check documentation");
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
        
        Map<String, Object> claims = Tokens.processToken(this, new String(token));
        return claims != null;
    }

    public boolean commit() throws LoginException {
        boolean success = user != null;
        if (success) {
            principals.add(new UserPrincipal(user));

            Set<String> ug = groups.get().get(user);
            if (ug != null) {
                // TODO: throw? validate so we never get here?
                ug.forEach(g -> principals.add(new GroupPrincipal(g))); 
            }
            // TODO: add PartitionPrincipal and maybe IssuerPrincipal from ClaimSet
            subject.getPrincipals().addAll(principals);
        }
        user = null;
        return success;
    }

    public boolean abort() throws LoginException {
        user = null;
        return true;
    }

    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        user = null;
        // if (verbose)...LOG.debug("logout");
        return true;
    }

    // TokenValidationContext interface methods
    public String getUser() {
        return this.user;
    }

    public Map<String, Set<String>> getGroups() {
        return this.groups.get();
    }

    public Path getKeysLocation() {
        return this.keys;
    }

    public Map<String, Object> getOptions() {
        return this.options;
    }

    public boolean isVerbose() {
        return verbose;
    }

}
