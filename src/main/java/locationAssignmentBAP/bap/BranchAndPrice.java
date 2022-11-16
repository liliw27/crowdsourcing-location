package locationAssignmentBAP.bap;

import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.cg.masterProblem.Master;
import locationAssignmentBAP.cg.pricing.PricingProblem;
import locationAssignmentBAP.model.LocationAssignment;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchAndPrice;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.BAPNode;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/7 17:41
 */
public class BranchAndPrice extends AbstractBranchAndPrice<LocationAssignment, AssignmentColumn, PricingProblem> {

    public BranchAndPrice(LocationAssignment modelData,
                          Master master,
                          List<PricingProblem> pricingProblems,
                          List<Class<? extends AbstractPricingProblemSolver<LocationAssignment, AssignmentColumn, PricingProblem>>> solvers,
                          List<? extends AbstractBranchCreator<LocationAssignment, AssignmentColumn, PricingProblem>> branchCreators,
                          int objectiveInitialSolution,
                          int lowerBound,
                          List<AssignmentColumn> initialSolution) {
        super(modelData, master, pricingProblems, solvers, branchCreators, lowerBound, objectiveInitialSolution);
        this.warmStart(objectiveInitialSolution, initialSolution);
    }




    /**
     * Generates an artificial solution. Columns in the artificial solution are of high cost such that they never end up in the final solution
     * if a feasible solution exists, since any feasible solution is assumed to be cheaper than the artificial solution. The artificial solution is used
     * to guarantee that the master problem has a feasible solution.
     *
     * @return artificial solution
     */
    @Override
    protected List<AssignmentColumn> generateInitialFeasibleSolution(BAPNode<LocationAssignment, AssignmentColumn> node) {
//        Set<AssignmentColumn> columnSet=new HashSet<>(node.getInitialColumns());
        List<AssignmentColumn> columns = new ArrayList<>();
//        for(AssignmentColumn column:incumbentSolution){
//            if(column instanceof AssignmentColumn_true){
//                AssignmentColumn_true column_true=(AssignmentColumn_true) column;
//                if(columnSet.contains(column_true)){
//                    continue;
//                }
//                AssignmentColumn_true assignmentColumn_true=new AssignmentColumn_true(column_true.associatedPricingProblem,true,"artificial",column_true.cost,column_true.demand,column_true.worker,column_true.customers,column_true.stationCandidate);
//                columns.add(assignmentColumn_true);
//                columnSet.add(assignmentColumn_true);
//            }
//        }
//        List<AssignmentColumn> columns1= Util.getInitialSolution(dataModel.instance,pricingProblems);
//        for(AssignmentColumn column:columns1){
//            if(column instanceof AssignmentColumn_true){
//                AssignmentColumn_true column_true=(AssignmentColumn_true) column;
//                if(columnSet.contains(column_true)){
//                    continue;
//                }
//                AssignmentColumn_true assignmentColumn_true=new AssignmentColumn_true(column_true.associatedPricingProblem,true,"artificial",column_true.cost,column_true.demand,column_true.worker,column_true.customers,column_true.stationCandidate);
//                columns.add(assignmentColumn_true);
//            }
//        }
        for (PricingProblem pricingProblem : pricingProblems) {
            AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblem, true, "initial", 1000000, 0, pricingProblem.worker, new HashSet<>(dataModel.instance.getCustomers()), dataModel.instance.getStationCandidates().get(0));
            columns.add(assignmentColumn_true);
        }
        return columns;
    }

    /**
     * Checks whether the given node is integer
     *
     * @param node Node in the Branch-and-Price tree
     * @return true if the solution is an integer solution
     */
    @Override
    protected boolean isIntegerNode(BAPNode<LocationAssignment, AssignmentColumn> node) {
        // check whether the values of location variables are fractional
            for (AssignmentColumn column : node.getSolution()) {
                if (column instanceof AssignmentColumn_virtual && MathProgrammingUtil.isFractional(column.value)) {
                    return false;
                }
            }
        return true;
    }
}
