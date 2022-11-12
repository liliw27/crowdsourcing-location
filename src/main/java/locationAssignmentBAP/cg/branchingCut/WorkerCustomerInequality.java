package locationAssignmentBAP.cg.branchingCut;

import model.Customer;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractCutGenerator;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;

/**
 * @author Wang Li
 * @description
 * @date 2022/11/6 19:26
 */
public class WorkerCustomerInequality extends AbstractInequality {
    public final Worker worker;
    public final Customer customer;
    private boolean isfixed;
    /**
     * Creates a new inequality
     *
     * @param maintainingGenerator Reference to the AbstractCutGenerator which generates inequalities of the type that extends this class
     */
    public WorkerCustomerInequality(AbstractCutGenerator maintainingGenerator, Worker worker, Customer customer, boolean isfixed) {
        super(maintainingGenerator);
        this.customer=customer;
        this.worker=worker;
        this.isfixed=isfixed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        else if (!(o instanceof WorkerCustomerInequality))
            return false;
        WorkerCustomerInequality other = (WorkerCustomerInequality) o;
            return (this.customer.equals(other.customer) && this.worker.equals(other.worker)&&this.isfixed==other.isfixed);
    }

    @Override
    public int hashCode() {
        int hash=worker.getIndexO();
        hash=hash*31+customer.getNodeIndex();
        if(isfixed){
            hash*=-1;
        }
        return hash;
    }
}
