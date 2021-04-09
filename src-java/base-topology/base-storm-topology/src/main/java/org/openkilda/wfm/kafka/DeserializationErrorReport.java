/* Copyright 2021 Telstra Open Source
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.openkilda.wfm.kafka;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

@Value
public class DeserializationErrorReport {
    String topic;

    String baseClassName;

    String errorMessage;

    String rawSource;

    public static DeserializationErrorReport createFromException(
            String topic, Class<?> baseClass, byte[] rawSource, Exception error) {
        return createFromException(
                topic, baseClass, StringUtils.toEncodedString(rawSource, StandardCharsets.UTF_8), error);
    }

    /**
     * Create {@link DeserializationErrorReport} instance using data from exception raised during deserialization.
     */
    public static DeserializationErrorReport createFromException(
            String topic, Class<?> baseClass, String rawSource, Exception error) {
        String errorMessage = String.format(
                "Failed to deserialize message in kafka-topic \"%s\" using base class %s: %s",
                topic, baseClass.getName(), error.getMessage());
        return new DeserializationErrorReport(topic, baseClass.getName(), rawSource, errorMessage);
    }
}
