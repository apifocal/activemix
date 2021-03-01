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
import java.net.URL;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * TODO: Doc
 */
public class EscalateLoginModuleTest {
    private static final Logger LOG = LoggerFactory.getLogger(EscalateLoginModuleTest.class);
    
    static {
        URL resource = Resources.getResource("login.config");
        if (resource != null) {
            System.setProperty("java.security.auth.login.config", resource.getFile());
        }
    }

    @Test
    public void testLogin() throws Exception {
        String token = "token";

        LoginContext context = new LoginContext("EscalateLogin", new UserTokenHandler("alice", token));
        try {
            context.login();
        } catch(LoginException e) {
            fail("Unexpected error during login call " + e);
        } finally {
            // context.logout();
        }

        Subject subject = context.getSubject();
        assertFalse(subject.getPrincipals().isEmpty());
    }

    private static class UserTokenHandler implements CallbackHandler {

        private final String user;
        private final String token;

        public UserTokenHandler(final String user, final String token) {
            this.user = user;
            this.token = token;
        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    ((NameCallback) callbacks[i]).setName(user);
                } else if (callbacks[i] instanceof PasswordCallback) {
                    ((PasswordCallback) callbacks[i]).setPassword(token.toCharArray());
                } else {
                    throw new UnsupportedCallbackException(callbacks[i]);
                }
            }
        }
    }

}
