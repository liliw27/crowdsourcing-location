package locationAssignmentBAP.bap.branching;

import locationAssignmentBAP.bap.branching.branchDecisions.FixLocationWithType;
import locationAssignmentBAP.bap.branching.branchDecisions.RemoveLocation;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.cg.pricing.PricingProblem;
import locationAssignmentBAP.model.LocationAssignment;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.BAPNode;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Class which creates new branches in the Branch-and-Price tree. This particular class branches on a pair of vertices, thereby creating
 * two branches. In one branch, these vertices receive the same color, whereas in the other branch they are colored differently
 */
public final class BranchOnLocationVar extends AbstractBranchCreator<LocationAssignment, AssignmentColumn, PricingProblem> {

    /**
     * index of station to branch on
     **/
    int locationIndex = -1;

    public BranchOnLocationVar(LocationAssignment dataModel, List<PricingProblem> pricingProblems) {
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
        boolean foundLocation = false;
        double bestValue=0;
        for (int i = 0; i < solution.size()  && !foundLocation; i++) {
            if (solution.get(i) instanceof AssignmentColumn_true) {
                continue;
            }
            AssignmentColumn_virtual column_virtual = (AssignmentColumn_virtual) solution.get(i);
            //Select the location with a fractional value closest to 0.5
            if(Math.abs(0.5-column_virtual.value) < Math.abs(0.5- bestValue)){
                locationIndex = column_virtual.stationIndex;
                bestValue=column_virtual.value;
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
        List<BAPNode<LocationAssignment, AssignmentColumn>> bapNodeList=new ArrayList<>();
        //Branch set 1
        for(int t=0;t<dataModel.instance.getType().length;t++){
            FixLocationWithType branchDecision=new FixLocationWithType(locationIndex,t);
            BAPNode<LocationAssignment, AssignmentColumn> node = this.createBranch(parentNode, branchDecision, parentNode.getSolution(), parentNode.getInequalities());
            bapNodeList.add(node);
        }

        //Branch 2:
        RemoveLocation branchingDecision2 = new RemoveLocation(locationIndex);
        BAPNode<LocationAssignment, AssignmentColumn> node = this.createBranch(parentNode, branchingDecision2, parentNode.getSolution(), parentNode.getInequalities());
        bapNodeList.add(node);
        return bapNodeList;
    }
}
