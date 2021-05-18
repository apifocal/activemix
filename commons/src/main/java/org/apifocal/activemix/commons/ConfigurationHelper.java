/*
 * Copyright 2017 apifocal LLC.
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
package org.apifocal.activemix.commons;


import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.io.Files;


/**
 * TODO: doc
 */
public final class ConfigurationHelper {

    public static final String ACTIVEMIX_CONFIG_DIR = "activemix";

    public static final String ACTIVEMIX_CONFIG_FILE = "authority.conf";

    public static final String ACTIVEMIX_LOCK_FILE = "ion.lock";

    public static final String ACTIVEMIX_BROKER_NAME = "notification.service";

    public static final String ACTIVEMIX_CONNECTION_URL = "connection.url";

    public static final String ACTIVEMIX_CONNECTION_USER = "connection.user";

    public static final String ACTIVEMIX_CONNECTION_PASS = "connection.pass";

    public static final String ACTIVEMIX_DEST_NOTIFY = "destination.notify";

    public static final String ACTIVEMIX_DEST_ACK = "destination.ack";

    public static final String ACTIVEMIX_DEST_CONTROL = "destination.control";

    public static final String USER_HOME = "user.home";

    public static final String DOT_SYMBOL = ".";

    public static final String ETC_DIR = "/etc";


    public static enum DESTINATION_ROLE {
        NOTIFY,
        CONTROL,
        ACK,
    }

    public static Path cwd() {
        return Paths.get(DOT_SYMBOL).toAbsolutePath().normalize();
    }

    public static Path etcConfig() {
        return dirOrNull(Paths.get(ETC_DIR).resolve(ACTIVEMIX_CONFIG_DIR));
    }

    public static Path homeConfig() {
        String userHomeDir = System.getProperty(USER_HOME);
        String configDir = DOT_SYMBOL + ACTIVEMIX_CONFIG_DIR;

        return dirOrNull(Paths.get(userHomeDir).resolve(configDir));
    }

    public static Path configDir() {
    	// Definition of the default configuration
        // directory. "~/.activemix" first, fallback to "/etc/activemix"
        return configDir(homeConfig(), etcConfig());
    }

    public static Path configDir(final Path path, final String name) {
    	// Alternative configuration directory using explicit path
        return configDir(resolve(path, name), configDir());
    }

    public static Path resolve(final Path dir, final String file) {
    	// convenient helper to avoid checking for null before resolve()
        if (dir != null) {
            return dir.resolve(file);
        }

        return null;
    }

    public static boolean checkConfig(final Path configDir) {
    	Path ionConfig = null;

    	if (configDir != null) {
    	    ionConfig = resolve(configDir, ACTIVEMIX_CONFIG_FILE);
        }

    	return ionConfig != null && Files.isFile().apply(ionConfig.toFile());
    }

    private static Path configDir(final Path expected, final Path fallback) {
        if (expected != null) {
            return expected;
        } else {
            return fallback;
        }
    }

    private static Path dirOrNull(final Path dir) {
        if (Files.isDirectory().apply(dir.toFile())) {
            return dir;
        }
        return null;
    }
}
