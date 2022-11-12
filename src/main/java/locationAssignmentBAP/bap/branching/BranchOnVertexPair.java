package locationAssignmentBAP.bap.branching;

import locationAssignmentBAP.bap.branching.branchDecisions.DifferentColumn;
import locationAssignmentBAP.bap.branching.branchDecisions.SameColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.cg.pricing.PricingProblem;
import locationAssignmentBAP.model.LocationAssignment;
import model.Customer;
import org.jgrapht.util.VertexPair;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.BAPNode;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Class which creates new branches in the Branch-and-Price tree. This particular class branches on a pair of vertices, thereby creating
 * two branches. In one branch, these vertices receive the same color, whereas in the other branch they are colored differently
 */
public final class BranchOnVertexPair extends AbstractBranchCreator<LocationAssignment, AssignmentColumn, PricingProblem> {

    /**
     * Pair of vertices to branch on
     **/
    VertexPair<Customer> candidateVertexPair = null;

    public BranchOnVertexPair(LocationAssignment dataModel, List<PricingProblem> pricingProblems) {
        super(dataModel, pricingProblems);
    }


    /**
     * Determine on which pair of vertices we are going to branch.
     *
     * @param solution Fractional column generation solution
     * @return true if a fractional edge exists
     */
    @Override
    protected boolean canPerformBranching(List<AssignmentColumn> solution) {
        //Find a vertex v1 which is in BOTH assignment column s1 and assignment column s2, and a vertex v2 which is ONLY in s1.
        Customer v1 = null;
        Customer v2 = null;

//        int v1=-1;
//        int v2=-1;
        boolean foundPair = false;
        for (int i = 0; i < solution.size() - 1 && !foundPair; i++) {
            if (solution.get(i) instanceof AssignmentColumn_virtual) {
                continue;
            }
            for (int j = i + 1; j < solution.size() && !foundPair; j++) {
                if (solution.get(j) instanceof AssignmentColumn_virtual) {
                    continue;
                }
                AssignmentColumn_true s1 = (AssignmentColumn_true) solution.get(i);
                AssignmentColumn_true s2 = (AssignmentColumn_true) solution.get(j);
                v1 = v2 = null;


                for (Iterator<Customer> it = s1.customers.iterator(); it.hasNext() && !foundPair; ) {
                    Customer v = it.next();
                    if (v1 == null && s2.customers.contains(v)) {
                        v1 = v;
                    } else if (v2 == null && !s2.customers.contains(v)) {
                        v2 = v;
                    }


                }
                for (Iterator<Customer> it = s2.customers.iterator(); it.hasNext() && !foundPair; ) {
                    Customer v = it.next();
                    if (v2 == null && !s1.customers.contains(v)) {
                        v2 = v;
                    }

                }
                foundPair = !(v1 == null || v2 == null);
            }
        }
        if (foundPair)
            candidateVertexPair = new VertexPair<>(v1, v2);
        return foundPair;
    }

    /**
     * Create the branches:
     * <ol>
     * <li>branch 1: pair of vertices {@code vertexPair} must be assigned to the same assignment column,</li>
     * <li>branch 2: pair of vertices {@code vertexPair} must be assigned to the different assignment columns,</li>
     * </ol>
     *
     * @param parentNode Fractional node on which we branch
     * @return List of child nodes
     */
    @Override
    protected List<BAPNode<LocationAssignment, AssignmentColumn>> getBranches(BAPNode<LocationAssignment, AssignmentColumn> parentNode) {
        //Branch 1: same column:
        SameColumn branchingDecision1 = new SameColumn(candidateVertexPair);
        BAPNode<LocationAssignment, AssignmentColumn> node2 = this.createBranch(parentNode, branchingDecision1, parentNode.getSolution(), parentNode.getInequalities());

        //Branch 2: different column:
        DifferentColumn branchingDecision2 = new DifferentColumn(candidateVertexPair);
        BAPNode<LocationAssignment, AssignmentColumn> node1 = this.createBranch(parentNode, branchingDecision2, parentNode.getSolution(), parentNode.getInequalities());

        return Arrays.asList(node1, node2);
    }
}
