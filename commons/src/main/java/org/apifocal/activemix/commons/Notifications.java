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

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * TODO: doc
 */
public final class Notifications {

    public static final String PREFIX_QUEUE = "queue:";
    public static final String PREFIX_TOPIC = "topic:";
    public static final String PREFIX_DOUBLE_SLASH = "//";

    public static Predicate<String> isQueue() {
        return Predicates.IS_QUEUE;
    }

    public static Function<String, String> queueName() {
        return Functions.QUEUE_NAME;
    }

    public static Predicate<String> isTopic() {
        return Predicates.IS_TOPIC;
    }

    public static Function<String, String> topicName() {
        return Functions.TOPIC_NAME;
    }

    enum Predicates implements Predicate<String> {
        /** @see Notifications#isQueue() */
        IS_QUEUE {
            @Override public boolean apply(String input) {
                return input != null && input.startsWith(PREFIX_QUEUE);
            }
            @Override public String toString() {
                return "Notifications.isQueue()";
            }
        },
        /** @see Notifications#isQueue() */
        IS_TOPIC {
            @Override public boolean apply(String input) {
                return input != null && input.startsWith(PREFIX_TOPIC);
            }
            @Override public String toString() {
                return "Notifications.isTopic()";
            }
        },
    }

    enum Functions implements Function<String, String> {
        /** @see Notifications#queueName() */
        QUEUE_NAME {
            @Override public String apply(String input) {
                if (isQueue().apply(input)) {
                    String dn = input.substring(PREFIX_QUEUE.length());
                    if (dn.startsWith(PREFIX_DOUBLE_SLASH)) {
                        dn = dn.substring(PREFIX_DOUBLE_SLASH.length());
                    }
                    return dn;
                }
                return null;
            }
        },
        /** @see Notifications#topicName() */
        TOPIC_NAME {
            @Override public String apply(String input) {
                if (isTopic().apply(input)) {
                    String dn = input.substring(PREFIX_TOPIC.length());
                    if (dn.startsWith(PREFIX_DOUBLE_SLASH)) {
                        dn = dn.substring(PREFIX_DOUBLE_SLASH.length());
                    }
                    return dn;
                }
                return null;
            }
        },
    }

}
    