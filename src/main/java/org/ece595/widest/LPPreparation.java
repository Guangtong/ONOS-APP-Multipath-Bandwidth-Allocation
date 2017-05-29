package org.ece595.widest;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyService;


import javax.validation.constraints.Null;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Created by mininet on 4/17/17.
 */
public class LPPreparation {
    public static int i = 0;
    public static String pathhops;
    String cor = "";
    private HostService hostService;
    private DeviceService deviceService;
    private TopologyService topologyService;
    private Path consideredPath;
    private int numEdges;
    private List<Link> linksInPath;
    private Topology topo;
    private TopologyGraph graph;
    private Set<TopologyEdge> alledges;
    private Hashtable<Integer, String> indexEdge;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private String temps = "";





    public LPPreparation(HostService hostService, TopologyService topologyService, DeviceService deviceService) {
        this.hostService = hostService;
        this.topologyService = topologyService;
        this.deviceService = deviceService;
        this.consideredPath = consideredPath;
        topo = topologyService.currentTopology();
        graph = topologyService.getGraph(topo);
        alledges = graph.getEdges();
        numEdges = alledges.size();
        //edgeIndex = new Hashtable<>();
        indexEdge = new Hashtable<>();
    }

//    public int[] getPathLinkRelation() {
//        String pathlinkrelationstr = "";
//        String edgeIndexstr = "";
//        pathhops = "";
//        edgeIndex = getEdgeIndex();
//        linksInPath = consideredPath.links();
//        linksInPath.forEach(link -> {
//            int src = Integer.parseInt(link.src().deviceId().toString().substring(3));
//            int dst = Integer.parseInt(link.dst().deviceId().toString().substring(3));
//            pathhops += String.valueOf(src) + "," + String.valueOf(dst) + ";";
//            String pair = String.valueOf(src) + "," + String.valueOf(dst);
//            if (edgeIndex.containsKey(pair)) {
//
//                pathLinkRelation[edgeIndex.get(pair)] = 1;
//            }
//        });
//        //log.info("\n===============\nconsidered path is:\n" + pathhops + "\n===============\n");
//        for (int j = 0; j < pathLinkRelation.length; j++) {
//            pathlinkrelationstr += String.valueOf(pathLinkRelation[j]);
//            pathlinkrelationstr += ",";
//        }
//
//
//        for (int j = 0; j < edgeIndex.size(); j++) {
//            pathlinkrelationstr += String.valueOf(pathLinkRelation[j]) + ",";
//        }
//
//
//
//        //new
//        Set<Map.Entry<String,Integer>> x = edgeIndex.entrySet();
//        x.forEach(entry -> {
//            String pair = entry.getKey();
//            int index = entry.getValue();
//            int rel = pathLinkRelation[index];
//            cor += pair + "_" + String.valueOf(index) + "_" + String.valueOf(rel) + ";" + "\n";
//        });
//        //log.info("\n==========\ncorresponding relaionship is:\n" + cor + "\n===========\n");
//
//        return pathLinkRelation;
//    }



    public int getRouteEdgeRelation(List<TopologyEdge> route, int rowNum) {

        Set<Map.Entry<String,Integer>> xx = AppComponent.edgeIndex.entrySet();
        temps = "";
        xx.forEach(entry -> {
            String pair = entry.getKey();
            int index = entry.getValue();
            temps += pair + "_" + String.valueOf(AppComponent.edgeIndex.get(pair)) + ";";
        });
        //log.info("\n=========\nLPPreparation: edgeIndex:\n" + temps + "\n=========\n");



        String pathlinkrelationstr = "";
        String edgeIndexstr = "";
        pathhops = "";
        route.forEach(link -> {
            int src = Integer.parseInt(link.src().deviceId().toString().substring(3));
            int dst = Integer.parseInt(link.dst().deviceId().toString().substring(3));
            pathhops += String.valueOf(src) + "," + String.valueOf(dst) + ";";
            String pair = String.valueOf(src) + "," + String.valueOf(dst);
            if (AppComponent.edgeIndex.containsKey(pair)) {
                //log.info("\n########\npathLinkRelation: rowNum, index, srcDst, edgecapacity:\n" + String.valueOf(rowNum) + "\n" + String.valueOf(AppComponent.edgeIndex.get(pair) + "\n" + pair + "\n" + String.valueOf(AppComponent.edgeCapacityArray[AppComponent.edgeIndex.get(pair)])) + "\n");
                AppComponent.pathLinkRelation[rowNum][AppComponent.edgeIndex.get(pair)] = 1;
            }
        });
        //log.info("\n$$$$$$$$$$$$$$\nconsidered path is:\n" + pathhops + "\n$$$$$$$$$$$$$$\n");
        for (int j = 0; j < AppComponent.pathLinkRelation[0].length; j++) {
            pathlinkrelationstr += String.valueOf(AppComponent.pathLinkRelation[rowNum][j]);
            pathlinkrelationstr += ",";
        }

        for (int j = 0; j < AppComponent.edgeIndex.size(); j++) {
            pathlinkrelationstr += String.valueOf(AppComponent.pathLinkRelation[rowNum][j]) + ",";
        }

        //new
        Set<Map.Entry<String,Integer>> x = AppComponent.edgeIndex.entrySet();
        x.forEach(entry -> {
            String pair = entry.getKey();
            int index = entry.getValue();
            int rel = AppComponent.pathLinkRelation[rowNum][index];
            cor += pair + "_" + String.valueOf(index) + "_" + String.valueOf(rel) + ";" + "\n";
        });
        //log.info("\n==========\ncorresponding relaionship is:\n" + cor + "\n===========\n");

        return 0;
    }

    public int paddingZeroes(int rowNum) {
        for (int j = 0; j < AppComponent.pathLinkRelation[0].length; j++) {
            AppComponent.pathLinkRelation[rowNum][j] = 0;
        }
        return 0;
    }

    public void fillCapacityRow() {
        Set<Map.Entry<String,Integer>> x = AppComponent.edgeIndex.entrySet();
        x.forEach(entry -> {
            String pair = entry.getKey();
            int index = entry.getValue();
            String[] srcDst = pair.split(",");
            if(srcDst.length >= 2) {
                int src = Integer.parseInt(srcDst[0]);
                int dst = Integer.parseInt(srcDst[1]);
                AppComponent.edgeCapacityArray[index] = AppComponent.edgeCapacity[src][dst];
            }else {
                log.info("\n=========\nsrcDst string array length < 2\n=========\n");
            }
        });
    }



    public int getEdgeIndex(Hashtable<String,Integer> edgeIndex) {
        //index starts from 0
        i = 0;
        if(alledges == null) {
            log.warn("\n=========\nalledges null\n=========\n");
        }
        if(alledges.size() == 0) {
            log.warn("\n=========\nalledges contains no edge\n==========\n");
        }
        alledges.forEach(edge -> {
            int src = Integer.parseInt(edge.src().deviceId().toString().substring(3));
            int dst = Integer.parseInt(edge.dst().deviceId().toString().substring(3));
            String pair = String.valueOf(src) + "," + String.valueOf(dst);
            edgeIndex.put(pair, i);
            i ++;
        });
        return 0;
    }


//    public Hashtable<Integer,String> getInvertIndexEdge() {
//        i = 0;
//        if(alledges == null) {
//            log.warn("\n=========\nalledges null\n=========\n");
//        }
//        if(alledges.size() == 0) {
//            log.warn("\n=========\nalledges contains no edge\n==========\n");
//        }
//        alledges.forEach(edge -> {
//            int src = Integer.parseInt(edge.src().deviceId().toString().substring(3));
//            int dst = Integer.parseInt(edge.dst().deviceId().toString().substring(3));
//            String pair = String.valueOf(src) + "," + String.valueOf(dst);
//            indexEdge.put(i, pair);
//
//            i ++;
//        });
//        return indexEdge;
//    }
}