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

package org.openkilda.floodlight.command.group;

import org.openkilda.floodlight.command.SpeakerCommandReport;
import org.openkilda.messaging.MessageContext;
import org.openkilda.messaging.Utils;
import org.openkilda.model.MirrorConfig;
import org.openkilda.model.MirrorConfig.MirrorConfigData;
import org.openkilda.model.SwitchId;

import com.google.common.collect.Lists;
import lombok.Getter;
import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFGroupMod;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
abstract class AbstractGroupInstall<T extends SpeakerCommandReport> extends GroupCommand<T> {
    // payload
    protected final MirrorConfig mirrorConfig;

    AbstractGroupInstall(MessageContext messageContext, SwitchId switchId, MirrorConfig mirrorConfig) {
        super(messageContext, switchId);
        this.mirrorConfig = mirrorConfig;
    }

    protected OFGroupMod makeGroupAddMessage() {
        final OFFactory ofFactory = getSw().getOFFactory();
        OFActionOutput mainOutput = ofFactory.actions().buildOutput()
                .setPort(OFPort.of(mirrorConfig.getMainPort())).build();
        OFBucket mainBucket = ofFactory.buildBucket()
                .setActions(Collections.singletonList(mainOutput))
                .setWatchGroup(OFGroup.ANY)
                .build();

        List<OFBucket> buckets = Lists.newArrayList(mainBucket);

        for (MirrorConfigData mirrorConfigData : mirrorConfig.getMirrorConfigDataSet()) {
            List<OFAction> mirrorActions = new ArrayList<>();
            mirrorActions.add(ofFactory.actions().buildOutput()
                    .setPort(OFPort.of(mirrorConfigData.getMirrorPort())).build());
            int mirrorVlan = mirrorConfigData.getMirrorVlan();
            if (mirrorVlan > 0) {
                mirrorActions.add(ofFactory.actions().buildPushVlan().setEthertype(EthType.of(Utils.ETH_TYPE)).build());
                mirrorActions.add(ofFactory.actions().buildSetField().setField(ofFactory.oxms().buildVlanVid()
                        .setValue(OFVlanVidMatch.ofVlan(mirrorVlan)).build())
                        .build());
            }
            buckets.add(ofFactory.buildBucket().setActions(mirrorActions).setWatchGroup(OFGroup.ANY).build());
        }

        return ofFactory.buildGroupAdd()
                .setGroup(OFGroup.of((int) mirrorConfig.getGroupId().getValue()))
                .setGroupType(OFGroupType.ALL)
                .setBuckets(buckets).build();
    }
}
