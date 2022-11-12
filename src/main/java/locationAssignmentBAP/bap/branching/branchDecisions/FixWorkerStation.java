package locationAssignmentBAP.bap.branching.branchDecisions;

import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.model.LocationAssignment;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;

/**
 * @author Wang Li
 * @description
 * @date 2022/10/29 00:44
 */
public class FixWorkerStation implements BranchingDecision<LocationAssignment, AssignmentColumn> {
    public final Worker worker;
    public final StationCandidate stationCandidate;
    public FixWorkerStation(Worker worker, StationCandidate stationCandidate){
        this.worker=worker;
        this.stationCandidate=stationCandidate;
    }
    @Override
    public boolean columnIsCompatibleWithBranchingDecision(AssignmentColumn column) {
        if(column instanceof AssignmentColumn_virtual){
            return false;
        }
        AssignmentColumn_true column_true=(AssignmentColumn_true) column;
        if(column_true.worker.equals(worker)&&!column_true.stationCandidate.equals(stationCandidate)){
            return false;
        }
        return true;
    }

    @Override
    public boolean inEqualityIsCompatibleWithBranchingDecision(AbstractInequality inequality) {
        return true;
    }
    public String toString(){
        return "FixWorkerStation: worker: "+worker.getIndex()+", station: "+stationCandidate.getIndex();
    }
}
