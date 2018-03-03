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

/**
 * TODO: Doc
 */
public interface StatsCollector {

	/**
	 * 
	 * @param timeSent time in milliseconds the ping was sent
	 * @param timeReceived time in milliseconds the ping was received
	 * 
	 * We use milliseconds for now mostly to stay aligned with the JMS spec.
	 */
	public void collect(long timeSent, long timeReceived);
}
