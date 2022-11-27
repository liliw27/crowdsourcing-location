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
    public final IloNumVar[] varsCapacity;
    public final IloNumVar[] varsQ;
    public final IloNumVar varQ;

    public Map<String, double[]>[] dualCostsMaps;
    public double[] objForEachScenario;
    //second stage decision variable

    public IloNumVar vart;
    public IloNumVar varz;

    public MipData(IloCplex cplex, IloIntVar[] varsLocation, IloNumVar[] varsCapacity,IloNumVar[] varsQ,IloNumVar varQ,IloNumVar vart,IloNumVar varz) {
        this.cplex = cplex;
        this.varsLocation = varsLocation;
        this.varsCapacity=varsCapacity;
        this.varsQ=varsQ;
        this.varQ=varQ;
        this.vart=vart;
        this.varz=varz;
    }


}
