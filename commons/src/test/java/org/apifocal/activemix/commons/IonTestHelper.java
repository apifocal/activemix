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

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apifocal.activemix.commons.ConfigurationHelper.ACTIVEMIX_CONFIG_DIR;

public final class IonTestHelper {
    private static final Logger LOG = LoggerFactory.getLogger(IonTestHelper.class);

    public static final String SUBSCRIBER_DIR = "target/test-classes";
    public static final String CONF_DIR = ACTIVEMIX_CONFIG_DIR;

    public static Path twd() {
        // use this test-working-directory instead of cwd() for unit tests
        return ConfigurationHelper.cwd().resolve(SUBSCRIBER_DIR);
    }

    public static void close(Object o) {
        if (o != null && (o instanceof Closeable)) {
            try {
                ((Closeable) o).close();
            } catch (IOException e) {
                LOG.warn("Could not close instance of type {}: {}", o.getClass().getName(), e.getMessage());
            }
        }
    }

    public static void recursiveDelete(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }

        final List<Path> pathsToDelete = Files.walk(root)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        for (Path path : pathsToDelete) {
            Files.deleteIfExists(path);
        }
    }
}
