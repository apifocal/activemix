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
package org.apifocal.amix.plugins.metrics.context;

import com.codahale.metrics.*;
import org.apifocal.amix.plugins.metrics.MeterContext;

import java.util.function.Supplier;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * A default implementation (thus simple) of meter context.
 *
 * It delegates creation of metric
 */
public class SimpleMeterContext implements MeterContext {

    private final MetricRegistry metricRegistry;
    private final String prefix;

    public SimpleMeterContext(MetricRegistry metricRegistry, String prefix) {
        this.metricRegistry = metricRegistry;
        this.prefix = prefix;
    }

    public SimpleMeterContext(MetricRegistry metricRegistry, Class<?> clazz) {
        this(metricRegistry, clazz.getName());
    }

    @Override
    public Timer timer(String name) {
        return metricRegistry.timer(createName(name));
    }

    @Override
    public Counter counter(String name) {
        return metricRegistry.counter(createName(name));
    }

    @Override
    public Meter meter(String name) {
        return metricRegistry.meter(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Gauge<Long> gauge(String name, Supplier<Long> supplier) {
        return metricRegistry.gauge(createName(name), () -> supplier::get);
    }

    @Override
    public MeterContext child(String name) {
        return new SimpleMeterContext(metricRegistry, createName(name));
    }

    @Override
    public MeterContext sibling(String name) {
        return new SimpleMeterContext(metricRegistry, name);
    }

    private String createName(String ... segments) {
        return name(prefix, segments);
    }

}
