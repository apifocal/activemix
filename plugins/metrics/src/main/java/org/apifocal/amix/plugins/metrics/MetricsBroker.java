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
package org.apifocal.amix.plugins.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.activemq.broker.*;
import org.apache.activemq.broker.region.Destination;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.*;
import org.apifocal.amix.plugins.metrics.context.SimpleMeterContext;
import org.apifocal.amix.plugins.metrics.threading.MeteredThreadPoolExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MetricsBroker extends BrokerFilter {

    private final MeterContext meter;

    /**
     * An alternative to Runnable type which allows to throw exception from method body.
     * Because most of methods in broker interface throws an exception we just bypass them to next broker filter.
     */
    @FunctionalInterface
    interface Execution {
        void execute() throws Exception;
    }

    public MetricsBroker(Broker next, MetricRegistry metricRegistry) {
        super(next);
        this.meter = new SimpleMeterContext(metricRegistry, "broker." + getBrokerId());
    }

    @Override
    public void acknowledge(ConsumerBrokerExchange consumerExchange, MessageAck ack) throws Exception {
        Timer timer = meter.timer("acknowledge");

        timer(timer, () -> getNext().acknowledge(consumerExchange, ack));
    }

    @Override
    public Response messagePull(ConnectionContext context, MessagePull pull) throws Exception {
        Timer timer = meter.timer("messagePull");

        return timer.time(() -> getNext().messagePull(context, pull));
    }

    @Override
    public void addConnection(ConnectionContext context, ConnectionInfo info) throws Exception {
        increase("connections", getConnectionMeterContext(context, info));

        Timer timer = meter.timer("addConnection");
        timer(timer, () -> getNext().addConnection(context, info));
    }

    @Override
    public void removeConnection(ConnectionContext context, ConnectionInfo info, Throwable error) throws Exception {
        decrease("connections", getConnectionMeterContext(context, info));

        Timer timer = meter.timer("removeConnection");
        timer(timer, () -> getNext().removeConnection(context, info, error));
    }

    @Override
    public void addSession(ConnectionContext context, SessionInfo info) throws Exception {
        increase("sessions", getSessionMeterContext(context, info));

        Timer timer = meter.timer("addSession");
        timer(timer, () -> getNext().addSession(context, info));
    }

    @Override
    public void removeSession(ConnectionContext context, SessionInfo info) throws Exception {
        decrease("sessions", getSessionMeterContext(context, info));

        Timer timer = meter.timer("removeSession");
        timer(timer, () -> getNext().removeSession(context, info));
    }

    @Override
    public Subscription addConsumer(ConnectionContext context, ConsumerInfo info) throws Exception {
        increase("consumers", getConsumerMeterContext(context, info));

        Timer timer = meter.timer("addConsumer");
        return timer.time(() -> getNext().addConsumer(context, info));
    }

    @Override
    public void addProducer(ConnectionContext context, ProducerInfo info) throws Exception {
        increase("producers", getProducerMeterContext(context, info));

        Timer timer = meter.timer("addConsumer");
        timer(timer, () -> getNext().addProducer(context, info));
    }

    @Override
    public void removeConsumer(ConnectionContext context, ConsumerInfo info) throws Exception {
        decrease("consumers", getConsumerMeterContext(context, info));

        Timer timer = meter.timer("removeConsumer");
        timer(timer, () -> getNext().removeConsumer(context, info));
    }

    @Override
    public void removeProducer(ConnectionContext context, ProducerInfo info) throws Exception {
        decrease("producers", getProducerMeterContext(context, info));

        Timer timer = meter.timer("removeProducer");
        timer(timer, () -> getNext().removeProducer(context, info));
    }

    @Override
    public Destination addDestination(ConnectionContext context, ActiveMQDestination destination, boolean createIfTemporary) throws Exception {
        increase("destinations", getDestinationMeterContext(context, destination));

        Timer timer = meter.timer("addDestination");
        return timer.time(() -> getNext().addDestination(context, destination, createIfTemporary));
    }

    @Override
    public void removeDestination(ConnectionContext context, ActiveMQDestination destination, long timeout) throws Exception {
        decrease("destinations", getDestinationMeterContext(context, destination));

        Timer timer = meter.timer("removeDestination");
        timer(timer, () -> getNext().removeDestination(context, destination, timeout));
    }

    @Override
    public ThreadPoolExecutor getExecutor() {
        return new MeteredThreadPoolExecutor(meter.child("pool"), getNext().getExecutor());
    }

    @Override
    public void networkBridgeStarted(BrokerInfo brokerInfo, boolean createdByDuplex, String remoteIp) {
        increase("bridges", getNetworkBridgeMeterContext(brokerInfo, createdByDuplex, remoteIp));

        Timer timer = meter.timer("networkBridgeStarted");
        timer.time(() -> getNext().networkBridgeStarted(brokerInfo, createdByDuplex, remoteIp));
    }

    @Override
    public void networkBridgeStopped(BrokerInfo brokerInfo) {
        decrease("bridges", getNetworkBridgeMeterContext(brokerInfo));

        Timer timer = meter.timer("networkBridgeStopped");
        timer.time(() -> getNext().networkBridgeStopped(brokerInfo));
    }

    private void timer(Timer timer, Execution runnable) throws Exception {
        timer.time(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                runnable.execute();
                return null;
            }
        });
    }

    private void increase(String name, MeterContext ... contexts) {
        increase(name, Arrays.stream(contexts));
    }

    private void increase(String name, List<MeterContext> contexts) {
        increase(name, contexts.stream());
    }

    private void increase(String name, Stream<MeterContext> contexts) {
        counter(name, contexts, Counter::inc);
    }

    private void decrease(String name, MeterContext ... contexts) {
        decrease(name, Arrays.stream(contexts));
    }

    private void decrease(String name, List<MeterContext> contexts) {
        decrease(name, contexts.stream());
    }

    private void decrease(String name, Stream<MeterContext> contexts) {
        counter(name, contexts, Counter::dec);
    }

    private void counter(String name, Stream<MeterContext> contexts, Consumer<Counter> callback) {
        contexts.map(context -> context.counter(name))
            .forEach(callback);
    }

    // extractors

    private List<MeterContext> getConnectionMeterContext(ConnectionContext context, ConnectionInfo info) {
        List<MeterContext> contexts = new ArrayList<>();
        contexts.add(meter);

        forClientIp(info).ifPresent(contexts::add);
        forClientId(context).ifPresent(contexts::add);
        forConnectionId(context).ifPresent(contexts::add);
        return contexts;
    }

    private List<MeterContext> getSessionMeterContext(ConnectionContext context, SessionInfo info) {
        List<MeterContext> contexts = new ArrayList<>();
        contexts.add(meter);

        forClientId(context).ifPresent(contexts::add);
        forConnectionId(context).ifPresent(contexts::add);
        forSessionId(info).ifPresent(contexts::add);
        return contexts;
    }

    private List<MeterContext> getConsumerMeterContext(ConnectionContext context, ConsumerInfo info) {
        List<MeterContext> contexts = new ArrayList<>();
        contexts.add(meter);

        forClientId(context).ifPresent(contexts::add);
        forConnectionId(context).ifPresent(contexts::add);
        forConsumerId(info).ifPresent(contexts::add);
        forCompositeDestination(info.getDestination()).ifPresent(contexts::addAll);
        return contexts;
    }

    private List<MeterContext> getProducerMeterContext(ConnectionContext context, ProducerInfo info) {
        List<MeterContext> contexts = new ArrayList<>();
        contexts.add(meter);

        forClientId(context).ifPresent(contexts::add);
        forConnectionId(context).ifPresent(contexts::add);
        forProducerId(info).ifPresent(contexts::add);
        forCompositeDestination(info.getDestination()).ifPresent(contexts::addAll);
        return contexts;
    }

    private List<MeterContext> getDestinationMeterContext(ConnectionContext context, ActiveMQDestination info) {
        List<MeterContext> contexts = new ArrayList<>();
        contexts.add(meter);

        forClientId(context).ifPresent(contexts::add);
        forConnectionId(context).ifPresent(contexts::add);
        // We are already in destination context, we don't need to extract this here.
        //forCompositeDestination(info).ifPresent(contexts::addAll);
        return contexts;
    }

    private List<MeterContext> getNetworkBridgeMeterContext(BrokerInfo brokerInfo, boolean createdByDuplex, String remoteIp) {
        List<MeterContext> contexts = getNetworkBridgeMeterContext(brokerInfo);
        contexts.add(meter.sibling("remote." + remoteIp));

        if (createdByDuplex) {
            contexts.add(meter.sibling("duplex"));
        }

        return contexts;
    }

    private List<MeterContext> getNetworkBridgeMeterContext(BrokerInfo brokerInfo) {
        List<MeterContext> contexts = new ArrayList<>();

        Arrays.stream(brokerInfo.getPeerBrokerInfos())
            .map(BrokerInfo::getBrokerId)
            .map(BrokerId::getValue)
            .map(id -> "remote.peer." + id)
            .map(meter::sibling)
            .forEach(contexts::add);

        return contexts;
    }

    // helpers for transforming internal ActiveMQ structures into meter contexts.
    private Optional<MeterContext> forClientIp(ConnectionInfo context) {
        return Optional.of(context)
            .map(ConnectionInfo::getClientIp)
            .map(this::simplifyClientIp)
            .map(id -> meter.sibling("client.ip." + id));
    }

    private Optional<MeterContext> forClientId(ConnectionContext context) {
        return Optional.of(context)
            .map(ConnectionContext::getClientId)
            .map(this::simplifyId)
            .map(id -> meter.sibling("client." + id));
    }

    private Optional<MeterContext> forConnectionId(ConnectionContext context) {
        return Optional.of(context)
            .map(ConnectionContext::getConnectionId)
            .map(ConnectionId::getValue)
            .map(this::simplifyId)
            .map(id -> meter.sibling("connection." + id));
    }

    private Optional<MeterContext> forSessionId(SessionInfo context) {
        return Optional.of(context)
            .map(SessionInfo::getSessionId)
            .map(SessionId::getValue)
            .map(id -> meter.sibling("session." + id));
    }

    private Optional<MeterContext> forConsumerId(ConsumerInfo info) {
        return Optional.of(info)
            .map(ConsumerInfo::getConsumerId)
            .map(ConsumerId::getValue)
            .map(id -> meter.sibling("consumer." + id));
    }

    private Optional<MeterContext> forProducerId(ProducerInfo info) {
        return Optional.of(info)
            .map(ProducerInfo::getProducerId)
            .map(ProducerId::getValue)
            .map(id -> meter.sibling("producer." + id));
    }

    private Optional<List<MeterContext>> forCompositeDestination(ActiveMQDestination destination) {
        if (!destination.isComposite()) {
            return forDestination(destination)
                .map(Arrays::asList);
        }

        List<MeterContext> context = new ArrayList<>();
        for (ActiveMQDestination dest : destination.getCompositeDestinations()) {
            forDestination(dest).ifPresent(context::add);
        }

        if (context.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(context);
    }

    private Optional<MeterContext> forDestination(ActiveMQDestination destination) {
        return Optional.of(destination)
            .map(dst -> dst.getDestinationTypeAsString() + "." + dst.getPhysicalName())
            .map(name -> meter.sibling("destination." + name));
    }

    /**
     * Reduce identifier to first segment if identifier is composed from dashes.
     *
     * @param id identifier in complex form ie ID:120302-xxx-xxx-xx.
     * @return Short representation of identifier containing just first segment.
     */
    private String simplifyId(String id) {
        if (id.contains("-")) {
            return id.substring(0, id.indexOf("-"));
        }

        return id;
    }

    /**
     * Extracts ip address from its URI representation.
     *
     * ActiveMQ IP information is kept as full uri, for example tcp://172.26.0.5:33212. This means that each socket
     * opened by client is unique, but that's not what we want - we want just IP for stats and nothing else.
     *
     * @param clientIp ActiveMQ client address.
     * @return IP of client.
     */
    private String simplifyClientIp(String clientIp) {
        if (clientIp.startsWith("tcp://")) {
            return clientIp.substring(6, clientIp.lastIndexOf(":"));
        }

        return clientIp;
    }
}
