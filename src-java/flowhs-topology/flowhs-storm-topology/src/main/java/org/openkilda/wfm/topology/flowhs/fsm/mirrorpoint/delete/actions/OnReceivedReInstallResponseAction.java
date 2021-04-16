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

package org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.delete.actions;

import static java.lang.String.format;

import org.openkilda.floodlight.api.request.factory.FlowSegmentRequestFactory;
import org.openkilda.floodlight.api.response.SpeakerFlowSegmentResponse;
import org.openkilda.floodlight.flow.response.FlowErrorResponse;
import org.openkilda.wfm.topology.flowhs.fsm.common.actions.HistoryRecordingAction;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.delete.FlowMirrorPointDeleteContext;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.delete.FlowMirrorPointDeleteFsm;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.delete.FlowMirrorPointDeleteFsm.Event;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.delete.FlowMirrorPointDeleteFsm.State;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class OnReceivedReInstallResponseAction
        extends HistoryRecordingAction<FlowMirrorPointDeleteFsm, State, Event, FlowMirrorPointDeleteContext> {
    private static final String FAILED_TO_REINSTALL_RULE_ACTION = "Failed to re-install rule";

    private final int speakerCommandRetriesLimit;

    public OnReceivedReInstallResponseAction(int speakerCommandRetriesLimit) {
        this.speakerCommandRetriesLimit = speakerCommandRetriesLimit;
    }

    @Override
    protected void perform(State from, State to, Event event,
                           FlowMirrorPointDeleteContext context, FlowMirrorPointDeleteFsm stateMachine) {
        SpeakerFlowSegmentResponse response = context.getSpeakerFlowResponse();
        UUID commandId = response.getCommandId();
        FlowSegmentRequestFactory command = stateMachine.getReInstallCommands().get(commandId);
        if (!stateMachine.getPendingCommands().containsKey(commandId) || command == null) {
            log.info("Received a response for unexpected command: {}", response);
            return;
        }

        if (response.isSuccess()) {
            stateMachine.getPendingCommands().remove(commandId);

            stateMachine.saveActionToHistory("Rule was re-installed",
                    format("The rule was re-installed: switch %s, cookie %s",
                            response.getSwitchId(), command.getCookie()));
        } else {
            FlowErrorResponse errorResponse = (FlowErrorResponse) response;

            int retries = stateMachine.getRetriedCommands().getOrDefault(commandId, 0);
            if (retries < speakerCommandRetriesLimit) {
                stateMachine.getRetriedCommands().put(commandId, ++retries);

                stateMachine.saveErrorToHistory(FAILED_TO_REINSTALL_RULE_ACTION, format(
                        "Failed to re-install the rule: commandId %s, switch %s, cookie %s. Error %s. "
                                + "Retrying (attempt %d)",
                        commandId, errorResponse.getSwitchId(), command.getCookie(), errorResponse, retries));

                stateMachine.getCarrier().sendSpeakerRequest(command.makeInstallRequest(commandId));

            } else {
                stateMachine.getPendingCommands().remove(commandId);

                stateMachine.saveErrorToHistory(FAILED_TO_REINSTALL_RULE_ACTION, format(
                        "Failed to re-install the rule: commandId %s, switch %s, cookie %s. Error: %s",
                        commandId, errorResponse.getSwitchId(), command.getCookie(), errorResponse));

                stateMachine.getFailedCommands().put(commandId, errorResponse);
            }
        }

        if (stateMachine.getPendingCommands().isEmpty()) {
            if (stateMachine.getFailedCommands().isEmpty()) {
                log.debug("Received responses for all pending re-install commands of the flow {}",
                        stateMachine.getFlowId());
                stateMachine.fire(Event.RULES_REINSTALLED);
            } else {
                String errorMessage = format("Received error response(s) for %d re-install commands",
                        stateMachine.getFailedCommands().size());
                stateMachine.saveErrorToHistory(errorMessage);
                stateMachine.fireError(errorMessage);
            }
        }
    }
}
