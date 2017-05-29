package org.ece595.widest;

import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by mininet on 4/28/17.
 */
public class DemandResult {

    public MacAddress srcMac;
    public IpPrefix srcIpPrefix;
    public MacAddress dstMac;
    public IpPrefix dstIpPrefix;
    public Set<PathResult> pathResults;
    public double demandSatisfied;
    public double demandRequested;

    public DemandResult(MacAddress srcMac, IpPrefix srcIpPrefix, MacAddress dstMac, IpPrefix dstIpPrefix, Set<PathResult> pathResults, double demandSatisfied, double demandRequested) {
        this.srcMac = srcMac;
        this.srcIpPrefix = srcIpPrefix;
        this.dstMac = dstMac;
        this.dstIpPrefix = dstIpPrefix;
        this.pathResults = pathResults;
        this.demandSatisfied = demandSatisfied;
        this.demandRequested = demandRequested;
    }
}
