package MIP.mipSetPartition;

import ilog.concert.IloIntVar;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import locationAssignmentBAP.cg.column.AssignmentColumn;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 6/21/22 3:02 PM
 */
public class MipDataSP {
    public final IloCplex cplex;
    //first stage variable
    public final IloIntVar[][] varsLocation;
    public final IloIntVar[] vars;
    public final List<AssignmentColumn> columns;
    //second stage decision variable

    public MipDataSP(IloCplex cplex, IloIntVar[][] varsLocation, IloIntVar[] vars, List<AssignmentColumn> columns) {
        this.cplex = cplex;
        this.varsLocation = varsLocation;
        this.vars=vars;
        this.columns=columns;
    }


}
