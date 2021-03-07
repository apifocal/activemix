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

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class Destinations {

	private static final Logger LOG = LoggerFactory.getLogger(SettingsBuilder.class);
	/*
	 * This is a more restrictive pattern than URNs that conform to something closer to:
	 *  "^urn:[a-z0-9][a-z0-9-]{0,31}:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+$" (case insensitive)
	 */
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
        "^urn:[0-9a-z][0-9a-z-]{0,31}:@?[A-Za-z0-9][A-Za-z0-9-]*$",
        0);

    /**
     * Translates a URN into a valid ActiveMQ Destination
     * @param urn - {@link String} representing the URN representation of an address
     * @return a {@link String} representing the corresponding messaging destination
     */
    public static String fromUrn(String urn) {
    	if (!isValidUrnAddress(urn)) {
    		LOG.info("Not a valid address URN: {}", urn);
    		return null;
    	}

    	String[] parts = urn.split(":");
    	if (parts.length != 3 || parts[1].isEmpty() || parts[2].isEmpty()) {
    		LOG.warn("IMPOSSIBLE! Valid address should have 3 parts; incorrect regex pattern matching!", urn);
    		return null;
    	}

    	// ignore parts[0], that's the 'urn' part

    	// check if this is a direct point-to-point domain (queue) or pub-sub (topic)
    	String dn = parts[2];
    	boolean isTopic = dn.startsWith("@");
    	dn = isTopic ? dn.substring(1) : dn;

    	String fqdn = new StringBuilder(isTopic ? "topic://" : "queue://")
    		.append("(")
    		.append(parts[1].replaceAll("-", "."))
    		.append(")")
    		.append(dn)
    		.toString();
    	return fqdn;
    }

    public static String toUrn(String fqdn) {
        boolean topic = false;
        String dn = null;
        String authority = null;
        if (fqdn.startsWith("queue:")) {
            dn = trimPrefix(trimPrefix(fqdn, "queue:"), "//");
        } else if (fqdn.startsWith("topic:")) {
        	topic = true;
            dn = trimPrefix(trimPrefix(fqdn, "topic:"), "//");
        } else {
    		LOG.warn("FQDN must start with either 'queue:' or 'topic:' (but was '{}')", fqdn);
    		return null;
        }

    	int i = dn.startsWith("(") ? dn.indexOf(")") : -1;
    	authority = i > 1 ? dn.substring(1, i) : null;
    	dn = i > 1 ? dn.substring(i + 1) : null;
    	if (authority == null || dn == null) {
    		LOG.warn("FQDN '{}' is missing authority info", fqdn);
    		return null;
    	}

    	String urn = new StringBuilder("urn:")
    		.append(authority.replaceAll("\\.", "-"))
    		.append(":")
    		.append(topic ? "@" : "")
    		.append(dn.replaceAll("\\.", "-"))
    		.toString();
    	if (!isValidUrnAddress(urn)) {
    		LOG.warn("Not a valid address URN: {} (from fqdn: '{}')", urn, fqdn);
    		return null;
    	}
    	return urn;
    }

	public static String trimPrefix(String s, String prefix) {
        return s.startsWith(prefix) ? s.substring(prefix.length()) : s;
    }

    public static boolean isValidUrnAddress(String urn) {
    	return ADDRESS_PATTERN.matcher(urn).matches();
    }

}
