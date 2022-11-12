package locationAssignmentBAP.bap.branching;

import locationAssignmentBAP.bap.branching.branchDecisions.FixWorkerStation;
import locationAssignmentBAP.bap.branching.branchDecisions.IncompatibleWorkerStation;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.cg.pricing.PricingProblem;
import locationAssignmentBAP.model.LocationAssignment;
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
public final class BranchOnStationWorkerPair extends AbstractBranchCreator<LocationAssignment, AssignmentColumn, PricingProblem> {

    /**
     * pair of station and worker to branch on
     **/
    public StationCandidate station = null;
    public Worker worker = null;

    public BranchOnStationWorkerPair(LocationAssignment dataModel, List<PricingProblem> pricingProblems) {
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
        double[][] values = new double[dataModel.instance.getWorkers().size()][dataModel.instance.getStationCandidates().size()];
        //Aggregate pair values
        for (int i = 0; i < solution.size(); i++) {
            if (solution.get(i) instanceof AssignmentColumn_virtual) {
                continue;
            }
            AssignmentColumn_true column_true = (AssignmentColumn_true) solution.get(i);

            int stationIndex = column_true.stationCandidate.getIndex();
            int workerIndex = column_true.worker.getIndex();
            values[workerIndex][stationIndex] += column_true.value;
        }

        for (int k = 0; k < values.length; k++) {
            for (int s = 0; s < values[k].length; s++) {
                if (Math.abs(0.5 - values[k][s]) < Math.abs(0.5 - bestValue)) {
                    station = dataModel.instance.getStationCandidates().get(s);
                    worker = dataModel.instance.getWorkers().get(k);
                    bestValue = values[k][s];
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

        FixWorkerStation branchDecision = new FixWorkerStation(worker,station);
        BAPNode<LocationAssignment, AssignmentColumn> node1 = this.createBranch(parentNode, branchDecision, parentNode.getSolution(), parentNode.getInequalities());


        //Branch 2: the worker is not allowed to visit the station
        IncompatibleWorkerStation branchingDecision2 = new IncompatibleWorkerStation(worker,station);
        BAPNode<LocationAssignment, AssignmentColumn> node2 = this.createBranch(parentNode, branchingDecision2, parentNode.getSolution(), parentNode.getInequalities());
        return Arrays.asList(node1, node2);
    }
}
