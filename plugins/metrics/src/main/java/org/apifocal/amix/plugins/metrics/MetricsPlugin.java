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

import com.codahale.metrics.MetricRegistry;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPlugin;

/**
 * A metrics plugin - attaches codehale/dropwizard statistics to activemq.
 *
 * After applying this plugin tracking of connections, destinations and other interactions will be made.
 *
 * @org.apache.xbean.XBean element="metrics"
 */
public class MetricsPlugin implements BrokerPlugin {

    /**
     * A metric registry which is used to attach meters.
     */
    private final MetricRegistry metricRegistry;

    /**
     * Create new plugin.
     *
     * @param metricRegistry Metric registry to use for putting stats.
     */
    public MetricsPlugin(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public Broker installPlugin(Broker broker) throws Exception {
        return new MetricsBroker(broker, metricRegistry);
    }
}
