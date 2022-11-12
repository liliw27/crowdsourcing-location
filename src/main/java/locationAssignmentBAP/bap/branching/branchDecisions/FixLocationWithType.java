package locationAssignmentBAP.bap.branching.branchDecisions;

import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.model.LocationAssignment;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;

/**
 * @author Wang Li
 * @description
 * @date 2022/10/29 00:44
 */
public class FixLocationWithType implements BranchingDecision<LocationAssignment, AssignmentColumn> {
    public final int locationIndex;
    public final int type;
    public FixLocationWithType(int locationIndex, int type){
        this.type=type;
        this.locationIndex=locationIndex;
    }
    @Override
    public boolean columnIsCompatibleWithBranchingDecision(AssignmentColumn column) {
        if(column instanceof AssignmentColumn_virtual){
            return false;
        }
        return true;
    }

    @Override
    public boolean inEqualityIsCompatibleWithBranchingDecision(AbstractInequality inequality) {
        return true;
    }
    public String toString(){
        return "LocateWithType "+locationIndex+" "+type;
    }
}
