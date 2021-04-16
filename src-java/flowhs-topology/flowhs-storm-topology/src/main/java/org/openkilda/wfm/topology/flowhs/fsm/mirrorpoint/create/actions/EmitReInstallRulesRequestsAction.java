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

import static java.lang.String.format;

import org.openkilda.floodlight.api.request.factory.FlowSegmentRequestFactory;
import org.openkilda.model.Flow;
import org.openkilda.model.FlowPath;
import org.openkilda.persistence.PersistenceManager;
import org.openkilda.wfm.CommandContext;
import org.openkilda.wfm.share.flow.resources.FlowResourcesManager;
import org.openkilda.wfm.topology.flowhs.fsm.common.actions.FlowProcessingAction;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.create.FlowMirrorPointCreateContext;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.create.FlowMirrorPointCreateFsm;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.create.FlowMirrorPointCreateFsm.Event;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.create.FlowMirrorPointCreateFsm.State;
import org.openkilda.wfm.topology.flowhs.model.RequestedFlowMirrorPoint;
import org.openkilda.wfm.topology.flowhs.service.FlowCommandBuilder;
import org.openkilda.wfm.topology.flowhs.service.FlowCommandBuilderFactory;
import org.openkilda.wfm.topology.flowhs.utils.SpeakerInstallSegmentEmitter;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Slf4j
public class EmitReInstallRulesRequestsAction
        extends FlowProcessingAction<FlowMirrorPointCreateFsm, State, Event, FlowMirrorPointCreateContext> {
    private final FlowCommandBuilderFactory commandBuilderFactory;

    public EmitReInstallRulesRequestsAction(PersistenceManager persistenceManager,
                                            FlowResourcesManager resourcesManager) {
        super(persistenceManager);
        commandBuilderFactory = new FlowCommandBuilderFactory(resourcesManager);
    }

    @Override
    protected void perform(State from, State to,
                           Event event, FlowMirrorPointCreateContext context, FlowMirrorPointCreateFsm stateMachine) {
        stateMachine.getReInstallCommands().clear();
        stateMachine.getPendingCommands().clear();

        String flowId = stateMachine.getFlowId();
        Flow flow = getFlow(flowId);

        FlowCommandBuilder commandBuilder = commandBuilderFactory.getBuilder(flow.getEncapsulationType());

        Collection<FlowSegmentRequestFactory> commands = buildCommands(commandBuilder, stateMachine, flow);

        // emitting
        SpeakerInstallSegmentEmitter.INSTANCE.emitBatch(
                stateMachine.getCarrier(), commands, stateMachine.getReInstallCommands());
        stateMachine.getReInstallCommands().forEach(
                (key, value) -> stateMachine.getPendingCommands().put(key, value.getSwitchId()));

        if (commands.isEmpty()) {
            stateMachine.saveActionToHistory("No need to re-install rules");
        } else {
            stateMachine.saveActionToHistory("Commands for re-installing rules have been sent");
            stateMachine.setRulesReInstalled(true);
        }
    }

    private Collection<FlowSegmentRequestFactory> buildCommands(FlowCommandBuilder commandBuilder,
                                                                FlowMirrorPointCreateFsm stateMachine, Flow flow) {
        FlowPath path;
        FlowPath oppositePath;
        RequestedFlowMirrorPoint mirrorPoint = stateMachine.getRequestedFlowMirrorPoint();
        switch (mirrorPoint.getMirrorPointDirection()) {
            case FORWARD:
                path = flow.getForwardPath();
                oppositePath = flow.getReversePath();
                break;
            case REVERSE:
                path = flow.getReversePath();
                oppositePath = flow.getForwardPath();
                break;
            default:
                throw new IllegalArgumentException(format("Flow mirror points direction %s is not supported",
                        mirrorPoint.getMirrorPointDirection()));
        }

        CommandContext context = stateMachine.getCommandContext();
        if (mirrorPoint.getMirrorPointSwitchId().equals(path.getSrcSwitchId())) {
            return new ArrayList<>(commandBuilder
                    .buildIngressOnlyOneDirection(context, flow, path, oppositePath,
                            buildBasePathContextForInstall(path.getSrcSwitchId())));

        } else if (mirrorPoint.getMirrorPointSwitchId().equals(path.getDestSwitchId())) {
            return new ArrayList<>(commandBuilder
                    .buildEgressOnlyOneDirection(context, flow, path, oppositePath));
        }

        return Collections.emptyList();
    }
}
