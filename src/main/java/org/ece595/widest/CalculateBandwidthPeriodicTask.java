package org.ece595.widest;

/**
 * Created by mininet on 4/25/17.
 */


import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CalculateBandwidthPeriodicTask extends TimerTask{
    //For every MK seconds, the controller needs to check if any node haven't sent the topology update
    private MaximizeThroughput maximizeThroughput;
    private HostService hostService;
    private DeviceService deviceService;
    private TopologyService topologyService;

    MultipathManager multipathManager;
    private LP lp;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private String temps;
    private String temps2;
    String pathhops = "";
    int count = 0;

    static int round = 0;


    public CalculateBandwidthPeriodicTask(HostService hostService, TopologyService topologyService, DeviceService deviceService, MultipathManager multipathManager, MaximizeThroughput maximizeThroughput) {
        this.hostService = hostService;
        this.topologyService = topologyService;
        this.deviceService = deviceService;
        this.maximizeThroughput = maximizeThroughput;
        this.multipathManager = multipathManager;
        this.lp = new LP();
    }

    public void run() {
        //synchronize
            synchronized (maximizeThroughput) {
                maximizeThroughput.fillCoefficientTable();
            }
            //test
            Set<Map.Entry<String,Integer>> x = AppComponent.edgeIndex.entrySet();
            temps = "";
            x.forEach(entry -> {
                String pair = entry.getKey();
                int index = entry.getValue();
                temps += pair + "_" + String.valueOf(AppComponent.edgeIndex.get(pair)) + ";";
            });
            //log.info("\n=========\nCalculate:edgeIndex:\n" + temps + "\n=========\n");

            //log.info("\n=======\ndemandsNum:\n" + String.valueOf(AppComponent.demandsNum) + "\n=======\n");

            String temp = "";
            for (int i = 0; i < AppComponent.hostsDemands.size() * AppComponent.consideredPathNum; i++) {
                for (int j = 0; j < AppComponent.pathLinkRelation[0].length; j++) {
                    temp += String.valueOf(AppComponent.pathLinkRelation[i][j]) + ",";
                }
                temp += "\n";
            }
            //log.info("\n=============\ntimer task: pathLinkRelation:\n" + temp + "\n=============\n");

            Set<Map.Entry<String,InitialDemand>> z = AppComponent.hostsDemands.entrySet();
            temps = "";
            z.forEach(entry -> {
                temps = "";
                String pair = entry.getKey();
                double demand = entry.getValue().demandbw;
                int groupNum = AppComponent.srcDstGroup.get(pair);
                double demandInArr = AppComponent.groupDemandArr[groupNum];
                temps += pair + "\n" + String.valueOf(demand) + "\n" + String.valueOf(groupNum) + "\n" + String.valueOf(demandInArr) + "\n";
                temps2 = "";
                if(AppComponent.groupDemandCoefficient.length > 0) {
                    temps2 += String.valueOf(groupNum) + "\n";
                    for (int j = 0; j < AppComponent.groupDemandCoefficient.length; j++) {
                        temps2 += String.valueOf(AppComponent.groupDemandCoefficient[j][groupNum]) + ",";
                    }
                }
                //log.info("\nQQQQQQQQQQ\npair, demand, groupNum, demand:\n" + temps + "\nQQQQQQQQQQ\n");
                //log.info("\nYYYYYYYYYY\ngroupNum, groupDemandCoefficient:\n" + temps2 + "\nYYYYYYYY\n");
            });

            String temp3 = "";
            for (int i = 0; i < AppComponent.demandsNum * AppComponent.consideredPathNum; i++) {
                List<TopologyEdge> route = AppComponent.VariableNumPath.get(i);
                temp3 += "path num " + String.valueOf(i) + "is:  ";
                if(route != null) {
                    for (int j = 0; j < route.size(); j++) {
                        String src = String.valueOf(Integer.parseInt(route.get(j).src().toString().substring(3)));
                        String dst = String.valueOf(Integer.parseInt(route.get(j).dst().toString().substring(3)));
                        temp3 += src + "," + dst + ";";
                    }
                }
                temp3 += "\n";
            }
            //log.info("\n%%%%%%%%%%\nVariableNumPath:\n" + temp3 + "\n%%%%%%%%%\n");


//            String allpaths = "";
//            for (int i = 0; i < AppComponent.demandsNum * AppComponent.consideredPathNum; i++) {
//                allpaths += "path num " + String.valueOf(i) + " is:  " + AppComponent.VariableNumPath.get(i) + "\n";
//            }
//            log.info("\n%%%%%%%%%%\nVariableNumPath:\n" + allpaths + "\n%%%%%%%%%\n");


            lp.calculate();

            //check if VariableNumBandwidth is assigned with correct value
//            String result = "Test in CalculateBandwidthPeriodicTask:\nRoute num and corresponding bandwidth are:\n";
//            for (int i = 0; i < AppComponent.demandsNum * AppComponent.consideredPathNum; i++) {
//                result += "RouteNum " + String.valueOf(i) + ": " + String.valueOf(AppComponent.VariableNumBandwidth.get(i)) + "\n";
//            }
//            log.info("\n~~~~~~~~~~\n" + result + "\n~~~~~~~~~~\n");


            //check the interface for further group table installation
            String important = "";
            for (int i = 0; i < AppComponent.finalResult.size(); i++) {
                important += "\n-----------------------\n";
                important += "demandNum is: " + String.valueOf(i) + "\n";
                DemandResult demandResult = AppComponent.finalResult.get(i);
                important += "demandRequested for this src dst is: " + String.valueOf(demandResult.demandRequested) + "\n";
                important += "demandSatisfied for this src dst is: " + String.valueOf(demandResult.demandSatisfied) + "\n";
                important += "srcMac is: " + demandResult.srcMac.toString() + "\n";
                important += "srcIpPrefix is: " + demandResult.srcIpPrefix.toString() + "\n";
                important += "dstMac is: " + demandResult.dstMac.toString() + "\n";
                important += "dstIpPrefix is: " + demandResult.dstIpPrefix + "\n";

                Iterator<PathResult> iterator = demandResult.pathResults.iterator();
                pathhops = "";
                count = 0;
                while (iterator.hasNext()) {
                    pathhops = "";
                    PathResult pathResult = iterator.next();
                    List<TopologyEdge> route = pathResult.route;
                    route.forEach(link -> {
                        int src = Integer.parseInt(link.src().deviceId().toString().substring(3));
                        int dst = Integer.parseInt(link.dst().deviceId().toString().substring(3));
                        pathhops += String.valueOf(src) + "," + String.valueOf(dst) + ";";
                    });
                    important += "\nroute" + count + " is: " + pathhops + "\n";
                    important += "bw assigned is: " + String.valueOf(pathResult.bw) + "\n";
                    count ++;
                }
                important += "\n-----------------------\n";
            }

            log.info("\n*****^^^^^^\n LinearProgramming Round {}:\n" + important + "\n*****^^^^^^^^^\n", round++);


            //reinitialize

            synchronized (maximizeThroughput) {
                maximizeThroughput.clearPublicVariables();
            }


            multipathManager.installPaths(AppComponent.finalResult);

        }

}



