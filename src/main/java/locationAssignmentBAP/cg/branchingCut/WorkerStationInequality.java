package locationAssignmentBAP.cg.branchingCut;

import locationAssignmentBAP.bap.branching.BranchOnStationWorkerPair;
import locationAssignmentBAP.bap.branching.branchDecisions.FixWorkerStation;
import locationAssignmentBAP.bap.branching.branchDecisions.IncompatibleWorkerStation;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractCutGenerator;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;

/**
 * @author Wang Li
 * @description
 * @date 2022/11/6 19:26
 */
public class WorkerStationInequality extends AbstractInequality {
    public final Worker worker;
    public final StationCandidate stationCandidate;
    private boolean isfixed;
    /**
     * Creates a new inequality
     *
     * @param maintainingGenerator Reference to the AbstractCutGenerator which generates inequalities of the type that extends this class
     */
    public WorkerStationInequality(AbstractCutGenerator maintainingGenerator, Worker worker, StationCandidate stationCandidate,boolean isfixed) {
        super(maintainingGenerator);
        this.stationCandidate=stationCandidate;
        this.worker=worker;
        this.isfixed=isfixed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        else if (!(o instanceof WorkerStationInequality))
            return false;
        WorkerStationInequality other = (WorkerStationInequality) o;
            return (this.stationCandidate.equals(other.stationCandidate) && this.worker.equals(other.worker)&&this.isfixed==other.isfixed);
    }

    @Override
    public int hashCode() {
        int hash=worker.getIndexO();
        hash=hash*31+stationCandidate.getNodeIndex();
        if(isfixed){
            hash*=-1;
        }
        return hash;
    }
}
