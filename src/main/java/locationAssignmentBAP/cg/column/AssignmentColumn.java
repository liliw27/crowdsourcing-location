package locationAssignmentBAP.cg.column;

import locationAssignmentBAP.cg.pricing.PricingProblem;
import locationAssignmentBAP.model.LocationAssignment;
import model.Customer;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.colgenMain.AbstractColumn;

import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/7 18:00
 */
public abstract class AssignmentColumn extends AbstractColumn<LocationAssignment, PricingProblem> implements Comparable<AssignmentColumn>{
   public  double reducedCost;
    /**
     * Constructs a new column
     *
     * @param associatedPricingProblem Pricing problem to which this column belongs
     * @param isArtificial             Is this an artificial column?
     * @param creator                  Who/What created this column?
     *
     */
    public AssignmentColumn(PricingProblem associatedPricingProblem, boolean isArtificial, String creator) {
        super(associatedPricingProblem, isArtificial, creator);

    }

    @Override
    public abstract boolean equals(Object o) ;

    @Override
    public abstract int hashCode() ;

    @Override
    public String toString() {
        return null;
    }


}
