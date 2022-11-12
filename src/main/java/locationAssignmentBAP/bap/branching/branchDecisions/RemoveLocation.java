package locationAssignmentBAP.bap.branching.branchDecisions;

import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.model.LocationAssignment;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;

/**
 * @author Wang Li
 * @description
 * @date 2022/10/29 00:49
 */
public class RemoveLocation implements BranchingDecision<LocationAssignment, AssignmentColumn> {
    public final int locationIndex;
    public RemoveLocation(int locationIndex){
        this.locationIndex=locationIndex;
    }
    @Override
    public boolean columnIsCompatibleWithBranchingDecision(AssignmentColumn column) {
        if(column instanceof AssignmentColumn_virtual){
            return false;
        }
        AssignmentColumn_true assignmentColumn_true=(AssignmentColumn_true)column;
        if(assignmentColumn_true.stationCandidate.getIndex()==locationIndex){
            return false;
        }
        return true;
    }

    @Override
    public boolean inEqualityIsCompatibleWithBranchingDecision(AbstractInequality inequality) {
        return true;
    }
    public String toString(){
        return "RemoveLocation "+locationIndex;
    }
}
