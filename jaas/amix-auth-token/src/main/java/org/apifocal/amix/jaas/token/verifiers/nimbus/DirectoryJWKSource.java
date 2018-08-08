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

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWK source backed by directory.
 *
 * @param <C> Additional context information.
 */
public class DirectoryJWKSource<C extends SecurityContext> implements JWKSource<C> {

    public static final String KEY_FILE_EXTENSION = ".keys";

    private final static FilenameFilter FILENAME_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(KEY_FILE_EXTENSION);
        }
    };

    private final File directory;

    public DirectoryJWKSource(File directory) {
        this.directory = directory;
    }

    @Override
    public List<JWK> get(JWKSelector jwkSelector, C context) throws KeySourceException {
        List<JWK> selectedKey = new ArrayList<>();

        if (context instanceof UserSecurityContext) {
            String user = ((UserSecurityContext) context).getUser();
            selectedKey.addAll(createKeySetAndFilter(jwkSelector, new File(directory, user + KEY_FILE_EXTENSION)));
        }

        File[] keyFiles = directory.listFiles(FILENAME_FILTER);

        if (keyFiles != null) {
            for (File file : keyFiles) {
                selectedKey.addAll(createKeySetAndFilter(jwkSelector, file));
            }
        }
        return selectedKey;
    }

    private List<JWK> createKeySetAndFilter(JWKSelector jwkSelector, File file) throws KeySourceException {
        try {
            SshAuthorizedKeysSet fileJWKSet = new SshAuthorizedKeysSet(file);
            return jwkSelector.select(fileJWKSet);
        } catch (IOException e) {
            throw new KeySourceException("Could not read file " + file, e);
        }
    }

}
