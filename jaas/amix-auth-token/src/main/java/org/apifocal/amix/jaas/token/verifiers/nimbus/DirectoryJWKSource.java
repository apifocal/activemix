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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JWK source backed by directory.
 *
 * @param <C> Additional context information.
 */
public class DirectoryJWKSource<C extends SecurityContext> implements JWKSource<C> {

    public static final String KEY_FILE_EXTENSION = ".keys";

    private final Logger logger = LoggerFactory.getLogger(DirectoryJWKSource.class);
    private final File directory;

    public DirectoryJWKSource(File directory) {
        this.directory = directory;
    }

    @Override
    public List<JWK> get(JWKSelector jwkSelector, C context) {
        List<JWK> selectedKey = new ArrayList<>();

        if (context instanceof TenantSecurityContext) {
            ((TenantSecurityContext) context).getTenant()
                .map(user -> {
                    try {
                        return SshAuthorizedKeysSet.createKeySet(new File(directory, user + KEY_FILE_EXTENSION), jwkSelector);
                    } catch (KeySourceException e) {
                        logger.warn("Couldn't load keys - failing back to empty set.", e);
                        return Collections.<JWK>emptySet();
                    }
                }
            ).ifPresent(selectedKey::addAll);
        }
        return selectedKey;
    }

}
