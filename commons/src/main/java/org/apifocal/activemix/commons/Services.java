/*
 * Copyright 2017 apifocal LLC.
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
package org.apifocal.activemix.commons;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;

/**
 * 
 */
public final class Services {
    private static final Logger LOG = LoggerFactory.getLogger(Services.class);
    private static final long DEF_TIMEOUT = 3000;


    public static ServiceManager manage(final Service service) {
        return manage(service, simpleListener(), DEF_TIMEOUT);
    }

    public static ServiceManager manage(final Service service, Listener listener, long timeout) {
        Set<Service> services = new HashSet<>();
        services.add(service);
        return manage(services, simpleListener(), timeout);
    }

    public static ServiceManager manage(final Set<Service> services) {
        return manage(services, simpleListener(), MoreExecutors.directExecutor(), DEF_TIMEOUT);
    }

    public static ServiceManager manage(final Set<Service> services, Listener listener, long timeout) {
        return manage(services, listener, MoreExecutors.directExecutor(), timeout);
    }

    public static ServiceManager manage(final Set<Service> services, Listener listener, Executor executor, long timeout) {
        ServiceManager manager = new ServiceManager(services);
        manager.addListener(listener, executor);
        manager.startAsync();
        try {
            manager.awaitHealthy(timeout, TimeUnit.MILLISECONDS);
            return manager;
        } catch (TimeoutException e) {
            LOG.warn("Failed to start services: {}", e.getLocalizedMessage());
            manager.stopAsync();
        }
        return null;
    }

    public static void unmanage(final ServiceManager manager) {
        // mildly convenient helper, collapses 4 lines into 1
        if (manager != null) {
            LOG.trace("Stopping ServiceManager");
            manager.stopAsync();
            manager.awaitStopped();
        }
    }

    public static Listener simpleListener() {
        return new Listener() {
            @Override public void healthy() {
                LOG.debug("ServiceManager status: healthy");
            }
            @Override public void stopped() {
                LOG.debug("ServiceManager status: stopped");
            }
            @Override public void failure(Service service) {
                LOG.debug("ServiceManager status: failure: {}", 
                    service.failureCause() == null ? null : service.failureCause().getLocalizedMessage());
            }
        };
    }


    private Services() {
        // utility
    }

}
