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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.junit.Assert;
import org.junit.Test;


public class ConfigurationTest {

    private Path conf() {
        return ConfigurationHelper.configDir(IonTestHelper.twd(), IonTestHelper.CONF_DIR);
    }

    @Test
    public void testConfigInCwd() throws Exception {
        Path confDir = conf();
        Assert.assertNotNull(confDir);
        Assert.assertTrue(Files.exists(confDir) && Files.isDirectory(confDir));
    }

    @Test
    public void testNoConfig() throws Exception {
        Path nowhere = ConfigurationHelper.cwd().resolve("target/nowhere");
        Path confDir = ConfigurationHelper.configDir(nowhere, ConfigurationHelper.ACTIVEMIX_CONFIG_DIR);
        Assert.assertFalse(Files.exists(confDir) && Files.isDirectory(confDir));
    }

    @Test
    public void testReadConfig() throws Exception {
        Path confDir = conf();
        File conf = confDir.resolve(ConfigurationHelper.ACTIVEMIX_CONFIG_FILE).toFile();

        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        PropertiesConfiguration config = new PropertiesConfiguration();
        layout.load(config, new InputStreamReader(new FileInputStream(conf)));

        String user = (String)config.getProperty(ConfigurationHelper.ACTIVEMIX_CONNECTION_USER);
        Assert.assertNotNull(user);
        Assert.assertEquals("user", user);

        String pass = (String)config.getProperty(ConfigurationHelper.ACTIVEMIX_CONNECTION_PASS);
        Assert.assertNotNull(pass);
        Assert.assertEquals("eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJmb28iLCJpc3MiOiJiYXIifQ.WQl3XKlooF-wIYK3ibYyT5AueKN9TSulBLoIdyj90sXmU9boa5yUCVHrdRI5BgC1Ep0RHbAlxGO1-e_5Z-yY81Li-wvf0MIg6jbQgQOJ1IDrDcfLS8VvnHqI5bpk5BhFaRkIQsyCvz7zbKGLqzTuI3VFvjUT6CJwSGhWdt19aJei2FiIZ6iPasVBfdZyJNmCxcKAZKdLlG2GWmXMYomVjSkitxM1SsjWGtu68ANKkkkUjdOoU-Q7v9hLb9Pa9VMIZoAQV4l__lvA-1lD2d11ezXa0I7nnoGri193Lvg1gBUtw7zzxr3Gmy0vSyjN4hegwXqvyBSIWW9sESaPYVyY2PIgMiFxJRhylqERcKOcT8Y8E43DYYkX5SdOsmwoOmScMZH7qoZfkWtMFc2rV72JyyCbjy16U-rjVFU-7hW8x3aaNEfMiXpJWaT9fU7yQYWmUO7w9TvzpH2YW3zX3qR-b9_pZaUBQvppzJmqY-_JTSR375gI3rMNS6mPHMEkMDORE1CuN7A138tXOypV3JvB3lV6AQeYMMBgepefxPwakj8A5LDDFpsiYbBRun3MHRvh8oAlr6xKzhogtbiUYo2-RG8LSEcToNpdbPqwJHCV7BtGSnfCHzI3ZsdvC9-Q4W0UwAxUpNEsgRkd178sMLuF4Ir1XwGzH05VXYKBKY0r2uY", pass);
    }

    @Test
    public void testUpdateConfig() throws Exception {
        Path confDir = conf();
        File conf = confDir.resolve(ConfigurationHelper.ACTIVEMIX_CONFIG_FILE).toFile();
        File out = confDir.resolve("updated.conf").toFile();

        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        PropertiesConfiguration config = new PropertiesConfiguration();
        layout.load(config, new InputStreamReader(new FileInputStream(conf)));
        config.setProperty(ConfigurationHelper.ACTIVEMIX_CONNECTION_USER, "test");
        layout.save(config, new FileWriter(out, false));

        // check that the configuration was updated (we trust that layout was preserved)
        Properties p = new Properties();
        p.load(new FileInputStream(out));
        String user = p.getProperty(ConfigurationHelper.ACTIVEMIX_CONNECTION_USER);
        Assert.assertNotNull(user);
        Assert.assertEquals("test", user);
    }

}
