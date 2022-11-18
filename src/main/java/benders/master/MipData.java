package benders.master;

import benders.cg.LocationAssignmentCGSolver;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 6/21/22 3:02 PM
 */
public class MipData {
    public final IloCplex cplex;
    //first stage variable
    public final IloIntVar[] varsLocation;
    public final IloIntVar[] varsCapacity;
    public final IloIntVar[] varsQ;
    public final IloIntVar varQ;

    public Map<String, double[]>[] dualCostsMaps;
    public double[] objForEachScenario;
    //second stage decision variable

    public MipData(IloCplex cplex, IloIntVar[] varsLocation, IloIntVar[] varsCapacity,IloIntVar[] varsQ,IloIntVar varQ) {
        this.cplex = cplex;
        this.varsLocation = varsLocation;
        this.varsCapacity=varsCapacity;
        this.varsQ=varsQ;
        this.varQ=varQ;
    }


}
