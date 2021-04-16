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

package org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.create.actions;

import org.openkilda.wfm.share.logger.FlowOperationsDashboardLogger;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.create.FlowMirrorPointCreateContext;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.create.FlowMirrorPointCreateFsm;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.create.FlowMirrorPointCreateFsm.Event;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.create.FlowMirrorPointCreateFsm.State;
import org.openkilda.wfm.topology.flowhs.model.RequestedFlowMirrorPoint;

import lombok.extern.slf4j.Slf4j;
import org.squirrelframework.foundation.fsm.AnonymousAction;

@Slf4j
public class OnFinishedWithErrorAction
        extends AnonymousAction<FlowMirrorPointCreateFsm, State, Event, FlowMirrorPointCreateContext> {
    private final FlowOperationsDashboardLogger dashboardLogger;

    public OnFinishedWithErrorAction(FlowOperationsDashboardLogger dashboardLogger) {
        this.dashboardLogger = dashboardLogger;
    }

    @Override
    public void execute(State from, State to, Event event, FlowMirrorPointCreateContext context,
                        FlowMirrorPointCreateFsm stateMachine) {
        RequestedFlowMirrorPoint mirrorPoint = stateMachine.getRequestedFlowMirrorPoint();
        dashboardLogger.onFailedFlowMirrorPointCreate(stateMachine.getFlowId(),
                mirrorPoint.getMirrorPointSwitchId(), mirrorPoint.getMirrorPointDirection().toString(),
                mirrorPoint.getReceiverPointSwitchId(), mirrorPoint.getReceiverPointPort(),
                mirrorPoint.getReceiverPointVlan(), stateMachine.getErrorReason());
        stateMachine.saveActionToHistory("Failed to create the flow mirror point", stateMachine.getErrorReason());
    }
}
