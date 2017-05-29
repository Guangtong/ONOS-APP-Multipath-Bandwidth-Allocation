/*
 * Copyright 2017-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ece595.widest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Timer;



@Component(immediate = true)
public class AppComponent {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterService meterService;



    private final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationId appId;
    private ConcurrentMap<Set<Criterion>, Intent> intentMap ;
    private WidestpathPacketProcessor packetProcessor;
    private DynamicWidestRouting widestPathRouting;
    private LPPreparation lpPreparation;
    private MaximizeThroughput maximizeThroughput;

    private Timer timer = new Timer("CalculateBandwidth Periodic Task");

    //establish a matrix that represents the bandwidth of each edge
    //below six will not be repeatly assigned different value
    public static int matrixLen; // support one less vertexes, assigned value in activate
    public static double[][] edgeCapacity;// assigned valure in activate
    public static Hashtable<String,Integer> edgeIndex = new Hashtable<>();
    public static double[] edgeCapacityArray;//activate
    public static int consideredPathNum = 5;
    public static int demandsNum = 0;
    public static double throughput = 0;
    public static double largestCapacity = 1000.0;//have to be large enough, otherwise affect lp solution
    // below three are initialized every time before use
    public static int[][] groupDemandCoefficient;
    public static double[] groupDemandArr;
    public static Hashtable<String,Integer> srcDstGroup;//demand number
    public static HashMap<Integer, List<TopologyEdge>> VariableNumPath;
    public static HashMap<Integer, Double> VariableNumBandwidth;
    public static HashMap<Integer, DemandResult> finalResult;
    //below two are cleared after use
    public static Hashtable<String,InitialDemand> hostsDemands;// assigned value in activate
    public static int[][] pathLinkRelation;//new created in activate


    private String temps;


    @Activate
    protected void activate() {

        appId = coreService.registerApplication("org.ece595.widest");
        intentMap = new ConcurrentHashMap<>();


        widestPathRouting = new DynamicWidestRouting(hostService, topologyService, deviceService);
        //widestPathRouting.setStatic();

        lpPreparation = new LPPreparation(hostService, topologyService, deviceService);

        maximizeThroughput = new MaximizeThroughput(hostService, topologyService, deviceService);

        packetProcessor = new WidestpathPacketProcessor(appId, widestPathRouting,intentService, deviceService, hostService, topologyService, intentMap, maximizeThroughput);

        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));

        packetService.requestPackets(DefaultTrafficSelector.builder().matchEthType(Ethernet.TYPE_IPV4).build(),
                                     PacketPriority.REACTIVE, appId);

        try {
            final String fileName = "/home/mininet/widest-path-app-LP/topo_exam.txt";
            ReadEdgeParameter readedgetool = new ReadEdgeParameter(fileName);
            readedgetool.readBw();
        } catch (Exception e1) {
            log.warn("\n***************\nCan not read edge parameters from ReadEdgeParameter!\n**************\n");
            return;
        }
        //If done this during activation, no changes of topology allowed(like link failure)
        if(lpPreparation.getEdgeIndex(edgeIndex) != 0) {
            log.warn("\n=======\nERROR getting edgeIndex hashtable\n========\n");
        }

        int temp = topologyService.getGraph(topologyService.currentTopology()).getEdges().size();
        pathLinkRelation = new int[100][temp];//column are all edges, rows are all considered paths for current demands
        edgeCapacityArray = new double[temp];
        lpPreparation.fillCapacityRow();//only once
        hostsDemands = new Hashtable<>();



        MultipathManager multipathManager = new MultipathManager(deviceService,groupService,flowRuleService, meterService, hostService, appId);

        long period = (long)10 * 1000;
        timer.schedule(new CalculateBandwidthPeriodicTask(hostService, topologyService, deviceService, multipathManager, maximizeThroughput), period, period);  //public void schedule(TimerTask task, long delayms, long periodms)
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        timer.cancel();
        timer.purge();
        packetService.removeProcessor(packetProcessor);
        packetService.cancelPackets(DefaultTrafficSelector.builder()
                                            .matchEthType(Ethernet.TYPE_IPV4).build(),
                                    PacketPriority.REACTIVE, appId);

        try {
            Thread.sleep(5000);
            groupService.purgeGroupEntries();
            flowRuleService.removeFlowRulesById(appId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        log.info("Stopped");

    }
}


