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
package org.apifocal.activemx.plugins.metrics.threading;

import org.apifocal.activemx.plugins.metrics.MeterContext;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MeteredQueue implements BlockingQueue<Runnable> {

    private final MeterContext meter;
    private final BlockingQueue<Runnable> delegate;

    public MeteredQueue(MeterContext meter, BlockingQueue<Runnable> delegate) {
        this.meter = meter;
        this.delegate = delegate;
        meter.gauge("size", () -> (long) size());
        meter.gauge("remainingCapacity", () -> (long) remainingCapacity());
    }

    @Override
    public boolean add(Runnable runnable) {
        return delegate.add(runnable);
    }

    @Override
    public boolean offer(Runnable runnable) {
        return delegate.offer(runnable);
    }

    @Override
    public void put(Runnable runnable) throws InterruptedException {
        delegate.put(runnable);
    }

    @Override
    public boolean offer(Runnable runnable, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.offer(runnable, timeout, unit);
    }

    @Override
    public Runnable take() throws InterruptedException {
        return delegate.take();
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.poll(timeout, unit);
    }

    @Override
    public int remainingCapacity() {
        return delegate.remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public int drainTo(Collection<? super Runnable> c) {
        return delegate.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super Runnable> c, int maxElements) {
        return delegate.drainTo(c, maxElements);
    }

    @Override
    public Runnable remove() {
        return delegate.remove();
    }

    @Override
    public Runnable poll() {
        return delegate.poll();
    }

    @Override
    public Runnable element() {
        return delegate.element();
    }

    @Override
    public Runnable peek() {
        return delegate.peek();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public Iterator<Runnable> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Runnable> c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean removeIf(Predicate<? super Runnable> filter) {
        return delegate.removeIf(filter);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public Spliterator<Runnable> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public Stream<Runnable> stream() {
        return delegate.stream();
    }

    @Override
    public Stream<Runnable> parallelStream() {
        return delegate.parallelStream();
    }
}
