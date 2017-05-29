package org.ece595.widest;

import org.onosproject.net.topology.TopologyEdge;

import java.util.List;

/**
 * Created by mininet on 4/28/17.
 */
public class PathResult {

    public List<TopologyEdge> route;
    public double bw;

    public PathResult(List<TopologyEdge> route, double bw) {
        this.route = route;
        this.bw = bw;
    }

}
