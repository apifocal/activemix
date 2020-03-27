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

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Settings object which allows to read configuration properties in more handy way.
 */
public final class Settings {
    private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

    private final Map<String, ?> config;
    private final String prefix;
    private final Predicate<Map.Entry<String, ?>> predicate;

    public Settings(Map<String, ?> config) {
        this(config, "", (entry) -> true);
    }

    Settings(Map<String, ?> config, String prefix, Predicate<Map.Entry<String, ?>> predicate) {
        this.config = config;
        this.prefix = prefix;
        this.predicate = predicate;
    }

    public Settings subset(String ... path) {
        return subset(Arrays.stream(path).reduce((s1, s2) -> s1 + "." + s2).orElse(""));
    }

    public Settings subset(String name) {
        return subset(name, keyPredicate(name));
    }

    private Settings subset(String prefix, Predicate<Map.Entry<String, ?>> predicate) {
        return new Settings(config, prefix, predicate);
    }

    private Predicate<Map.Entry<String, ?>> keyPredicate(String name) {
        return new Predicate<Map.Entry<String, ?>>() {
            @Override
            public boolean test(Map.Entry<String, ?> entry) {
                return entry.getKey().startsWith(name);
            }
        };
    }

    public String stringOption(String key, String fallback) {
        return stringOption(key).orElse(fallback);
    }

    public Optional<String> stringOption(String key) {
        return getObject(key)
            .map(Object::toString);
    }

    public boolean booleanOption(String key) {
        return booleanOption(key, false);
    }

    public boolean booleanOption(String key, boolean fallback) {
        Optional<?> value = getObject(key);

        return value.map(Object::toString)
            .map(Boolean::parseBoolean)
            .orElse(fallback);
    }

    private Optional<?> getObject(String key) {
        String keyPrefix = prefix.isEmpty() ? "" : prefix + ".";
        return config.entrySet().stream().filter(predicate.and(keyPredicate(keyPrefix + key)))
            .map(Map.Entry::getValue)
            .findFirst();
    }


    // utilities

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

}
