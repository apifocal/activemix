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
package org.apifocal.activemix.jaas.commons;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

import org.apifocal.activemix.commons.Settings;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.Resources;

/**
 * TODO: Doc
 */
public class SettingsTest {
    
    private static final String SAMPLE_GROUPS = "# Sample comment\n"
            + "admin=bob,alice\n"
            + "\n"
            + "# Users\n"
            + "users=charlie,frank\n"
            + "space= eve, mallory , sybil \n";

    @Test
    public void testLoadProperties() throws Exception {
        Properties p = fromSample(SAMPLE_GROUPS);
        Assert.assertNotNull(p);
        Assert.assertNotNull(p.getProperty("admin"));
    }

    @Test
    public void testParseProperties() throws Exception {
        Properties p = fromSample(SAMPLE_GROUPS);
        Assert.assertNotNull(p);

        Map<String, Set<String>> groups = Settings.parse(p);
        Assert.assertNotNull(groups);
        Assert.assertNotNull(groups.get("admin"));
        Assert.assertNull(groups.get("operator"));

        Set<String> admins = groups.get("admin");
        Assert.assertNotNull(admins);
        Assert.assertEquals(2, admins.size());
        Assert.assertTrue(admins.contains("alice"));
        Assert.assertTrue(admins.contains("bob"));
        Assert.assertFalse(admins.contains("charlie"));
    }

    @Test
    public void testParseIgnoreSpaces() throws Exception {
        Properties p = fromSample(SAMPLE_GROUPS);
        Assert.assertNotNull(p);

        Map<String, Set<String>> groups = Settings.parse(p);
        Assert.assertNotNull(groups);
        Assert.assertNotNull(groups.get("space"));

        Set<String> space = groups.get("space");
        Assert.assertNotNull(space);
        Assert.assertEquals(3, space.size());
        Assert.assertTrue(space.contains("eve"));
        Assert.assertTrue(space.contains("mallory"));
        Assert.assertTrue(space.contains("sybil"));
        Assert.assertFalse(space.contains("frank"));
    }

    @Test
    public void testInvertedProperties() throws Exception {
        Properties p = fromSample(SAMPLE_GROUPS);
        Assert.assertNotNull(p);

        Map<String, Set<String>> users = Settings.invert(Settings.parse(p));
        Assert.assertNotNull(users);
        Assert.assertNotNull(users.get("bob"));
        Assert.assertNull(users.get("judy"));
    }

    @Test
    public void testLoadGroups() throws Exception {
        Supplier<Map<String, Set<String>>> groups = Settings.groups(Paths.get(Resources.getResource("groups.properties").toURI()));
        Assert.assertNotNull(groups);
        Assert.assertNotNull(groups.get());
        Assert.assertEquals(2, groups.get().size());
        Assert.assertNotNull(groups.get().get("bob"));
        Assert.assertNull(groups.get().get("judy"));
    }

    @Test
    public void testKeys() throws Exception {
        Set<String> keys = Settings.loadKeys(Settings.keyPath("keys", "bob"));
        Assert.assertNotNull(keys);
        Assert.assertEquals(2, keys.size());
    }

    private Properties fromSample(String text ) throws IOException {
        return Settings.loadProperties(new StringReader(text));
    }

}
