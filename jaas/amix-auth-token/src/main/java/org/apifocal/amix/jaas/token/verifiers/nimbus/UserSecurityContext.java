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
package org.apifocal.amix.jaas.token.verifiers.nimbus;

import com.nimbusds.jose.proc.SecurityContext;

import java.util.Objects;

/**
 * A security context which delivers a user context to token processing.
 */
public class UserSecurityContext implements SecurityContext {

    private final String user;

    public UserSecurityContext(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserSecurityContext)) return false;
        UserSecurityContext that = (UserSecurityContext) o;
        return Objects.equals(getUser(), that.getUser());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUser());
    }

    @Override
    public String toString() {
        return "UserSecurityContext(" + user + ")";
    }
}
