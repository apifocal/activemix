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
package org.apifocal.amix.tools.ping;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * TODO: Doc
 */
public class Pinger {

    private final long DEFAULT_TTL = 120000; // milliseconds
    private final int DEFAULT_THROTTLE = 20; // milliseconds
    // TODO: add undocumented feature to force throttle to 0 to flood broker?

    private final String url;
    private final String dn;
    private String user;
    private String pass;

    private ConnectionFactory factory;
    private Connection connection;

    private String data;

    private boolean async = false;
    private int interval = 0;
    private int count = 1;
    private int throttle = DEFAULT_THROTTLE;
    private int rx = 0;
    private CountDownLatch counter = new CountDownLatch(1);
    private AtomicBoolean green = new AtomicBoolean(true);
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public Pinger(String url, String dn) {
        this.url = url;
        // randomize destination name to avoid receiving foreign ping(s)
        this.dn = dn + "." + RandomStringUtils.randomAlphabetic(6);
    }

    public Pinger credentials(String user, String password) {
        if (user != null && password != null && !user.isEmpty() && !password.isEmpty()) {
            this.user = user;
            this.pass = password;
        }
        return this;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void randomData(int length) {
        this.data = RandomStringUtils.randomAlphanumeric(length);
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public void setCount(int count) {
        this.count = count;
        counter = new CountDownLatch(this.count);
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void noThrottle(boolean nothrottle) {
        if (nothrottle) {
            this.async = true;
            this.throttle = 0;
        }
    }

    public void start() throws Exception {
        factory =  new ActiveMQConnectionFactory(url);
        connection = createConnection(factory, user, pass);
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination d = createDestination(session, dn);

        MessageConsumer consumer = session.createConsumer(d);
        consumer.setMessageListener(pingListener());
        connection.start();
    }

    public void run() throws Exception {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination d = createDestination(session, dn);
        final MessageProducer producer = session.createProducer(d);
        producer.setTimeToLive(DEFAULT_TTL); // no point in keeping pings too longer at broker

        executor.execute(new Runnable() {
            public void run() {
                Message message;
                try {
                    int tx = 0;
                    long delay = interval > 0 ? interval * 1000 : throttle;
                    for(tx = 0; tx == 0 || !counter.await(delay, TimeUnit.MILLISECONDS); ) {
                        if (async || green.get()) { // received, send another one
                            green.set(false);
                            message = session.createTextMessage(data);
                            message.setJMSTimestamp(System.currentTimeMillis());
                            producer.send(message);
                            if (++tx >= count) { // only increment if message sent
                                break;
                            }
                        }
                    }
                    System.out.println("Completed: " + tx + " messages sent");
                } catch (Exception e) {
                }
            }
        });

        counter.await();
        System.out.println("Completed: " + rx + " messages received");
    }

    public void stop() throws Exception {
        executor.shutdown();
        connection.stop();
        connection.close();
    }

    protected MessageListener pingListener() {
        return new MessageListener() {
            public void onMessage(Message message) {
                try {
                    if (message instanceof TextMessage) {
                        long time = message.getJMSTimestamp();
                        System.out.printf("ping: destination=%s, size=%d bytes, time=%d ms\n",
                            dn, data.length(), System.currentTimeMillis() - time);
                        green.set(true);
                    }
                } catch (JMSException e) {
                }
                rx++;
                counter.countDown();
            }
        };
    }

    protected static Connection createConnection(ConnectionFactory cf, String user, String pass) throws JMSException {
        return (user != null && !user.isEmpty()) ? cf.createConnection(user,pass) : cf.createConnection();
    }

    protected static Destination createDestination(Session s, String dn) throws JMSException {
        if (dn == null) {
            throw new IllegalArgumentException("Destination name cannot be 'null'");
        }
        if (dn.startsWith("queue:")) {
            dn = dn.substring("queue:".length());
            dn = trimDoubleSlash(dn);
            return s.createQueue(trimDoubleSlash(dn));
        } else if (dn.startsWith("topic:")) {
            dn = dn.substring("topic:".length());
            dn = trimDoubleSlash(dn);
            return s.createTopic(trimDoubleSlash(dn));
        }
        return null;
    }
    
    protected static String trimDoubleSlash(String s) {
        return s.startsWith("//") ? s.substring("//".length()) : s;
    }

}
