package benders.cg.masterProblem;

import benders.cg.column.AssignmentColumn;
import benders.cg.pricing.PricingProblem;
import benders.model.LocationAssignment;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import org.jorlib.frameworks.columnGeneration.master.MasterData;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/7 18:04
 */
public class LocationAssignmentMasterData extends MasterData<LocationAssignment, AssignmentColumn, PricingProblem, IloNumVar> {
    /** Cplex instance **/
    public final IloCplex cplex;
    public final List<PricingProblem> pricingProblems;

    /**
     * Creates a new MasterData object
     *
     * @param varMap A double map which stores the variables. The first key is the pricing problem, the second key is a column and the value is a variable object, e.g. an IloNumVar in cplex.
     */
    public LocationAssignmentMasterData(IloCplex cplex,Map varMap,List<PricingProblem> pricingProblems) {
        super(varMap);
        this.cplex=cplex;
        this.pricingProblems=pricingProblems;
    }
}
