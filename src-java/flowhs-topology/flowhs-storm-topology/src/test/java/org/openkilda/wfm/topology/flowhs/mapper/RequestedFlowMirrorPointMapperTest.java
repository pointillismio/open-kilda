/* Copyright 2020 Telstra Open Source
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

package org.openkilda.wfm.topology.flowhs.mapper;

import static org.junit.Assert.assertEquals;

import org.openkilda.messaging.command.flow.FlowMirrorPointCreateRequest;
import org.openkilda.model.SwitchId;
import org.openkilda.wfm.topology.flowhs.model.RequestedFlowMirrorPoint;
import org.openkilda.wfm.topology.flowhs.model.RequestedFlowMirrorPoint.MirrorPointDirection;

import org.junit.Test;

public class RequestedFlowMirrorPointMapperTest {

    public static final String FLOW_ID = "flow_id";
    private static final String MIRROR_POINT_ID = "mirror_point_id";
    private static final String MIRROR_POINT_DIRECTION = "forward";
    public static final SwitchId MIRROR_POINT_SWITCH_ID = new SwitchId("1");
    public static final SwitchId RECEIVER_POINT_SWITCH_ID = new SwitchId("2");
    public static final int RECEIVER_POINT_PORT = 2;
    public static final int RECEIVER_POINT_VLAN = 4;

    @Test
    public void testFlowMirrorPointCreateRequestToRequestedFlowMirrorPoint() {
        FlowMirrorPointCreateRequest request = FlowMirrorPointCreateRequest.builder()
                .flowId(FLOW_ID)
                .mirrorPointId(MIRROR_POINT_ID)
                .mirrorPointDirection(MIRROR_POINT_DIRECTION)
                .mirrorPointSwitchId(MIRROR_POINT_SWITCH_ID)
                .receiverPointSwitchId(RECEIVER_POINT_SWITCH_ID)
                .receiverPointPort(RECEIVER_POINT_PORT)
                .receiverPointVlan(RECEIVER_POINT_VLAN)
                .build();

        RequestedFlowMirrorPoint flowMirrorPoint = RequestedFlowMirrorPointMapper.INSTANCE.map(request);

        assertEquals(request.getFlowId(), flowMirrorPoint.getFlowId());
        assertEquals(request.getMirrorPointId(), flowMirrorPoint.getMirrorPointId());
        assertEquals(MirrorPointDirection.FORWARD, flowMirrorPoint.getMirrorPointDirection());
        assertEquals(request.getMirrorPointSwitchId(), flowMirrorPoint.getMirrorPointSwitchId());
        assertEquals(request.getReceiverPointSwitchId(), flowMirrorPoint.getReceiverPointSwitchId());
        assertEquals(request.getReceiverPointPort(), flowMirrorPoint.getReceiverPointPort());
        assertEquals(request.getReceiverPointVlan(), flowMirrorPoint.getReceiverPointVlan());
    }
}
