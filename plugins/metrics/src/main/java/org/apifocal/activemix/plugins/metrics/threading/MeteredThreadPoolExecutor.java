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
package org.apifocal.activemix.plugins.metrics.threading;

import com.codahale.metrics.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apifocal.activemix.plugins.metrics.MeterContext;

public class MeteredThreadPoolExecutor extends ThreadPoolExecutor {

    private final ThreadPoolExecutor delegate;
    private final Meter submitted;
    private final Counter running;
    private final Meter completed;
    private final Timer idle;
    private final Timer duration;

    public MeteredThreadPoolExecutor(MeterContext meter, ThreadPoolExecutor delegate) {
        super(delegate.getCorePoolSize(), delegate.getMaximumPoolSize(),
            delegate.getKeepAliveTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS,
            new MeteredQueue(meter, delegate.getQueue()), new MeteredThreadFactory(meter, delegate.getThreadFactory()),
            new MeteredRejectedExecutionHandler(meter, delegate.getRejectedExecutionHandler())
        );

        this.delegate = delegate;
        this.submitted = meter.meter("submitted");
        this.running = meter.counter( "running");
        this.completed = meter.meter( "completed");
        this.idle = meter.timer("idle");
        this.duration = meter.timer("duration");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Runnable runnable) {
        submitted.mark();
        delegate.execute(new MeteredRunnable(runnable));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<?> submit(Runnable runnable) {
        submitted.mark();
        return delegate.submit(new MeteredRunnable(runnable));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Future<T> submit(Runnable runnable, T result) {
        submitted.mark();
        return delegate.submit(new MeteredRunnable(runnable), result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        submitted.mark();
        return delegate.submit(new MeteredCallable<>(task));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAll(instrumented);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAll(instrumented, timeout, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAny(instrumented);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAny(instrumented, timeout, unit);
    }

    private <T> Collection<? extends Callable<T>> instrument(Collection<? extends Callable<T>> tasks) {
        final List<MeteredCallable<T>> instrumented = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            instrumented.add(new MeteredCallable<>(task));
        }
        return instrumented;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
        return delegate.awaitTermination(l, timeUnit);
    }

    private class MeteredRunnable implements Runnable {
        private final Runnable task;
        private final Timer.Context idleContext;

        MeteredRunnable(Runnable task) {
            this.task = task;
            this.idleContext = idle.time();
        }

        @Override
        public void run() {
            idleContext.stop();
            running.inc();
            final Timer.Context durationContext = duration.time();
            try {
                task.run();
            } finally {
                durationContext.stop();
                running.dec();
                completed.mark();
            }
        }
    }

    private class MeteredCallable<T> implements Callable<T> {
        private final Callable<T> callable;

        MeteredCallable(Callable<T> callable) {
            this.callable = callable;
        }

        @Override
        public T call() throws Exception {
            running.inc();
            final Timer.Context context = duration.time();
            try {
                return callable.call();
            } finally {
                context.stop();
                running.dec();
                completed.mark();
            }
        }
    }

}
