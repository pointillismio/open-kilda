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

package org.openkilda.integration.model;

import org.openkilda.model.DetectedDevice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

import java.io.Serializable;

/**
 * The Class FlowEndpoint.
 *
 * @author Gaurav Chugh
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"switch-id", "port-id", "vlan-id"})
@Data
public class FlowEndpoint implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("switch_id")
    private String switchId;

    @JsonProperty("port_number")
    private int portId;

    @JsonProperty("vlan_id")
    private int vlanId;
    
    @JsonProperty("inner_vlan_id")
    private int innerVlanId;
    
    @JsonProperty("switch-name")
    private String switchName;
    
    @JsonProperty("detect_connected_devices")
    private DetectedDevice detectedDevice;

    public String getSwitchId() {
        return switchId;
    }

    public void setSwitchId(final String switchId) {
        this.switchId = switchId;
    }

    public int getPortId() {
        return portId;
    }

    public void setPortId(final int portId) {
        this.portId = portId;
    }

    public int getVlanId() {
        return vlanId;
    }

    public void setVlanId(final int vlanId) {
        this.vlanId = vlanId;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }
    
    public String getSwitchName() {
        return switchName;
    }

    public void setSwitchName(String switchName) {
        this.switchName = switchName;
    }
    
    @Override
    public String toString() {
        return "FlowEndpoint [switchId=" + switchId + ", portId=" + portId + ", vlanId=" + vlanId
                + "]";
    }
}
