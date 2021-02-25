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
import org.apifocal.activemix.jaas.commons.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * TODO: Doc
 * 
 */
public class EscalateLoginModule implements LoginModule {

    private static final Logger LOG = LoggerFactory.getLogger(EscalateLoginModule.class);

    private CallbackHandler callbackHandler;
    private Subject subject;
    private final Set<Principal> principals = Sets.newConcurrentHashSet();

    private Settings settings;
    private boolean verbose;


    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;

        settings = new Settings(options);

        verbose = settings.booleanOption("debug");
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
        char[] token = pcb.getPassword();

        // escalate identity check to authority service
        return true;
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
}
