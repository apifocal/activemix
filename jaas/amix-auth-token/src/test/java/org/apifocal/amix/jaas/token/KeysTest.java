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

import org.junit.Assert;
import org.junit.Test;


public class KeysTest {
    private static String SAMPLE_MD5 = "MD5:33:f2:84:1d:c3:0d:d5:12:57:f3:c6:e2:1f:cc:52:af";
    private static String SAMPLE_SHA256 = "SHA256:NUzLZ0kLqJh47YsctlAFMXT54fVNCHDM7zbAkoQ2a6g";

    @Test
    public void testMatchDigestAlgorithm() throws Exception {
        Assert.assertEquals("MD5", Keys.digestAlgorithmFromId("MD5"));
        Assert.assertEquals("SHA-1", Keys.digestAlgorithmFromId("SHA1"));
        Assert.assertEquals("SHA-256", Keys.digestAlgorithmFromId("SHA256"));
        Assert.assertEquals("SHA-512", Keys.digestAlgorithmFromId("SHA512"));
        Assert.assertNull(Keys.digestAlgorithmFromId("hello-world"));
     }

    @Test
    public void testMatchDigestName() throws Exception {
        Assert.assertEquals("MD5", Keys.digestAlgorithmToId("MD5"));
        Assert.assertEquals("SHA1", Keys.digestAlgorithmToId("SHA-1"));
        Assert.assertEquals("SHA256", Keys.digestAlgorithmToId("SHA-256"));
        Assert.assertEquals("SHA512", Keys.digestAlgorithmToId("SHA-512"));
        Assert.assertNull(Keys.digestAlgorithmToId("hello-world"));
     }

    @Test
    public void testGetDigestName() throws Exception {
        Assert.assertEquals("MD5", Keys.digestAlgorithmFromId(Keys.getDigestName(SAMPLE_MD5)));
        Assert.assertEquals("SHA-256", Keys.digestAlgorithmFromId(Keys.getDigestName(SAMPLE_SHA256)));
    }

}
