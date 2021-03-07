/*
 * Copyright (c) 2020-2021 apifocal LLC. All rights reserved.
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apifocal.activemix.jaas.commons.Destinations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Resources;

/**
 * TODO: Doc
 */
public class AuthorityLinkRequestsTest {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorityLinkRequestsTest.class);
    private static final List<BrokerService> BROKERS = new ArrayList<BrokerService>();
    private static final String DEFAULT_QUEUE = "Stocks";
    private static final String AA_TOKEN =
            "eyJhbGciOiJSUzI1NiJ9." +
            "eyJzdWIiOiJmb28iLCJpc3MiOiJiYXIifQ." +
            "WQl3XKlooF-wIYK3ibYyT5AueKN9TSulBLoIdyj90sXmU9boa5yUCVHrdRI5BgC1Ep0RHbAlxGO1-e_5Z-yY81Li-wvf0MIg6jbQgQOJ1IDrDcfLS8VvnHqI5bpk5BhFaRkIQsyCvz7zbKGLqzTuI3VFvjUT6CJwSGhWdt19aJei2FiIZ6iPasVBfdZyJNmCxcKAZKdLlG2GWmXMYomVjSkitxM1SsjWGtu68ANKkkkUjdOoU-Q7v9hLb9Pa9VMIZoAQV4l__lvA-1lD2d11ezXa0I7nnoGri193Lvg1gBUtw7zzxr3Gmy0vSyjN4hegwXqvyBSIWW9sESaPYVyY2PIgMiFxJRhylqERcKOcT8Y8E43DYYkX5SdOsmwoOmScMZH7qoZfkWtMFc2rV72JyyCbjy16U-rjVFU-7hW8x3aaNEfMiXpJWaT9fU7yQYWmUO7w9TvzpH2YW3zX3qR-b9_pZaUBQvppzJmqY-_JTSR375gI3rMNS6mPHMEkMDORE1CuN7A138tXOypV3JvB3lV6AQeYMMBgepefxPwakj8A5LDDFpsiYbBRun3MHRvh8oAlr6xKzhogtbiUYo2-RG8LSEcToNpdbPqwJHCV7BtGSnfCHzI3ZsdvC9-Q4W0UwAxUpNEsgRkd178sMLuF4Ir1XwGzH05VXYKBKY0r2uY";

    private static ConnectionFactory authConnectionFactory;
    private Connection authConnection;

    @BeforeClass
    public static void startBroker() throws Exception {
        createBroker("broker1");
        authConnectionFactory =  new ActiveMQConnectionFactory("vm://broker1");
    }

    @AfterClass
    public static void stopBroker() throws Exception {
        for (BrokerService b : BROKERS) {
            if (b != null) {
                b.stop();
            }
        }
    }

    @Before
    public void setupAuthConnection() throws Exception {
        authConnection = authConnectionFactory.createConnection("aa@local", AA_TOKEN);
        final Session session = authConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final MessageProducer responder = session.createProducer(null);
        MessageConsumer consumer = session.createConsumer(
            AuthorityAgent.createDestination(session, Destinations.fromUrn("urn:example:verify")));
        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(javax.jms.Message message) {
                try {
                    LOG.debug("Authority received message from stream: {}", ((ActiveMQTextMessage)message).getText());
                    LOG.debug("  reply-to: '{}\'", message.getJMSReplyTo().toString());
                    responder.send(message.getJMSReplyTo(), session.createTextMessage("OK"));
                } catch (JMSException e) {
                    LOG.debug("Error reading message: {}", e.getMessage());
                }
            }
        });
        authConnection.start();
    }

    @After
    public void teardownAuthConnection() throws Exception {
        if (authConnection != null) {
            authConnection.stop();
            authConnection = null;
        }
    }

    @Test
    public void testLogin() throws Exception {
        Assert.assertEquals(1, BROKERS.size());
        Thread.sleep(1000);

        ConnectionFactory cf =  new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection connection = cf.createConnection("user", AA_TOKEN);
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        final CountDownLatch messageSignal = new CountDownLatch(1);
        MessageConsumer consumer = session.createConsumer(session.createQueue(DEFAULT_QUEUE));
        consumer.setMessageListener(createListener("L1", messageSignal));
        connection.start();

        MessageProducer producer = session.createProducer(session.createQueue(DEFAULT_QUEUE));
        // producer.send(session.createTextMessage("QQQ: $1000.00"));

        Thread.sleep(2000);
        connection.stop();

    }

    public static BrokerService createBroker(String name) throws Exception {
        BrokerService b = BrokerFactory.createBroker("xbean:META-INF/org/apache/activemq/" + name + ".xml");
        if (!name.equals(b.getBrokerName())) {
            LOG.warn("Broker name mismatch (expecting '{}'). Check configuration.", name);
            return null;
        }
        BROKERS.add(b);
        b.start();
        b.waitUntilStarted();
        LOG.info("Broker '{}' started.", name);
        return b;
    }

    private MessageListener createListener(final String id, final CountDownLatch latch) {
        return new MessageListener() {
            @Override
            public void onMessage(javax.jms.Message message) {
                try {
                    LOG.debug("Listener '{}' received user message from stream: {}", id, ((ActiveMQTextMessage)message).getDestination().getPhysicalName());
                    LOG.debug("  message-id: \"{}\"content: \"{}\".",
                        message.getJMSMessageID().toString(), ((ActiveMQTextMessage)message).getText());
                    LOG.debug("  reply-to: '{}\'", message.getJMSReplyTo().toString());
                    if (latch != null) {
                        latch.countDown();
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        };
    }

}
