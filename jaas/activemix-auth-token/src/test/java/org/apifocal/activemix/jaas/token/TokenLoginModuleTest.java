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

import org.apifocal.activemix.jaas.token.mappers.IssuerPrincipal;
import org.apifocal.activemix.jaas.token.mappers.SubjectPrincipal;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * TODO: Doc
 */
public class TokenLoginModuleTest {
    private static final Logger LOG = LoggerFactory.getLogger(TokenLoginModuleTest.class);
    
    static {
        URL resource = Resources.getResource("login.config");
        if (resource != null) {
            System.setProperty("java.security.auth.login.config", resource.getFile());
        }
    }

    @Test
    public void testLogin() throws Exception {
        // --user foo --issuer bar --app test
        String token = "eyJhbGciOiJSUzI1NiJ9." +
            "eyJzdWIiOiJmb28iLCJpc3MiOiJiYXIifQ." +
            "WQl3XKlooF-wIYK3ibYyT5AueKN9TSulBLoIdyj90sXmU9boa5yUCVHrdRI5BgC1Ep0RHbAlxGO1-e_5Z-yY81Li-wvf0MIg6jbQgQOJ1IDrDcfLS8VvnHqI5bpk5BhFaRkIQsyCvz7zbKGLqzTuI3VFvjUT6CJwSGhWdt19aJei2FiIZ6iPasVBfdZyJNmCxcKAZKdLlG2GWmXMYomVjSkitxM1SsjWGtu68ANKkkkUjdOoU-Q7v9hLb9Pa9VMIZoAQV4l__lvA-1lD2d11ezXa0I7nnoGri193Lvg1gBUtw7zzxr3Gmy0vSyjN4hegwXqvyBSIWW9sESaPYVyY2PIgMiFxJRhylqERcKOcT8Y8E43DYYkX5SdOsmwoOmScMZH7qoZfkWtMFc2rV72JyyCbjy16U-rjVFU-7hW8x3aaNEfMiXpJWaT9fU7yQYWmUO7w9TvzpH2YW3zX3qR-b9_pZaUBQvppzJmqY-_JTSR375gI3rMNS6mPHMEkMDORE1CuN7A138tXOypV3JvB3lV6AQeYMMBgepefxPwakj8A5LDDFpsiYbBRun3MHRvh8oAlr6xKzhogtbiUYo2-RG8LSEcToNpdbPqwJHCV7BtGSnfCHzI3ZsdvC9-Q4W0UwAxUpNEsgRkd178sMLuF4Ir1XwGzH05VXYKBKY0r2uY";

        LoginContext context = new LoginContext("TokenLogin", new UserTokenHandler("alice", token));
        try {
            context.login();
        } catch(LoginException e) {
            fail("Unexpected error during login call " + e);
        }

        Subject subject = context.getSubject();
        assertFalse(subject.getPrincipals().isEmpty());

        assertFalse(subject.getPrincipals(SubjectPrincipal.class).isEmpty());
        assertFalse(subject.getPrincipals(IssuerPrincipal.class).isEmpty());
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
