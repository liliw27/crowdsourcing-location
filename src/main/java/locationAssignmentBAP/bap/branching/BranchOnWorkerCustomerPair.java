package locationAssignmentBAP.bap.branching;

import locationAssignmentBAP.bap.branching.branchDecisions.FixWorkerCustomer;
import locationAssignmentBAP.bap.branching.branchDecisions.FixWorkerStation;
import locationAssignmentBAP.bap.branching.branchDecisions.IncompatibleWorkerCustomer;
import locationAssignmentBAP.bap.branching.branchDecisions.IncompatibleWorkerStation;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.cg.pricing.PricingProblem;
import locationAssignmentBAP.model.LocationAssignment;
import model.Customer;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.BAPNode;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Class which creates new branches in the Branch-and-Price tree. This particular class branches on a pair of vertices, thereby creating
 * two branches. In one branch, these vertices receive the same color, whereas in the other branch they are colored differently
 */
public final class BranchOnWorkerCustomerPair extends AbstractBranchCreator<LocationAssignment, AssignmentColumn, PricingProblem> {

    /**
     * pair of station and worker to branch on
     **/
    public Customer customer = null;
    public Worker worker = null;

    public BranchOnWorkerCustomerPair(LocationAssignment dataModel, List<PricingProblem> pricingProblems) {
        super(dataModel, pricingProblems);
    }


    /**
     * Determine on which edge from the red or blue matchings we are going to branch.
     *
     * @param solution Fractional column generation solution
     * @return true if a fractional edge exists
     */
    @Override
    protected boolean canPerformBranching(List<AssignmentColumn> solution) {
        double bestValue = 0;
        double[][] values = new double[dataModel.instance.getWorkers().size()][dataModel.instance.getCustomers().size()];
        //Aggregate pair values
        for (int i = 0; i < solution.size(); i++) {
            if (solution.get(i) instanceof AssignmentColumn_virtual) {
                continue;
            }
            AssignmentColumn_true column_true = (AssignmentColumn_true) solution.get(i);
            for (Customer customer1 : column_true.customers) {
                int customerIndex = customer1.getIndex();
                int workerIndex = column_true.worker.getIndex();
                values[workerIndex][customerIndex] += column_true.value;
            }
        }

        for (int k = 0; k < values.length; k++) {
            for (int i = 0; i < values[k].length; i++) {
                if (Math.abs(0.5 - values[k][i]) < Math.abs(0.5 - bestValue)) {
                    customer = dataModel.instance.getCustomers().get(i);
                    worker = dataModel.instance.getWorkers().get(k);
                    bestValue = values[k][i];
                }
            }
        }

        return MathProgrammingUtil.isFractional(bestValue);
    }

    /**
     * Create the branches:
     * <ol>
     * <li>branch set 1: fix a location with each capacity type </li>
     * <li>branch 2: remove a location</li>
     * </ol>
     *
     * @param parentNode Fractional node on which we branch
     * @return List of child nodes
     */
    @Override
    protected List<BAPNode<LocationAssignment, AssignmentColumn>> getBranches(BAPNode<LocationAssignment, AssignmentColumn> parentNode) {
        //Branch 1: the worker must visit the specific station


        FixWorkerCustomer branchDecision = new FixWorkerCustomer(worker, customer);
        BAPNode<LocationAssignment, AssignmentColumn> node1 = this.createBranch(parentNode, branchDecision, parentNode.getSolution(), parentNode.getInequalities());


        //Branch 2: the worker is not allowed to visit the station
        IncompatibleWorkerCustomer branchingDecision2 = new IncompatibleWorkerCustomer(worker, customer);
        BAPNode<LocationAssignment, AssignmentColumn> node2 = this.createBranch(parentNode, branchingDecision2, parentNode.getSolution(), parentNode.getInequalities());
        return Arrays.asList(node1, node2);
    }
}
