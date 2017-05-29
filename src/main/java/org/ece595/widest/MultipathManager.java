package org.ece595.widest;

import javafx.util.Pair;
import org.onlab.graph.Edge;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.util.DataRateUnit;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by mininet on 4/29/17.
 */
public class MultipathManager {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private DeviceService deviceService;
    private GroupService groupService;
    private FlowRuleService flowRuleService;
    private MeterService meterService;
    private HostService hostService;
    private ApplicationId appId;
    private HashMap<DeviceId, DeviceRuleStore> ruleStore;


    private static int groupId = 0;


    public MultipathManager(DeviceService deviceService, GroupService groupService, FlowRuleService flowRuleService, MeterService meterService, HostService hostService, ApplicationId appId) {
        this.deviceService = deviceService;
        this.groupService = groupService;
        this.flowRuleService = flowRuleService;
        this.appId = appId;
        this.meterService = meterService;
        this.hostService = hostService;
        this.ruleStore = new HashMap<>();
    }

    public void installPaths(Map<Integer, DemandResult> demandsAllocation) {


        //=======for each demand:

        int size = demandsAllocation.size();
        for (int i = 0; i < size; i++) {
            DemandResult dr = demandsAllocation.get(i);

            //===========for each device, create a group table for the demand

            Iterator<Device> devices = deviceService.getDevices().iterator();
            while (devices.hasNext()) {

                Device device = devices.next();
                groupId++; //every demand on every device has a group table
                HashSet<Pair<MeterId, MeterRequest>> meterSet = new HashSet<>(); // set of meters, for this demand, this device, used for buckets


                DeviceRuleStore deviceRuleStore;
                if (ruleStore.containsKey(device.id())) {
                    deviceRuleStore = ruleStore.get(device.id());
                } else {
                    deviceRuleStore = new DeviceRuleStore(device.id());

                    ruleStore.put(device.id(), deviceRuleStore);
                }

                List<GroupBucket> buckets = new ArrayList<>();
//                log.info("\ndevice: " + device.id().toString());


                //============for each path, fill one bucket of the group table on this device

                Iterator<PathResult> pathResults = dr.pathResults.iterator();

                while (pathResults.hasNext()) {

                    PathResult pr = pathResults.next();
                    short weight = (short) (pr.bw * 1000);
                    //short weight = (short) (pr.bw / dr.demandSatisfied * 10000);
                    if(weight < 2) {
                        continue;
                    }

                    List<TopologyEdge> edges = pr.route;


                    //find a link starting from this device

                    for (int j = 0; j < edges.size(); j++) {
                        Link l = edges.get(j).link();

//                        log.info("\nTest Link: " + l.src().deviceId() + " to " + l.dst().deviceId());




                        if (l.src().deviceId().equals(device.id())) {
//                            log.info("\nfind a link starting from this device");

                            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

                            tBuilder.setOutput(l.src().port());

//                            Band band = DefaultBand.builder()
//                                    .ofType(Band.Type.DROP)
//                                    .withRate((long)(pr.bw * 1000))         //Mbps to kbps
//                                    .build();

//                            MeterRequest.Builder mrb = DefaultMeterRequest.builder();
//                            MeterRequest mr = mrb.forDevice(device.id())
//                                    .fromApp(appId)
//                                    .withBands(Collections.singleton(band))
//                                    .withUnit(Meter.Unit.KB_PER_SEC)
//                                    .add();
//
//                            Meter m = meterService.submit(mr);
//                            //tBuilder.meter(m.id());
//                            meterSet.add(new Pair<>(m.id(), mr));

                            GroupBucket bucket = DefaultGroupBucket.createSelectGroupBucket(tBuilder.build(), weight);
                            buckets.add(bucket);
                            break;
                        }


//                        if this is the last link, we also need to check whether the device is the ending device
//                        otherwise, we only need to check whether the device is the starting device of the link
//                        if(j == edges.size()-1 && l.dst().deviceId().equals(device.id())) {
//                            //get the dst host
//
//                            HostId dstHostId = HostId.hostId(dr.dstMac);
//                            Host dstHost = hostService.getHost(dstHostId);
//
//                            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
//                            tBuilder.setOutput(dstHost.location().port());
//                            GroupBucket bucket = DefaultGroupBucket.createSelectGroupBucket(tBuilder.build(), weight);
//                            buckets.add(bucket);
//
//                        }

                    }
                }

                //There will be many devices not part of the demand, but needs to be checked. Not efficient, can be improved
                if (buckets.isEmpty()) {
                    continue; //no flow for this device
                }


                //===========Use the buckets to create group table for the device for this flow
                //1. Define the flow selector
                TrafficSelector selector = DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPSrc(dr.srcIpPrefix)
                        .matchIPDst(dr.dstIpPrefix)
                        .build();


                Set<Criterion> criteria = selector.criteria();

                //2. Remove old group and flow rule
                if (deviceRuleStore.groupKeyStore.containsKey(criteria)) {
                    GroupKey lastKey = deviceRuleStore.groupKeyStore.get(criteria);
                    GroupBuckets lastGroupBuckets = groupService.getGroup(device.id(), lastKey).buckets();
//                    if (lastGroupBuckets.equals(new GroupBuckets(buckets))) {
//                        continue; //no change to this group and its rule, to next device
//                    }
                    //otherwise, remove the group
//                    log.info("\nRemoving Group {}", groupService.getGroup(device.id(), lastKey).id().toString() );
                    groupService.removeGroup(device.id(), lastKey, appId);//async, so not effective immediately

                    flowRuleService.removeFlowRules(deviceRuleStore.flowRuleStore.get(criteria));
//                    deviceRuleStore.meterSetStore.get(criteria).forEach(meterIdMeterRequestPair -> {
//                        meterService.withdraw(meterIdMeterRequestPair.getValue(),meterIdMeterRequestPair.getKey());
//                    });

                }

                //deviceRuleStore.meterSetStore.put(criteria,meterSet);
                //3. generate a key with the new groupId, since lastKey is pending-remove

                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                buffer.putInt(groupId);
                GroupKey newKey = new DefaultGroupKey(buffer.array());
                deviceRuleStore.groupKeyStore.put(criteria, newKey);

                //4. Install group table
                GroupDescription groupDescription = new DefaultGroupDescription(device.id(),
                                                                                GroupDescription.Type.SELECT,
                                                                                new GroupBuckets(buckets),
                                                                                newKey,
                                                                                groupId,
                                                                                appId);
                groupService.addGroup(groupDescription);
//                log.info("\nInstalling Group {}", groupId );

                //5. generate flow rule
                TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                        .group(new GroupId(groupId))
                        .build();


                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(device.id())
                        .withSelector(selector)
                        .withTreatment(treatment)
                        .withPriority(55555)
                        .fromApp(appId)
                        .makePermanent()
                        .build();

                deviceRuleStore.flowRuleStore.put(criteria, rule);
                flowRuleService.applyFlowRules(rule);


            }

        }
    }

    class DeviceRuleStore {

        protected HashMap<Set<Criterion>, GroupKey> groupKeyStore;
        protected HashMap<Set<Criterion>, FlowRule> flowRuleStore;
        //protected HashMap<Set<Criterion>, Set<Pair<MeterId, MeterRequest>> > meterSetStore;

        DeviceId id;

        public DeviceRuleStore(DeviceId id) {
            this.groupKeyStore = new HashMap<>();
            this.flowRuleStore = new HashMap<>();
            //this.meterSetStore = new HashMap<>();
            this.id = id;
        }
    }

}
