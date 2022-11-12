package locationAssignmentBAP.cg.masterProblem;

import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import locationAssignmentBAP.cg.branchingCut.WorkerCustomerInequality;
import locationAssignmentBAP.cg.branchingCut.WorkerStationInequality;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.pricing.PricingProblem;
import locationAssignmentBAP.model.LocationAssignment;
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
    public final IloNumVar[][] locationVars;
    public final Map<WorkerStationInequality, IloRange> workerStationInequalities;
    public final Map<WorkerCustomerInequality, IloRange> workerCustomerInequalities;

    /**
     * Creates a new MasterData object
     *
     * @param varMap A double map which stores the variables. The first key is the pricing problem, the second key is a column and the value is a variable object, e.g. an IloNumVar in cplex.
     */
    public LocationAssignmentMasterData(IloCplex cplex,Map varMap,IloNumVar[][] locationVars,List<PricingProblem> pricingProblems) {
        super(varMap);
        this.cplex=cplex;
        this.locationVars=locationVars;
        this.pricingProblems=pricingProblems;
        workerStationInequalities =new LinkedHashMap<>();
        workerCustomerInequalities =new LinkedHashMap<>();
    }
}
