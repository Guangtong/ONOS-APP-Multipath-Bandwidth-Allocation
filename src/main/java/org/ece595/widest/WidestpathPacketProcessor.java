package org.ece595.widest;

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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;



public class WidestpathPacketProcessor implements PacketProcessor {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DeviceService deviceService;
    private final HostService hostService;
    private final TopologyService topologyService;
    private DynamicWidestRouting widestPathRouting; //strategy pattern
    private ApplicationId appId;
    private ConcurrentMap<Set<Criterion>, Intent> intentMap;
    private IntentService intentService;
    private MaximizeThroughput maximizeThroughput;
    static  int count = 0;

    public WidestpathPacketProcessor(ApplicationId appId,
                                     DynamicWidestRouting routing,
                                     IntentService intentService,
                                     DeviceService deviceService, HostService hostService, TopologyService topologyService,
                                     ConcurrentMap<Set<Criterion>, Intent> intentMap, MaximizeThroughput maximizeThroughput) {
        this.widestPathRouting = routing;
        this.appId = appId;
        this.intentMap = intentMap;
        this.intentService = intentService;
        this.deviceService = deviceService;
        this.topologyService = topologyService;
        this.hostService = hostService;
        this.maximizeThroughput = maximizeThroughput;
    }

    @Override
    public void process(PacketContext context) {
        //true if this packet is already sent or blocked
        if (context.isHandled()) {
            return;
        }

        Ethernet pkt = context.inPacket().parsed();
        if (pkt.getEtherType() == Ethernet.TYPE_IPV4) {
            HostId srcHostId = HostId.hostId(pkt.getSourceMAC());  //HostId is VlanID+MAC
            HostId dstHostId = HostId.hostId(pkt.getDestinationMAC());
            MacAddress srcMac = pkt.getSourceMAC();
            MacAddress dstMac = pkt.getDestinationMAC();
            IPv4 ipPkt = (IPv4) pkt.getPayload();  //peel off MAC info, get ip layer packet
            IpPrefix srcIpPrefix = IpPrefix.valueOf(ipPkt.getSourceAddress(), 32);
            IpPrefix dstIpPrefix = IpPrefix.valueOf(ipPkt.getDestinationAddress(), 32);


            //log.info("\n\n\n\n #{} - Got Demand: {} to {} \n\n\n\n", count++, srcHostId, dstHostId);

            //accumulate hosts demands over a period of time
            synchronized (maximizeThroughput) {
                maximizeThroughput.getDemand(srcHostId, dstHostId, srcMac, dstMac, srcIpPrefix, dstIpPrefix, AppComponent.hostsDemands);
            }
//            String temp = "";
//            for (int i = 0; i < AppComponent.consideredPathNum; i++) {
//                for (int j = 0; j < AppComponent.pathLinkRelation[0].length; j++) {
//                    temp += String.valueOf(AppComponent.pathLinkRelation[i][j]) + ",";
//                }
//                temp += "\n";
//            }
//            log.info("\n=============\npathLinkRelation:\n" + temp + "\n=============\n");
        }
    }
}
