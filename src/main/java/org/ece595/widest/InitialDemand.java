package org.ece595.widest;

import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

/**
 * Created by mininet on 4/29/17.
 */
public class InitialDemand {
    public double demandbw;// demand requested
    public MacAddress srcMac;
    public IpPrefix srcIpPrefix;
    public MacAddress dstMac;
    public IpPrefix dstIpPrefix;

    public InitialDemand(double demandbw, MacAddress srcMac, IpPrefix srcIpPrefix, MacAddress dstMac, IpPrefix dstIpPrefix) {
        this.demandbw = demandbw;
        this.srcMac = srcMac;
        this.srcIpPrefix = srcIpPrefix;
        this.dstMac = dstMac;
        this.dstIpPrefix = dstIpPrefix;
    }
}
