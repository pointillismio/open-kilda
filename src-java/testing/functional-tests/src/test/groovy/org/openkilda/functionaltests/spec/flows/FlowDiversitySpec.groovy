package org.openkilda.functionaltests.spec.flows

import static org.junit.jupiter.api.Assumptions.assumeTrue
import static org.openkilda.functionaltests.extension.tags.Tag.LOW_PRIORITY
import static org.openkilda.functionaltests.extension.tags.Tag.SMOKE
import static org.openkilda.testing.Constants.WAIT_OFFSET

import org.openkilda.functionaltests.HealthCheckSpecification
import org.openkilda.functionaltests.extension.failfast.Tidy
import org.openkilda.functionaltests.extension.tags.Tags
import org.openkilda.functionaltests.helpers.PathHelper
import org.openkilda.functionaltests.helpers.Wrappers
import org.openkilda.functionaltests.helpers.model.SwitchPair
import org.openkilda.messaging.info.event.IslChangeType
import org.openkilda.messaging.info.event.PathNode
import org.openkilda.messaging.payload.flow.FlowPathPayload
import org.openkilda.testing.model.topology.TopologyDefinition.Switch

import org.springframework.beans.factory.annotation.Value
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Narrative
import spock.lang.See

@See("https://github.com/telstra/open-kilda/tree/develop/docs/design/solutions/pce-diverse-flows")
@Narrative("""
This test suite verifies the ability to create diverse flows in the system. Diverse flows are flows that should not 
overlap at all or the overlapping should be minimal. Such flows form a so-called diversity group. Ideally, the diversity 
group should not have flows with overlapping paths. But it depends on the cost of paths. The paths of flows from 
the same diversity group may overlap if the cost of each non-overlapping path is more than the cost of the overlapping 
path. The cost of paths for diverse flows is calculated in real time and consists of the following parameters: 

1. The cost of ISL involved in the flow path (taken from DB);
2. (diversity.switch.cost) * (the number of diverse flows going through this switch);
3. (diversity.isl.cost) * (the number of diverse flows going through this ISL). 

Refer to https://github.com/telstra/open-kilda/issues/1231 for more details.
""")
@Tags([LOW_PRIORITY])
class FlowDiversitySpec extends HealthCheckSpecification {

    @Value('${diversity.isl.cost}')
    int diversityIslCost

    @Value('${diversity.switch.cost}')
    int diversitySwitchCost

    @Tidy
    @Tags(SMOKE)
    def "Able to create diverse flows"() {
        given: "Two active neighboring switches with three not overlapping paths at least"
        def switchPair = getSwitchPair(3)

        when: "Create three flows with diversity enabled"
        def flow1 = flowHelper.randomFlow(switchPair, false)
        def flow2 = flowHelper.randomFlow(switchPair, false, [flow1]).tap { it.diverseFlowId = flow1.id }
        def flow3 = flowHelper.randomFlow(switchPair, false, [flow1, flow2]).tap { it.diverseFlowId = flow2.id }
        [flow1, flow2, flow3].each { flowHelper.addFlow(it) }

        then: "All flows have diverse flow IDs in response"
        northbound.getFlow(flow1.id).diverseWith.sort() == [flow2.id, flow3.id].sort()
        northbound.getFlow(flow2.id).diverseWith.sort() == [flow1.id, flow3.id].sort()
        northbound.getFlow(flow3.id).diverseWith.sort() == [flow1.id, flow2.id].sort()

        and: "All flows have different paths"
        def allInvolvedIsls = [flow1, flow2, flow3].collectMany {
            pathHelper.getInvolvedIsls(PathHelper.convert(northbound.getFlowPath(it.id)))
        }
        allInvolvedIsls.unique(false) == allInvolvedIsls

        cleanup: "Delete flows"
        [flow1, flow2, flow3].each { it && flowHelper.deleteFlow(it.id) }
    }

    @Tidy
    def "Able to update flows to become diverse"() {
        given: "Two active neighboring switches with three not overlapping paths at least"
        def switchPair = getSwitchPair(3)

        and: "Create three flows"
        def flow1 = flowHelper.randomFlow(switchPair, false)
        def flow2 = flowHelper.randomFlow(switchPair, false, [flow1])
        def flow3 = flowHelper.randomFlow(switchPair, false, [flow1, flow2])
        [flow1, flow2, flow3].each { flowHelper.addFlow(it) }

        def (flow1Path, flow2Path, flow3Path) = [flow1, flow2, flow3].collect {
            PathHelper.convert(northbound.getFlowPath(it.id))
        }
        assert [flow1Path, flow2Path, flow3Path].toSet().size() == 1

        when: "Update the second flow to become diverse"
        flowHelper.updateFlow(flow2.id, flow2.tap { it.diverseFlowId = flow1.id })

        then: "The flow became diverse and changed the path"
        def flow2PathUpdated = PathHelper.convert(northbound.getFlowPath(flow2.id))
        flow2PathUpdated != flow2Path

        and: "All flows except last one have the 'diverse_with' field"
        northbound.getFlow(flow1.id).diverseWith == [flow2.id]
        northbound.getFlow(flow2.id).diverseWith == [flow1.id]
        !northbound.getFlow(flow3.id).diverseWith

        when: "Update the third flow to become diverse"
        flowHelper.updateFlow(flow3.id, flow3.tap { it.diverseFlowId = flow2.id })

        then: "The flow became diverse and all flows have different paths"
        def flow3PathUpdated = PathHelper.convert(northbound.getFlowPath(flow3.id))
        [flow1Path, flow2PathUpdated, flow3PathUpdated].toSet().size() == 3

        def allInvolvedIsls = [flow1Path, flow2PathUpdated, flow3PathUpdated].collectMany {
            pathHelper.getInvolvedIsls(it)
        }
        allInvolvedIsls.unique(false) == allInvolvedIsls

        cleanup: "Delete flows"
        [flow1, flow2, flow3].each { it && flowHelper.deleteFlow(it.id) }
    }

    @Tidy
    @Tags(SMOKE)
    def "Able to update flows to become not diverse"() {
        given: "Two active neighboring switches with three not overlapping paths at least"
        def switchPair = getSwitchPair(3)

        and: "Create three flows with diversity enabled"
        def flow1 = flowHelper.randomFlow(switchPair, false)
        def flow2 = flowHelper.randomFlow(switchPair, false, [flow1]).tap { it.diverseFlowId = flow1.id }
        def flow3 = flowHelper.randomFlow(switchPair, false, [flow1, flow2]).tap { it.diverseFlowId = flow2.id }
        [flow1, flow2, flow3].each { flowHelper.addFlow(it) }

        def (flow1Path, flow2Path, flow3Path) = [flow1, flow2, flow3].collect {
            PathHelper.convert(northbound.getFlowPath(it.id))
        }
        def allInvolvedIsls = [flow1Path, flow2Path, flow3Path].collectMany { pathHelper.getInvolvedIsls(it) }
        assert allInvolvedIsls.unique(false) == allInvolvedIsls

        when: "Update the second flow to become not diverse"
        flowHelper.updateFlow(flow2.id, flow2.tap { it.diverseFlowId = "" })

        then: "The flow became not diverse and rerouted to the more preferable path (path of the first flow)"
        def flow2PathUpdated = PathHelper.convert(northbound.getFlowPath(flow2.id))
        flow2PathUpdated != flow2Path
        flow2PathUpdated == flow1Path

        and: "The 'diverse_with' field is removed"
        !northbound.getFlow(flow2.id).diverseWith

        when: "Update the third flow to become not diverse"
        flowHelper.updateFlow(flow3.id, flow3.tap { it.diverseFlowId = "" })

        then: "The flow became not diverse and rerouted to the more preferable path (path of the first flow)"
        def flow3PathUpdated = PathHelper.convert(northbound.getFlowPath(flow3.id))
        flow3PathUpdated != flow3Path
        flow3PathUpdated == flow1Path

        and: "The 'diverse_with' field is removed"
        !northbound.getFlow(flow3.id).diverseWith

        cleanup: "Delete flows"
        [flow1, flow2, flow3].each { it && flowHelper.deleteFlow(it.id) }
    }

    @Tidy
    @Tags(SMOKE)
    def "Diverse flows are built through the same path if there are no alternative paths available"() {
        given: "Two active neighboring switches with two not overlapping paths at least"
        def switchPair = getSwitchPair(2)

        and: "Create a flow going through these switches"
        def flow1 = flowHelper.randomFlow(switchPair)
        flowHelper.addFlow(flow1)
        def flow1Path = PathHelper.convert(northbound.getFlowPath(flow1.id))

        and: "Make all alternative paths unavailable (bring ports down on the source switch)"
        List<PathNode> broughtDownPorts = []
        switchPair.paths.findAll { it != flow1Path }.unique { it.first() }.each { path ->
            def src = path.first()
            broughtDownPorts.add(src)
            antiflap.portDown(src.switchId, src.portNo)
        }
        Wrappers.wait(WAIT_OFFSET) {
            assert northbound.getAllLinks().findAll {
                it.state == IslChangeType.FAILED
            }.size() == broughtDownPorts.size() * 2
        }

        when: "Create the second flow with diversity enabled"
        def flow2 = flowHelper.randomFlow(switchPair, false, [flow1]).tap { it.diverseFlowId = flow1.id }
        flowHelper.addFlow(flow2)
        def flow2Path = PathHelper.convert(northbound.getFlowPath(flow2.id))

        then: "The second flow is built through the same path as the first flow"
        flow2Path == flow1Path

        cleanup: "Restore topology, delete flows and reset costs"
        [flow1, flow2].each { it && flowHelper.deleteFlow(it.id) }
        broughtDownPorts.every { antiflap.portUp(it.switchId, it.portNo) }
        Wrappers.wait(discoveryInterval + WAIT_OFFSET) {
            northbound.getAllLinks().each { assert it.state != IslChangeType.FAILED }
        }
        database.resetCosts()
    }

    @Tidy
    @Tags(SMOKE)
    def "Links and switches get extra cost that is considered while calculating diverse flow paths"() {
        given: "Two active neighboring switches with three not overlapping paths at least"
        def switchPair = getSwitchPair(3)

        and: "Create a flow going through these switches"
        def flow1 = flowHelper.randomFlow(switchPair)
        flowHelper.addFlow(flow1)
        def flow1Path = PathHelper.convert(northbound.getFlowPath(flow1.id))

        and: "Make each alternative path less preferable than the first flow path"
        def altPaths = switchPair.paths
        altPaths.remove(flow1Path)

        def flow1PathCost = pathHelper.getCost(flow1Path) + diversityIslCost + diversitySwitchCost * 2
        altPaths.each { altPath ->
            def altPathCost = pathHelper.getCost(altPath) + diversitySwitchCost * 2
            int difference = flow1PathCost - altPathCost
            def firstAltPathIsl = pathHelper.getInvolvedIsls(altPath)[0]
            int firstAltPathIslCost = database.getIslCost(firstAltPathIsl)
            northbound.updateLinkProps([islUtils.toLinkProps(firstAltPathIsl,
                    ["cost": (firstAltPathIslCost + Math.abs(difference) + 1).toString()])])
        }

        when: "Create the second flow with diversity enabled"
        def flow2 = flowHelper.randomFlow(switchPair, false, [flow1]).tap { it.diverseFlowId = flow1.id }
        flowHelper.addFlow(flow2)
        def flow2Path = PathHelper.convert(northbound.getFlowPath(flow2.id))

        then: "The flow is built through the most preferable path (path of the first flow)"
        flow2Path == flow1Path

        when: "Create the third flow with diversity enabled"
        def flow3 = flowHelper.randomFlow(switchPair, false, [flow1, flow2]).tap {
            it.diverseFlowId = flow2.id
        }
        flowHelper.addFlow(flow3)
        def flow3Path = PathHelper.convert(northbound.getFlowPath(flow3.id))

        then: "The flow is built through one of alternative paths because they are preferable already"
        def involvedIsls = [flow2Path, flow3Path].collectMany { pathHelper.getInvolvedIsls(it) }
        flow3Path != flow2Path
        involvedIsls.unique(false) == involvedIsls

        cleanup: "Delete flows and link props"
        [flow1, flow2, flow3].each { it && flowHelper.deleteFlow(it.id) }
        northbound.deleteLinkProps(northbound.getAllLinkProps())
    }

    @Tidy
    def "Able to get flow paths with correct overlapping segments stats (casual flows)"() {
        given: "Two active neighboring switches with three not overlapping paths at least"
        def switchPair = getSwitchPair(3)

        and: "Create three flows with diversity enabled"
        def flow1 = flowHelper.randomFlow(switchPair, false)
        def flow2 = flowHelper.randomFlow(switchPair, false, [flow1]).tap { it.diverseFlowId = flow1.id }
        def flow3 = flowHelper.randomFlow(switchPair, false, [flow1, flow2]).tap { it.diverseFlowId = flow2.id }
        [flow1, flow2, flow3].each { flowHelper.addFlow(it) }

        when: "Get flow path for all flows"
        def (flow1Path, flow2Path, flow3Path) = [flow1, flow2, flow3].collect { northbound.getFlowPath(it.id) }

        then: "Flow path response for all flows has correct overlapping segments stats"
        def flow2SwitchCount = pathHelper.getInvolvedSwitches(PathHelper.convert(flow2Path)).size()
        def flow3SwitchCount = pathHelper.getInvolvedSwitches(PathHelper.convert(flow3Path)).size()
        def expectedValuesMap = [
                diverseGroup: [
                        (flow1.id): [islCount: 0, switchCount: 2, islPercent: 0, switchPercent: 100],
                        (flow2.id): [islCount     : 0, switchCount: 2, islPercent: 0,
                                     switchPercent: (2 * 100 / flow2SwitchCount).toInteger()],
                        (flow3.id): [islCount     : 0, switchCount: 2, islPercent: 0,
                                     switchPercent: (2 * 100 / flow3SwitchCount).toInteger()]
                ],
                otherFlows  : [
                        (flow1.id): [
                                (flow2.id): [islCount: 0, switchCount: 2, islPercent: 0, switchPercent: 100],
                                (flow3.id): [islCount: 0, switchCount: 2, islPercent: 0, switchPercent: 100]
                        ],
                        (flow2.id): [
                                (flow1.id): [islCount     : 0, switchCount: 2, islPercent: 0,
                                             switchPercent: (2 * 100 / flow2SwitchCount).toInteger()],
                                (flow3.id): [islCount     : 0, switchCount: 2, islPercent: 0,
                                             switchPercent: (2 * 100 / flow2SwitchCount).toInteger()]
                        ],
                        (flow3.id): [
                                (flow1.id): [islCount     : 0, switchCount: 2, islPercent: 0,
                                             switchPercent: (2 * 100 / flow3SwitchCount).toInteger()],
                                (flow2.id): [islCount     : 0, switchCount: 2, islPercent: 0,
                                             switchPercent: (2 * 100 / flow3SwitchCount).toInteger()]
                        ]
                ]
        ]
        verifySegmentsStats([flow1Path, flow2Path, flow3Path], expectedValuesMap)

        cleanup: "Delete flows"
        [flow1, flow2, flow3].each { it && flowHelper.deleteFlow(it.id) }
    }

    @Tidy
    @Issue("https://github.com/telstra/open-kilda/issues/2072")
    @Ignore("Functionality is currently not supported yet")
    def "Able to get flow paths with correct overlapping segments stats (single-switch flows)"() {
        given: "Two active switches"
        def (Switch sw1, Switch sw2) = topology.getActiveSwitches()[0..1]

        and: "Create two single-switch flows with diversity enabled on the first switch"
        def flow1 = flowHelper.singleSwitchFlow(sw1, false)
        def flow2 = flowHelper.singleSwitchFlow(sw1, false, [flow1]).tap { it.diverseFlowId = flow1.id }
        [flow1, flow2].each { flowHelper.addFlow(it) }

        and: "Create the third single-switch flow with diversity enabled on the second switch"
        def flow3 = flowHelper.singleSwitchFlow(sw2, false).tap { it.diverseFlowId = flow2.id }
        flowHelper.addFlow(flow3)

        when: "Get flow path for all flows"
        def (flow1Path, flow2Path, flow3Path) = [flow1, flow2, flow3].collect { northbound.getFlowPath(it.id) }

        then: "Flow path response for all flows has correct overlapping segments stats"
        def expectedValuesMap = [
                diverseGroup: [
                        (flow1.id): [islCount: 0, switchCount: 1, islPercent: 0, switchPercent: 100],
                        (flow2.id): [islCount: 0, switchCount: 1, islPercent: 0, switchPercent: 100],
                        (flow3.id): [islCount: 0, switchCount: 0, islPercent: 0, switchPercent: 0]
                ],
                otherFlows  : [
                        (flow1.id): [
                                (flow2.id): [islCount: 0, switchCount: 1, islPercent: 0, switchPercent: 100],
                                (flow3.id): [islCount: 0, switchCount: 0, islPercent: 0, switchPercent: 0]
                        ],
                        (flow2.id): [
                                (flow1.id): [islCount: 0, switchCount: 1, islPercent: 0, switchPercent: 100],
                                (flow3.id): [islCount: 0, switchCount: 0, islPercent: 0, switchPercent: 0]
                        ],
                        (flow3.id): [
                                (flow1.id): [islCount: 0, switchCount: 0, islPercent: 0, switchPercent: 0],
                                (flow2.id): [islCount: 0, switchCount: 0, islPercent: 0, switchPercent: 0]
                        ]
                ]
        ]
        verifySegmentsStats([flow1Path, flow2Path, flow3Path], expectedValuesMap)

        cleanup: "Delete flows"
        [flow1, flow2, flow3].each { it && flowHelper.deleteFlow(it.id) }
    }

    @Tidy
    @Issue("https://github.com/telstra/open-kilda/issues/2072")
    @Ignore("Functionality is currently not supported yet")
    def "Able to get flow paths with correct overlapping segments stats (casual + single-switch flows)"() {
        given: "Two active not neighboring switches"
        def switchPair = topologyHelper.getNotNeighboringSwitchPair()

        and: "Create a casual flow going through these switches"
        def flow1 = flowHelper.randomFlow(switchPair, false)
        flowHelper.addFlow(flow1)

        and: "Create a single-switch with diversity enabled on the source switch of the first flow"
        def flow2 = flowHelper.singleSwitchFlow(switchPair.src, false, [flow1]).tap { it.diverseFlowId = flow1.id }
        flow2 = flowHelper.addFlow(flow2)

        and: "Create a single-switch with diversity enabled on the destination switch of the first flow"
        def flow3 = flowHelper.singleSwitchFlow(switchPair.dst, false, [flow1]).tap { it.diverseFlowId = flow2.id }
        flowHelper.addFlow(flow3)

        when: "Get flow path for all flows"
        def (flow1Path, flow2Path, flow3Path) = [flow1, flow2, flow3].collect { northbound.getFlowPath(it.id) }

        then: "Flow path response for all flows has correct overlapping segments stats"
        def flow1SwitchCount = pathHelper.getInvolvedSwitches(PathHelper.convert(flow1Path)).size()
        def expectedValuesMap = [
                diverseGroup: [
                        (flow1.id): [islCount     : 0, switchCount: 2, islPercent: 0,
                                     switchPercent: (2 * 100 / flow1SwitchCount).toInteger()],
                        (flow2.id): [islCount: 0, switchCount: 1, islPercent: 0, switchPercent: 100],
                        (flow3.id): [islCount: 0, switchCount: 1, islPercent: 0, switchPercent: 100]
                ],
                otherFlows  : [
                        (flow1.id): [
                                (flow2.id): [islCount: 0, switchCount: 1, islPercent: 0, switchPercent: 100],
                                (flow3.id): [islCount: 0, switchCount: 1, islPercent: 0, switchPercent: 100]
                        ],
                        (flow2.id): [
                                (flow1.id): [islCount     : 0, switchCount: 1, islPercent: 0,
                                             switchPercent: (2 * 100 / flow1SwitchCount).toInteger()],
                                (flow3.id): [islCount: 0, switchCount: 0, islPercent: 0, switchPercent: 0]
                        ],
                        (flow3.id): [
                                (flow1.id): [islCount     : 0, switchCount: 1, islPercent: 0,
                                             switchPercent: (2 * 100 / flow1SwitchCount).toInteger()],
                                (flow2.id): [islCount: 0, switchCount: 0, islPercent: 0, switchPercent: 0]
                        ]
                ]
        ]
        verifySegmentsStats([flow1Path, flow2Path, flow3Path], expectedValuesMap)

        cleanup: "Delete flows"
        [flow1, flow2, flow3].each { it && flowHelper.deleteFlow(it.id) }
    }

    SwitchPair getSwitchPair(minNotOverlappingPaths) {
        topologyHelper.getAllNeighboringSwitchPairs().find {
            it.paths.collect { pathHelper.getInvolvedIsls(it) }.unique { a, b -> a.intersect(b) ? 0 : 1 }.size() >=
                    minNotOverlappingPaths
        } ?: assumeTrue(false, "No suiting switches found")
    }

    void verifySegmentsStats(List<FlowPathPayload> flowPaths, Map expectedValuesMap) {
        flowPaths.each { flow ->
            with(flow.diverseGroupPayload) { diverseGroup ->
                verifyAll(diverseGroup.overlappingSegments) {
                    islCount == expectedValuesMap["diverseGroup"][flow.id]["islCount"]
                    switchCount == expectedValuesMap["diverseGroup"][flow.id]["switchCount"]
                    islPercent == expectedValuesMap["diverseGroup"][flow.id]["islPercent"]
                    switchPercent == expectedValuesMap["diverseGroup"][flow.id]["switchPercent"]
                }
                with(diverseGroup.otherFlows) { otherFlows ->
                    assert (flowPaths*.id - flow.id).containsAll(otherFlows*.id)
                    otherFlows.each { otherFlow ->
                        verifyAll(otherFlow.segmentsStats) {
                            islCount == expectedValuesMap["otherFlows"][flow.id][otherFlow.id]["islCount"]
                            switchCount == expectedValuesMap["otherFlows"][flow.id][otherFlow.id]["switchCount"]
                            islPercent == expectedValuesMap["otherFlows"][flow.id][otherFlow.id]["islPercent"]
                            switchPercent == expectedValuesMap["otherFlows"][flow.id][otherFlow.id]["switchPercent"]
                        }
                    }
                }
            }
        }
    }
}
