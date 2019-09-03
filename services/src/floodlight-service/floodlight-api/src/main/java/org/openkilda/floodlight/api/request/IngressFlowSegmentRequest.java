/* Copyright 2019 Telstra Open Source
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

package org.openkilda.floodlight.api.request;

import static java.util.Objects.requireNonNull;

import org.openkilda.messaging.MessageContext;
import org.openkilda.model.Cookie;
import org.openkilda.model.FlowEndpoint;
import org.openkilda.model.MeterConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
abstract class IngressFlowSegmentRequest extends FlowSegmentRequest {
    @JsonProperty("endpoint")
    protected final FlowEndpoint endpoint;

    @JsonProperty("meter_config")
    protected final MeterConfig meterConfig;

    IngressFlowSegmentRequest(
            MessageContext context, UUID commandId, String flowId, Cookie cookie, FlowEndpoint endpoint,
            MeterConfig meterConfig) {
        super(context, endpoint.getDatapath(), commandId, flowId, cookie);

        requireNonNull(endpoint, "Argument endpoint must not be null");

        this.endpoint = endpoint;
        this.meterConfig = meterConfig;
    }
}
