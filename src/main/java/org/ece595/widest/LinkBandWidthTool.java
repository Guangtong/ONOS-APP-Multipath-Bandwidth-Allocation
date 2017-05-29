package org.ece595.widest;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyEdge;
import java.lang.Object;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostLocation;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.provider.ProviderId;

import org.onosproject.net.device.PortStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.net.Host;

import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.HostId;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.lang.String;
import java.io.PrintWriter;
import java.io.IOException;



/**
 * Created by sgt on 4/6/17.
 */

//We use Mbps as uinit

public class LinkBandWidthTool {
    private DeviceService deviceService;
    private HostService hostService;
    private TopologyService topologyService;
    private final Logger log = LoggerFactory.getLogger(getClass());
    public DeviceId switchid;
    public PortNumber hostportnum;
    public ConnectPoint connectPoint;
    public String hspd = "";
    public double hostdemand;
    public PortStatistics portStatistics;
    //redundent
    private final ProviderId routeProviderId = new ProviderId("ECE595", "SGT,FHF");
    public LinkBandWidthTool(DeviceService deviceService, HostService hostService, TopologyService topologyService) {
        this.deviceService = deviceService;
        this.hostService = hostService;
        this.topologyService = topologyService;
        //dynamicWidestRoute = new DynamicWidestRouting(this.hostService, this.topologyService, this.deviceService);
    }
    //private DynamicWidestRouting dynamicWidestRoute;



    public double getRemain(Link l) {

        if (l.state() == Link.State.INACTIVE) {
            return 0;
        }

        double remain = getCapacity(l) - getUsage(l);

//        log.info("\n ====Edge:" + l.toString() +
//                 "\n ====capacity:" + getCapacity(l) +
//                 "\n ====Usage:" + getUsage(l));


        if (remain < 0) {
            return 0;
        }else {
            return remain;
        }
    }

    public double getCapacity(Link l) {
        if (l.state() == Link.State.INACTIVE) {
            return 0;
        }
        DeviceId src = l.src().deviceId();
        DeviceId dst = l.dst().deviceId();
        int srci = Integer.parseInt(src.toString().substring(3));
        int dsti = Integer.parseInt(dst.toString().substring(3));

        //double srcPortBw = deviceService.getPort(l.src()).portSpeed() ;  //Mbps
        //double dstPortBw = deviceService.getPort(l.dst()).portSpeed() ;

        //return Math.min(srcPortBw, dstPortBw);

        //in our simulation, we read link bw from a file because ovsk in mininet always return 10Gbps BW

        Random r = new Random();

        //return 1 + 4 * r.nextDouble();   //r.nextDouble() return 0~1


        return AppComponent.edgeCapacity[srci][dsti];

    }

    public double getUsage(Link l) {

        if (l.state() == Link.State.INACTIVE) {
            return 0;
        }
        ConnectPoint src = l.src();
        ConnectPoint dst = l.dst();
        double srcBw = 0;
        double dstBw = 0;

        if(src.elementId() instanceof DeviceId) {
            PortStatistics stat = deviceService.getDeltaStatisticsForPort(src.deviceId(), src.port());

//            srcBw = stat.bytesSent() * 8.0e3 /stat.durationNano();  //bytes * 8bit/byte * 1e-6Mbit/bit / (nano * 1e-9s/nano) = bytes / nano * 8000 (Mbps)
//
//            log.info("\n ====Edge" + edge.link().toString() +
//                    "\n ====Stat:" + stat.toString() +
//                    "\n ====DurationNano:" + stat.durationNano());
            // durationNano is not correct: 0 or 999000000
            // durationSec is not accurate: 4 or 5

            //I decide to use 5 all the time

            srcBw = stat.bytesSent() * 8.0 / (5 * 1024 * 1024);

        }
        if(dst.elementId() instanceof DeviceId) {
            PortStatistics stat = deviceService.getDeltaStatisticsForPort(dst.deviceId(), dst.port());
            dstBw = stat.bytesReceived() * 8.0 / (5 * 1024 * 1024);
        }
        return Math.max(srcBw, dstBw);

    }
    public EdgeLink getEdgeLink(Host host, boolean isIngress) {
        return new DefaultEdgeLink(routeProviderId, new ConnectPoint(host.id(), PortNumber.portNumber(0)),
                                   host.location(), isIngress);
    }


//    public double getDemands() {
//        Iterable<Host> hosts = hostService.getHosts();// only get h3 and h5
//        hspd = "";// avoid accumulating
//        hosts.forEach(host -> {
//            EdgeLink srcLink = getEdgeLink(host, false);//if false, next line should be src
//            switchid = srcLink.src().deviceId();//get correct switch linked to the host, yes!
//            connectPoint = host.location();
//            hostportnum = connectPoint.port();//correct with the onos gui map port display, yes!
//            portStatistics = deviceService.getDeltaStatisticsForPort(switchid, hostportnum);//wrong number and can not distinguish in and out traffic
//            hostdemand = portStatistics.bytesReceived() * 8.0 / (5 * 1024 * 1024);//during transfer, 0; at end, correct but if time too short measurement error larger
//            hspd += host.id().toString() + "\n" + switchid.toString() + "\n" + hostportnum.toString() + "\n" + String.valueOf(hostdemand) + ";\n";
//        });
//        log.info("\n==========\nall hosts,corresponding switch,portnum,hostdemand:\n" + hspd + "\n============\n");
//        return 0;//needs change
//    }

    public double getDemand(HostId srcHostId, HostId dstHostId, MacAddress srcMac, MacAddress dstMac, IpPrefix srcIpPrefix, IpPrefix dstIpPrefix, Hashtable<String,Double> hostsDemands) {
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
        //put the <src,dst,demand> into public hostsDemands hashtable
        String srcDst = srcSwitchId.toString() + "," + dstSwitchId.toString();
        hostsDemands.put(srcDst,hostdemand);
        return hostdemand;
    }
}
