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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.util.ServiceStopper;
import org.apache.activemq.util.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication Authority client
 */
public class AuthorityAgent extends ServiceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AuthorityAgent.class);

    private final String broker;
	private final String authority;
	private final ConnectionFactory connectionFactory;

	private Connection connection;
	private Session session;
	private Destination dnRequests;
	private Destination dnReplies; // how do we know this is unique?

	public AuthorityAgent(String brokerName, String authority) {
		// TODO: maybe use a singleton Connection for all authorities (once we support many)?
		this.broker = brokerName;
		this.authority = authority;
	    // Use in-memory vm transport, no need for authentication or persistence
		this.connectionFactory =  new ActiveMQConnectionFactory("vm://" + broker);
	}

	protected void doStart() throws Exception {
		LOG.debug("Starting AuthorityAgent for the '{}' authority", authority);
        connection = connectionFactory.createConnection("whocares@local", "FIXME");
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        dnRequests = session.createQueue(dnAuthRequest(authority));
        dnReplies = session.createQueue(dnAuthReply(authority));

        connection.start();
	}

	protected void doStop(ServiceStopper stopper) throws Exception {
		// wait for outstanding requests ?
		if (connection != null) {
			LOG.debug("Stopping AuthorityAgent for the '{}' authority", authority);
			if ((connection instanceof ActiveMQConnection) && ((ActiveMQConnection)connection).isClosing()) {
				connection.stop();
				connection.close();
			}

			session = null;
			connection = null;
		}
	}

	public boolean verify(String user, String credential) {
		LOG.info("Escalating credential verification for user '{}' to '{}'", user, dnRequests.toString());
		try {
			MessageProducer producer = session.createProducer(dnRequests);
	        producer.send(session.createTextMessage(user));
	        Thread.sleep(200);
		} catch (Exception e) {
			LOG.warn("FAILED");
		}

		// FIXME: not all credentials are good...
		return true;
	}

	// TODO: should we refactor this out of here? maybe some helper class?
	private static String dnAuthRequest(String authority) {
		return "_" + authority + ".verify";
	}

	// TODO: should we refactor this out of here? maybe some helper class?
	private static String dnAuthReply(String authority) {
		return "_" + authority + ".verify";
	}

}
