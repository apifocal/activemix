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
package org.apifocal.activemix.jaas.escalate;

import org.apache.activemq.broker.BrokerRegistry;
import org.apache.activemq.broker.BrokerService;
import org.apifocal.activemix.commons.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication Authority client
 */
public class AsyncAuthorityLink implements AuthorityLink {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncAuthorityLink.class);

	private BrokerService broker;
    private AuthorityAgent proxy;

	public AsyncAuthorityLink(Settings settings) {
	}

    public void initialize(String brokerName, String authority) {
    	BrokerRegistry br = BrokerRegistry.getInstance();
    	this.broker = br.lookup(brokerName);
    	if (broker == null) {
    		throw new IllegalStateException("Invalid broker name: " + brokerName);
    	}

    	proxy = new AuthorityAgent(brokerName, authority);
    	broker.addService(proxy);
    }

    public boolean verify(String user, String credential) {
    	LOG.debug("Verifying user's '{}' credentials '{}'", user, credential);
    	try {
    		// This will only work the first time, it won't try to start a second time...
			proxy.start();
		} catch (Exception e) {
			LOG.warn("Failed to start AuthorityAgent proxy", e);
			return false;
		}

    	return proxy.verify(user, credential);
    }

}
