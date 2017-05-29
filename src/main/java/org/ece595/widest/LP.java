package org.ece595.widest;

/**
 * Created by mininet on 4/27/17.
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.linear.LinearConstraint;
import org.apache.commons.math3.optimization.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optimization.linear.Relationship;
import org.apache.commons.math3.optimization.linear.SimplexSolver;
import org.onosproject.net.topology.TopologyEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class LP {
//    public double[][] table = {
//            {1,0,1,0,0,0,1,0,0,0,1,0},
//            {1,0,0,0,1,0,0,0,1,0,1,0},
//            {0,0,0,0,0,0,0,0,0,0,0,0},
//            {0,0,0,0,0,0,0,0,0,0,0,0},
//            {0,0,0,0,0,0,0,0,0,0,0,0},
//            {0,0,1,0,0,1,0,0,0,0,0,0},
//            {0,0,0,0,0,0,0,1,1,0,0,0},
//            {0,0,0,0,0,0,0,0,0,0,0,0},
//            {0,0,0,0,0,0,0,0,0,0,0,0},
//            {0,0,0,0,0,0,0,0,0,0,0,0},
//            {2,2,1,1,1,1,10,10,10,10,2,2}
//    };
//    public double[][] demandcon = {
//            {1,0},
//            {1,0},
//            {0,0},
//            {0,0},
//            {0,0},
//            {0,1},
//            {0,1},
//            {0,0},
//            {0,0},
//            {0,0},
//            {2,4}
//    };

    private final Logger log = LoggerFactory.getLogger(getClass());

    public void calculate() {
        AppComponent.VariableNumBandwidth = new HashMap<>();
        //describe the optimization problem
        int Nvar = AppComponent.consideredPathNum * AppComponent.demandsNum;
        double[] objectiveCoefficient = new double[Nvar];
        for (int i = 0; i < Nvar; i++) {
            objectiveCoefficient[i] = 1;
        }
        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoefficient, 0);

        Collection constraints = new ArrayList();
        //link capacity constraints
        double[] row = new double[Nvar];
        for(int i = 0; i < AppComponent.edgeIndex.size(); i ++) {
            for(int j = 0; j < Nvar; j ++) {
                row[j] = AppComponent.pathLinkRelation[j][i];
            }
            constraints.add(new LinearConstraint(row, Relationship.LEQ, AppComponent.edgeCapacityArray[i]));
        }
        //demand constraints, each group
        for(int groupNum = 0; groupNum < AppComponent.demandsNum; groupNum ++) {
            for (int k = 0; k < Nvar; k++) {
                row[k] = 0;
            }
            for(int pathNumInGroup = 0; pathNumInGroup < AppComponent.consideredPathNum; pathNumInGroup ++) {
                int temp = groupNum * AppComponent.consideredPathNum + pathNumInGroup;
                row[temp] = AppComponent.groupDemandCoefficient[pathNumInGroup][groupNum];
            }
            constraints.add(new LinearConstraint(row, Relationship.LEQ, AppComponent.groupDemandArr[groupNum]));
        }
        //in order to bound the variables that do not appear in any constraint function, less than largest capacity
        for (int i = 0; i < Nvar; i++) {
            for (int j = 0; j < Nvar; j++) {
                row[j] = 0;
            }
            row[i]= 1;
            constraints.add(new LinearConstraint(row, Relationship.LEQ, AppComponent.largestCapacity));
        }

        //bandwidth can not be negative
        //create and run solver
        PointValuePair solution = null;
        try {
            solution = new SimplexSolver().optimize(f, constraints, GoalType.MAXIMIZE, true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (solution != null) {
            //print decision variables
            String result = "The bandwidth for each path is:\n";
            AppComponent.throughput = 0;
            for (int i = 0; i < Nvar; i++) {
                double temp = solution.getPoint()[i];
                if(AppComponent.VariableNumPath.get(i) == null) {
                    AppComponent.VariableNumBandwidth.put(i, 0.0);
                    result += String.valueOf(0.0) + "\n";
                }else {
                    AppComponent.VariableNumBandwidth.put(i,temp);
                    AppComponent.throughput += temp;
                    result += String.valueOf(temp) + "\n";
                }
            }
            String overall = "The maximum overall throughput is:\n" + String.valueOf(AppComponent.throughput) + "\n";
            //log.info("\n@@@@@@@@@@@@\n" + overall + result + "\n@@@@@@@@@@@@\n");
        }

        // assign values to the interface for group table installation
        for (int i = 0; i < AppComponent.finalResult.size(); i++) {
            //log.info("\nDDDDDDDDDDDDDDDDDDDDD" + i + "\n");
            int demandNum = i;
            DemandResult demandResult = AppComponent.finalResult.get(demandNum);
            Set<PathResult> pathResults = new HashSet<>();
            double demandSatisfied = 0;
            for (int j = 0; j < AppComponent.consideredPathNum; j++) {
                List<TopologyEdge> edgeList = AppComponent.VariableNumPath.get(demandNum * AppComponent.consideredPathNum + j);
                double bw = AppComponent.VariableNumBandwidth.get(demandNum * AppComponent.consideredPathNum + j);
                if(edgeList != null) {
                    PathResult pathResult = new PathResult(edgeList,bw);
                    pathResults.add(pathResult);
                    demandSatisfied += bw;
                }
            }
            demandResult.pathResults = pathResults;
            demandResult.demandSatisfied = demandSatisfied;
            AppComponent.finalResult.put(demandNum, demandResult);//update
        }





    }

}
