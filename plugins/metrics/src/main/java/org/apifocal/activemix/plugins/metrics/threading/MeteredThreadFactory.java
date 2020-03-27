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
package org.apifocal.activemix.plugins.metrics.threading;

import java.util.concurrent.ThreadFactory;

import org.apifocal.activemix.plugins.metrics.MeterContext;

public class MeteredThreadFactory implements ThreadFactory {

    private final MeterContext meter;
    private final ThreadFactory delegate;

    public MeteredThreadFactory(MeterContext meter, ThreadFactory delegate) {
        this.meter = meter;
        this.delegate = delegate;
    }

    @Override
    public Thread newThread(Runnable r) {
        MeteredRunnable runnable = new MeteredRunnable(r);
        Thread thread = delegate.newThread(runnable);
        runnable.setTimer(meter.timer(thread.getName()));
        return thread;
    }


}
