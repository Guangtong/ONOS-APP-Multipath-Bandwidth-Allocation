# ONOS-APP-Multipath-Bandwidth-Allocation

Proposed an Multipath Traffic Engineering strategy for Interdomain Routing and validated it using ONOS SDN Controller.

1. Measure traffic demands (src, dst, bw) from ingress edge routers. 
2. Find all paths for each traffic demand.
3. Periodically do a linear programming to allocate bandwidth to each path of each traffic demand so that the total throught is maximized.

See FinalReport.pptx for details and demo.


P.S. 

It's part of a course project done by two students.The first part is in https://github.com/Guangtong/ONOS-APP-Dynamic-Widest-Path
We met many challenges in our project. Some are solved such as multipath using cpqd switch. But some are left unsolved when the semester ended, leaving these imperfections: 

- Lacking bandwidth constraint

In Multipath Optimal Allocation, we only specified the weight for each output port to split traffic to multiple paths. If the input flow rate is larger than its expected allocated bandwidth, all of it will still go through.

- Demand measuring dilemma

In Multipath Optimal Allocation, we need to keep measuring the flow demand so that the periodic linear programming always use the correct flow demand. Currently our workaround is not installing forwarding rule on the last switch. So the packet will always go to the controller telling it which flow is currently carrying so that the controller knows where to measure the flow demand. The cost is the packet can never reach the destination host.
