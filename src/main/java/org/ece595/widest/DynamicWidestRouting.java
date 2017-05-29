package org.ece595.widest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.onosproject.common.DefaultTopology;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.ElementId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.lang.String;
import java.io.PrintWriter;
import java.io.IOException;



public class DynamicWidestRouting {

    private HostService hostService;
    private DeviceService deviceService;
    private TopologyService topologyService;
    private boolean isDynamic;


    public DynamicWidestRouting(HostService hostService, TopologyService topologyService, DeviceService deviceService) {
        this.hostService = hostService;
        this.topologyService = topologyService;
        this.deviceService = deviceService;
        linkBandWidth = new LinkBandWidthTool(deviceService,hostService,topologyService);
        isDynamic = true;
    }

    public void setStatic(){
        isDynamic = false;
    }
    public void setDynamic(){
        isDynamic = true;
    }


    private final ProviderId routeProviderId = new ProviderId("ECE595", "SGT,FHF");
    private final Logger log = LoggerFactory.getLogger(getClass());

    private LinkBandWidthTool linkBandWidth;


    /**
     * Generate EdgeLink which is between Host and Device.
     *
     * @param host
     * @param isIngress whether it is Ingress to Device or not.
     * @return
     */
    private EdgeLink getEdgeLink(Host host, boolean isIngress) {
        return new DefaultEdgeLink(routeProviderId, new ConnectPoint(host.id(), PortNumber.portNumber(0)),
                                   host.location(), isIngress);
    }

    /**
     * find all route between Src and Dst.
     * Use "route" to mean a list of edges, Path is a route with cost
     *
     */
    public Set<List<TopologyEdge>> findAllRoutes(Topology topo, DeviceId src, DeviceId dst) {
        if (!(topo instanceof DefaultTopology)) {
            log.error("topology is not the object of DefaultTopology.");
            return ImmutableSet.of(); //return empty set
        }

        Set<List<TopologyEdge>> graghResult = new HashSet<>();  //a set of path (all routes)
        dfsFindAllRoutes(new DefaultTopologyVertex(src), new DefaultTopologyVertex(dst),
                         new ArrayList<>(), new ArrayList<>(),
                         ((DefaultTopology) topo).getGraph(), graghResult);

        return graghResult;
    }

    /**
     * Get all possible path between Src and Dst using DFS
     */

    private void dfsFindAllRoutes(TopologyVertex src,
                                  TopologyVertex dst,
                                  List<TopologyEdge> passedLink,
                                  List<TopologyVertex> passedDevice,
                                  TopologyGraph topoGraph,
                                  Set<List<TopologyEdge>> result) {
        if (src.equals(dst)) {
            return;
        }

        passedDevice.add(src);

        Set<TopologyEdge> egressSrc = topoGraph.getEdgesFrom(src);
        egressSrc.forEach(egress -> {
            TopologyVertex vertexDst = egress.dst();
            if (vertexDst.equals(dst)) {
                //Gain a Path
                passedLink.add(egress);
                result.add(ImmutableList.copyOf(passedLink.iterator()));
                passedLink.remove(egress);

            } else if (!passedDevice.contains(vertexDst)) {
                //DFS into
                passedLink.add(egress);
                dfsFindAllRoutes(vertexDst, dst, passedLink, passedDevice, topoGraph, result);
                passedLink.remove(egress);

            } else {
                //means - passedDevice.contains(vertexDst)
                //We hit a loop, NOT go into
            }
        });

        passedDevice.remove(src);
    }



}
