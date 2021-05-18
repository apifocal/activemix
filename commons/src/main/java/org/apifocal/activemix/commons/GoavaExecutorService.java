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

import com.google.common.util.concurrent.AbstractService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: doc
 */
public class GoavaExecutorService extends AbstractService {

    private static final Logger LOG = LoggerFactory.getLogger(GoavaExecutorService.class);

    private AuthorityService authorityService;

    public GoavaExecutorService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Override
	protected void doStart() {
        try {
            authorityService.start();
        } catch (Exception e) {
            notifyFailed(e);
        }
	}

	@Override
	protected void doStop() {
        try {
            authorityService.stop();
        } catch (Exception e) {
            notifyFailed(e);
        }
        notifyStopped();
	}
}
    