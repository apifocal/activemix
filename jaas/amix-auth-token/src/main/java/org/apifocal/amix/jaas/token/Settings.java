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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

/**
 * 
 */
public final class Settings {
    private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

    // TODO: decide if useful alias...
    public static interface OrgChart extends Supplier<Map<String, Set<String>>> {};

    public static Supplier<Map<String, Set<String>>> groups(Path path) throws IOException {
        return new StaticValue<>(invert(parse(loadProperties(path))));
    }

    public static Properties loadProperties(Path path) throws IOException {
        return loadProperties(Files.newBufferedReader(path));
    }

    public static Properties loadProperties(Reader reader) throws IOException {
        Properties p = new Properties();
        p.load(reader);
        return p;
    }

    public static Path keyPath(String location, String user) throws Exception {
        return Paths.get(Resources.getResource(location).toURI()).resolve(user + ".keys");
    }

    public static Set<String> loadKeys(Path path) throws IOException {
        return Files.lines(path).collect(Collectors.toSet());
    }

    public static Map<String, Set<String>> parse(Properties properties) {
        Map<String, Set<String>> result = new HashMap<>();
        properties.stringPropertyNames().forEach(k -> {
            String[] values = properties.getProperty(k).split(",");
            Arrays.stream(values).map(String::trim).toArray(unused -> values);
            result.put(k, ImmutableSet.copyOf(values));
        });
        return result;
    }
    
    public static Map<String, Set<String>> invert(Map<String, Set<String>> input) {
        final Map<String, Set<String>> result = new HashMap<>();
        input.forEach((k, v) -> {
            v.forEach(s -> { 
                Set<String> iv = result.get(s);
                if (iv == null) {
                    iv = new HashSet<>();
                    result.put(s, iv);
                }
                iv.add(k);
            });
        });
        return result;
    }

    public static boolean booleanOption(Object option, boolean def) {
        return option != null ? Boolean.parseBoolean(option.toString()) : def;
    }

    private interface Reference<T> extends Supplier<T> {}

    private static final class StaticValue<V> implements Reference<V> {
        private V value;
        
        public StaticValue(V v) {
            this.value = v;
        }

        public V get() {
            return value;
        }

        public int hashCode() {
            return System.identityHashCode(value); // compare by identity
        }
        public boolean equals(Object obj) {
            return obj == this || obj instanceof Supplier && this.value == ((Supplier<?>) obj).get();  // compare by identity
        }

    }

    // utility; no instances
    private Settings() {
    }

}
