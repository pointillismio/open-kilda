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

package org.openkilda.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.Set;

@Value
@Builder
public class MirrorConfig implements Serializable {

    @JsonProperty("group_id")
    GroupId groupId;

    @JsonProperty("main_port")
    int mainPort;

    @JsonProperty("mirror_data_set")
    Set<MirrorConfigData> mirrorConfigDataSet;

    @JsonProperty("clean_excess_group")
    boolean cleanExcessGroup;

    @JsonCreator
    public MirrorConfig(@JsonProperty("group_id") GroupId groupId,
                        @JsonProperty("main_port") int mainPort,
                        @JsonProperty("mirror_data_set") Set<MirrorConfigData> mirrorConfigDataSet,
                        @JsonProperty("clean_excess_group") boolean cleanExcessGroup) {
        this.groupId = groupId;
        this.mainPort = mainPort;
        this.mirrorConfigDataSet = mirrorConfigDataSet;
        this.cleanExcessGroup = cleanExcessGroup;
    }

    @Value
    public static class MirrorConfigData implements Serializable {
        @JsonProperty("mirror_port")
        int mirrorPort;

        @JsonProperty("mirror_vlan")
        int mirrorVlan;

        @JsonCreator
        public MirrorConfigData(@JsonProperty("mirror_port") int mirrorPort,
                                @JsonProperty("mirror_vlan") int mirrorVlan) {
            this.mirrorPort = mirrorPort;
            this.mirrorVlan = mirrorVlan;
        }
    }
}
