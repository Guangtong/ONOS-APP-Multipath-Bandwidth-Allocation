package org.ece595.widest;

import com.google.common.collect.ImmutableList;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.HTMLDocument;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.Object;
import org.onosproject.net.ElementId;
import org.onosproject.net.DeviceId;
import javax.validation.constraints.Null;


/**
 * Created by mininet on 4/23/17.
 */
public class MaximizeThroughput {
    //every call of this class will change and record one src dst demand pair

    private HostService hostService;
    private DeviceService deviceService;
    private TopologyService topologyService;
    private LinkBandWidthTool linkBandWidth;
    private DynamicWidestRouting pathRouting;
    private LPPreparation lpPreparation;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private int groupNum = 0;
    private int rowNum = 0;
    private int pathNumInGroup = 0;
    private Topology topo;
    private String pathhops;



    public MaximizeThroughput(HostService hostService, TopologyService topologyService, DeviceService deviceService) {
        this.hostService = hostService;
        this.topologyService = topologyService;
        this.deviceService = deviceService;
        linkBandWidth = new LinkBandWidthTool(this.deviceService,this.hostService,this.topologyService);
        pathRouting = new DynamicWidestRouting(this.hostService, this.topologyService, this.deviceService);
        lpPreparation = new LPPreparation(this.hostService, this.topologyService, this.deviceService);
        topo = topologyService.currentTopology();
    }

    //EdgeLink srcLink = linkBandWidth.getEdgeLink(hostService.getHost(srcHostId), true);
    //EdgeLink dstLink = linkBandWidth.getEdgeLink(hostService.getHost(dstHostId), false);

    public void fillCoefficientTable() {
        //after this method is called, the pathLinkRelation matrix is filled; demandGroupCor is filled
        groupNum = 0;//necessary
        Set<Map.Entry<String,InitialDemand>> x = AppComponent.hostsDemands.entrySet();
        AppComponent.groupDemandCoefficient = new int[AppComponent.consideredPathNum][x.size()];
        AppComponent.groupDemandArr = new double[x.size()];
        AppComponent.srcDstGroup = new Hashtable<>();
        AppComponent.finalResult = new HashMap<>();
        AppComponent.VariableNumPath = new HashMap<>();
        x.forEach(entry -> {
            double demand = entry.getValue().demandbw;
            if(demand > 0.02) {
                String pair = entry.getKey();
                AppComponent.srcDstGroup.put(pair, groupNum);
                DemandResult demandResult = new DemandResult(entry.getValue().srcMac, entry.getValue().srcIpPrefix, entry.getValue().dstMac, entry.getValue().dstIpPrefix, null,0, entry.getValue().demandbw);
                AppComponent.finalResult.put(groupNum, demandResult);
                String[] srcDst = pair.split(",");
                String srcStr = srcDst[0];
                String dstStr = srcDst[1];
                DeviceId src = DeviceId.deviceId(srcStr);
                DeviceId dst = DeviceId.deviceId(dstStr);
                Set<List<TopologyEdge>> allRoutes = pathRouting.findAllRoutes(topo, src, dst);
                Set<List<TopologyEdge>> randomRoutes = getRandomRoutes(allRoutes, AppComponent.consideredPathNum);


                Iterator<List<TopologyEdge>> listIterator = randomRoutes.iterator();
                pathNumInGroup = 0;
                while (listIterator.hasNext()) {
                    //.next will move the pointer, so can not call .next twice in one round of the loops
                    List<TopologyEdge> routeTemp = listIterator.next();
                    lpPreparation.getRouteEdgeRelation(routeTemp, groupNum * AppComponent.consideredPathNum + pathNumInGroup);
                    AppComponent.groupDemandCoefficient[pathNumInGroup][groupNum] = 1;
                    AppComponent.VariableNumPath.put(groupNum * AppComponent.consideredPathNum + pathNumInGroup, routeTemp);

//                pathhops = "";
//                routeTemp.forEach(link -> {
//                    int srcEdge = Integer.parseInt(link.src().deviceId().toString().substring(3));
//                    int dstEdge = Integer.parseInt(link.dst().deviceId().toString().substring(3));
//                    pathhops += String.valueOf(srcEdge) + "," + String.valueOf(dstEdge) + ";";
//                });
//                AppComponent.VariableNumPath.put(groupNum * AppComponent.consideredPathNum + pathNumInGroup, pathhops);


                    pathNumInGroup ++;
                }
                if(pathNumInGroup < AppComponent.consideredPathNum) {
                    //not enough paths for this src dst pair, need to fill the extra rows with zeroes
                    while (pathNumInGroup < AppComponent.consideredPathNum) {
                        lpPreparation.paddingZeroes(groupNum * AppComponent.consideredPathNum + pathNumInGroup);
                        AppComponent.groupDemandCoefficient[pathNumInGroup][groupNum] = 0;
                        AppComponent.VariableNumPath.put(groupNum * AppComponent.consideredPathNum + pathNumInGroup, null);


//                    pathhops = "";
//                    AppComponent.VariableNumPath.put(groupNum * AppComponent.consideredPathNum + pathNumInGroup, pathhops);


                        pathNumInGroup ++;
                    }
                }
                //fill in groupDemandArr
                AppComponent.groupDemandArr[groupNum] = demand;
                groupNum ++;
            }
        });
        AppComponent.demandsNum = x.size();
    }



    private Set<List<TopologyEdge>> getRandomRoutes(Set<List<TopologyEdge>> allRoutes, int k) {
        // randomly get k or less routes from all routes
        int num = allRoutes.size();
        if(k >= num) {
            return allRoutes;
        }else {
            Set<List<TopologyEdge>> randomRoutes = new HashSet<>();
            int count = 0;
            Iterator<List<TopologyEdge>> iterator = allRoutes.iterator();
            while(iterator.hasNext() && count < k) {
                randomRoutes.add(iterator.next());
                count ++;
            }
            return randomRoutes;
        }
    }

    private final ProviderId routeProviderId = new ProviderId("ECE595", "SGT,FHF");

    public EdgeLink getEdgeLink(Host host, boolean isIngress) {
        return new DefaultEdgeLink(routeProviderId, new ConnectPoint(host.id(), PortNumber.portNumber(0)),
                                   host.location(), isIngress);
    }


    public double getDemand(HostId srcHostId, HostId dstHostId, MacAddress srcMac, MacAddress dstMac, IpPrefix srcIpPrefix, IpPrefix dstIpPrefix, Hashtable<String, InitialDemand> hostsDemands) {
        //get src and dst switch id
        Host srcHost = hostService.getHost(srcHostId);
        Host dstHost = hostService.getHost(dstHostId);
        EdgeLink srcLink = getEdgeLink(srcHost, false);//if false, next line should be src
        EdgeLink dstLink = getEdgeLink(dstHost, false);
        DeviceId srcSwitchId = srcLink.src().deviceId();//get correct switch linked to the host, yes!
        DeviceId dstSwitchId = dstLink.src().deviceId();
        //get src host demand
        ConnectPoint connectPoint = srcHost.location();
        PortNumber hostportnum = connectPoint.port();//correct with the onos gui map port display, yes!
        PortStatistics portStatistics = deviceService.getDeltaStatisticsForPort(srcSwitchId, hostportnum);//wrong number and can not distinguish in and out traffic
        double hostdemand = portStatistics.bytesReceived() * 8.0 / (5 * 1024 * 1024);//during transfer, 0; at end, correct but if time too short measurement error larger
//        if(hostdemand < 0.02) {
//            return hostdemand;
//        }


        //put the <src,dst,demand> into public hostsDemands hashtable
        String srcDst = srcSwitchId.toString() + "," + dstSwitchId.toString();
        InitialDemand initialDemand = new InitialDemand(hostdemand, srcMac, srcIpPrefix, dstMac, dstIpPrefix);
        hostsDemands.put(srcDst,initialDemand);
        return hostdemand;
    }

    public void clearPublicVariables() {
        AppComponent.hostsDemands.clear();
        AppComponent.pathLinkRelation = new int[100][AppComponent.edgeIndex.size()];
    }



}
