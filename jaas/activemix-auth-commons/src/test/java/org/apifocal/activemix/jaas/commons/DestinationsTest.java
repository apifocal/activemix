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

import org.apifocal.activemix.commons.Destinations;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/*
 */
// TODO: add tests for invalid addresses and FQDNs
public class DestinationsTest {

	@Test
    public void testValidDestinationUrns() throws Exception {
        Assert.assertTrue(Destinations.isValidUrnAddress("urn:0:pizza"));
        Assert.assertTrue(Destinations.isValidUrnAddress("urn:0:Pizza"));
        Assert.assertTrue(Destinations.isValidUrnAddress("urn:0:PIZZA"));
        Assert.assertTrue(Destinations.isValidUrnAddress("urn:0:@PIZZA"));
        Assert.assertTrue(Destinations.isValidUrnAddress("urn:example:@PIZZA"));

        Assert.assertTrue(Destinations.isValidUrnAddress("urn:food:@pizza"));
        Assert.assertTrue(Destinations.isValidUrnAddress("urn:food-xyz:company1-business"));
        Assert.assertTrue(Destinations.isValidUrnAddress("urn:food-xyz:company2-business"));
        Assert.assertTrue(Destinations.isValidUrnAddress("urn:food-xyz-c1:business"));
	}

	@Test
    public void testInvalidDestinationUrns() throws Exception {
        Assert.assertFalse(Destinations.isValidUrnAddress("urn:0:@")); // destination name is empty
        Assert.assertFalse(Destinations.isValidUrnAddress("urn:0:@@pizza")); // invalid character '@' destination name
     }

	@Test
	@Ignore("Add rules for reserved authorities, etc...")
    public void testDestinationConstraints() throws Exception {
	}

	@Test
    public void testDestinationNames() throws Exception {
        Assert.assertEquals("queue://(0)pizza", Destinations.fromUrn("urn:0:pizza"));

        Assert.assertEquals("topic://(0)pizza", Destinations.fromUrn("urn:0:@pizza"));
	}

	@Test
    public void testUrnAddresses() throws Exception {
        Assert.assertEquals("urn:0:pizza", Destinations.toUrn("queue://(0)pizza"));

        Assert.assertEquals("urn:0:@pizza", Destinations.toUrn("topic://(0)pizza"));
	}

}