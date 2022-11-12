package locationAssignmentBAP.cg.column;

import locationAssignmentBAP.cg.pricing.PricingProblem;
import model.Customer;
import model.StationCandidate;
import model.Worker;

import java.util.Set;

/**
 * @author Wang Li
 * @description This class is a virtual "column" recording the solution value corresponding to
 * the location variable (including the location and the capacity type)
 * @date 2022/10/28 20:33
 */
public class AssignmentColumn_virtual extends AssignmentColumn{
    public final int stationIndex;
    public final int type;
    public AssignmentColumn_virtual(PricingProblem associatedPricingProblem, boolean isArtificial, String creator, int stationIndex,int type) {
        super(associatedPricingProblem, isArtificial, creator);
        this.stationIndex = stationIndex;
        this.type = type;
    }
    public int hashCode() {
        return stationIndex*31+type;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AssignmentColumn_virtual))
            return false;
        AssignmentColumn_virtual other = (AssignmentColumn_virtual) o;
        return (this.stationIndex == other.stationIndex&&this.type==other.type);
    }
    public String toString() {
        String string="Value: "+this.value+" creator: "+creator+" stationIndex: "+stationIndex+" type: "+type;
        return string;
    }

    @Override
    public int compareTo(AssignmentColumn o) {
        return 0;
    }
}
