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

package org.openkilda.floodlight.flow.request;

import org.openkilda.floodlight.flow.MeterConfig;
import org.openkilda.messaging.AbstractMessage;
import org.openkilda.messaging.MessageContext;
import org.openkilda.model.SwitchId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class InstallMeteredRule extends AbstractMessage {
    /**
     * Unique identifier for the command.
     */
    @JsonProperty("command_id")
    private final UUID commandId;

    /**
     * The switch id to manage flow on. It is a mandatory parameter.
     */
    @JsonProperty("switch_id")
    final SwitchId switchId;

    @JsonProperty("config")
    private final MeterConfig config;

    @JsonCreator
    public InstallMeteredRule(
            @JsonProperty("message_context") MessageContext messageContext,
            @JsonProperty("command_id") UUID commandId,
            @JsonProperty("switch_id") SwitchId switchId,
            @JsonProperty("config") MeterConfig config) {
        super(messageContext);
        this.commandId = commandId;
        this.switchId = switchId;
        this.config = config;
    }
}
