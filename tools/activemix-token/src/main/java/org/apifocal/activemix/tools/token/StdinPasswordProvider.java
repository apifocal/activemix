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
package org.apifocal.activemix.tools.token;

import java.io.Console;

import org.apifocal.activemix.jaas.token.PasswordProvider;

import com.google.common.base.Strings;

public final class StdinPasswordProvider implements PasswordProvider {

    private final String key;
    
    public StdinPasswordProvider(String key) {
        this.key = Strings.nullToEmpty(key);
    }
    
    public String getPassword() {
        String password = "";
        try {
            Console console = System.console();
            // TODO: use the format version vs just concatenating strings?
            password = console != null ? new String(console.readPassword("Enter passphrase for " + key + ": ")) : password;
        } catch (Exception ex) {
            // LOG
        }
        return password;
    }

}
