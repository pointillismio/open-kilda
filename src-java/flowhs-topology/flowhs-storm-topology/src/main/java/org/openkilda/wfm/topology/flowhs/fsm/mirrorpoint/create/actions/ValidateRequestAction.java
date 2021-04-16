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

import org.openkilda.messaging.Message;
import org.openkilda.messaging.error.ErrorType;
import org.openkilda.model.Flow;
import org.openkilda.model.FlowStatus;
import org.openkilda.persistence.PersistenceManager;
import org.openkilda.persistence.repositories.FlowRepository;
import org.openkilda.persistence.repositories.IslRepository;
import org.openkilda.persistence.repositories.RepositoryFactory;
import org.openkilda.persistence.repositories.SwitchPropertiesRepository;
import org.openkilda.persistence.repositories.SwitchRepository;
import org.openkilda.wfm.share.history.model.FlowEventData;
import org.openkilda.wfm.share.logger.FlowOperationsDashboardLogger;
import org.openkilda.wfm.topology.flowhs.exception.FlowProcessingException;
import org.openkilda.wfm.topology.flowhs.fsm.common.actions.NbTrackableAction;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.create.FlowMirrorPointCreateContext;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.create.FlowMirrorPointCreateFsm;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.create.FlowMirrorPointCreateFsm.Event;
import org.openkilda.wfm.topology.flowhs.fsm.mirrorpoint.create.FlowMirrorPointCreateFsm.State;
import org.openkilda.wfm.topology.flowhs.model.RequestedFlowMirrorPoint;
import org.openkilda.wfm.topology.flowhs.validation.FlowValidator;
import org.openkilda.wfm.topology.flowhs.validation.InvalidFlowException;
import org.openkilda.wfm.topology.flowhs.validation.UnavailableFlowEndpointException;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ValidateRequestAction
        extends NbTrackableAction<FlowMirrorPointCreateFsm, State, Event, FlowMirrorPointCreateContext> {
    private final FlowValidator flowValidator;
    private final FlowOperationsDashboardLogger dashboardLogger;

    public ValidateRequestAction(PersistenceManager persistenceManager,
                                 FlowOperationsDashboardLogger dashboardLogger) {
        super(persistenceManager);
        RepositoryFactory repositoryFactory = persistenceManager.getRepositoryFactory();
        FlowRepository flowRepository = repositoryFactory.createFlowRepository();
        SwitchRepository switchRepository = repositoryFactory.createSwitchRepository();
        IslRepository islRepository = repositoryFactory.createIslRepository();
        SwitchPropertiesRepository switchPropertiesRepository = repositoryFactory.createSwitchPropertiesRepository();
        flowValidator = new FlowValidator(flowRepository, switchRepository, islRepository, switchPropertiesRepository);
        this.dashboardLogger = dashboardLogger;
    }

    @Override
    protected Optional<Message> performWithResponse(State from, State to, Event event,
                                                    FlowMirrorPointCreateContext context,
                                                    FlowMirrorPointCreateFsm stateMachine) {
        String flowId = stateMachine.getFlowId();
        RequestedFlowMirrorPoint mirrorPoint = context.getMirrorPoint();

        dashboardLogger.onFlowMirrorPointCreate(flowId, mirrorPoint.getMirrorPointSwitchId(),
                mirrorPoint.getMirrorPointDirection().toString(), mirrorPoint.getReceiverPointSwitchId(),
                mirrorPoint.getReceiverPointPort(), mirrorPoint.getReceiverPointVlan());

        stateMachine.setRequestedFlowMirrorPoint(mirrorPoint);
        Flow flow = transactionManager.doInTransaction(() -> {
            Flow foundFlow = getFlow(flowId);
            if (foundFlow.getStatus() == FlowStatus.IN_PROGRESS) {
                throw new FlowProcessingException(ErrorType.REQUEST_INVALID,
                        format("Flow %s is in progress now", flowId));
            }
            return foundFlow;
        });

        try {
            flowValidator.flowMirrorPointValidate(mirrorPoint);
        } catch (InvalidFlowException e) {
            throw new FlowProcessingException(e.getType(), e.getMessage(), e);
        } catch (UnavailableFlowEndpointException e) {
            throw new FlowProcessingException(ErrorType.DATA_INVALID, e.getMessage(), e);
        }

        stateMachine.saveNewEventToHistory("Flow was validated successfully",
                FlowEventData.Event.FLOW_MIRROR_POINT_CREATE);

        return Optional.empty();
    }

    @Override
    protected String getGenericErrorMessage() {
        return "Could not create flow mirror point";
    }
}
