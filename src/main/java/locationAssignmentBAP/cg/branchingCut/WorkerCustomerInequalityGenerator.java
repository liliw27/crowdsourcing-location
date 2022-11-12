package locationAssignmentBAP.cg.branchingCut;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.cg.masterProblem.LocationAssignmentMasterData;
import locationAssignmentBAP.cg.pricing.PricingProblem;
import locationAssignmentBAP.model.LocationAssignment;
import model.Customer;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractCutGenerator;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 2022/11/6 19:39
 */

public class WorkerCustomerInequalityGenerator extends AbstractCutGenerator<LocationAssignment, LocationAssignmentMasterData> {
    private final Worker worker;
    private final Customer customer;
    private boolean isfixed;

    /**
     * Creates a new AbstractCutGenerator
     *
     * @param dataModel data model instance
     **/
    public WorkerCustomerInequalityGenerator(LocationAssignment dataModel, Worker worker, Customer customer, boolean isfixed) {
        super(dataModel, "WCIneqGenerator");
        this.customer = customer;
        this.worker = worker;
        this.isfixed = isfixed;
    }

    @Override
    public List<AbstractInequality> generateInqualities() {
        WorkerCustomerInequality inequality = new WorkerCustomerInequality(this, worker, customer, isfixed);
        this.addCut(inequality);
        return Collections.singletonList(inequality);
    }

    private void addCut(WorkerCustomerInequality workerCustomerInequality) {
        if (masterData.workerCustomerInequalities.containsKey(workerCustomerInequality))
            throw new RuntimeException("Error, duplicate subtour cut is being generated! This cut should already exist in the master problem: " + workerCustomerInequality);
        //Create the inequality in cplex
        try {
            IloLinearNumExpr expr = masterData.cplex.linearNumExpr();
            for (PricingProblem pricingProblem : masterData.pricingProblems) {

                //Register the columns with this constraint.
                if (pricingProblem.worker.equals(worker)) {
                    for (AssignmentColumn column : masterData.getColumnsForPricingProblemAsList(pricingProblem)) {
                        if (!(column instanceof AssignmentColumn_true)) {
                            continue;
                        }
                        AssignmentColumn_true column_true = (AssignmentColumn_true) column;
                        if (!column_true.customers.contains(customer)) {
                            continue;
                        }
                        IloNumVar var = masterData.getVar(pricingProblem, column);
                        expr.addTerm(1, var);
                    }
                }
            }
            if (isfixed) {
                IloRange workerCustomerConstraint = masterData.cplex.addEq(expr, 1, "FixWorkerCustomer");
                masterData.workerCustomerInequalities.put(workerCustomerInequality, workerCustomerConstraint);
            } else {
                IloRange workerStationConstraint = masterData.cplex.addEq(expr, 0, "IncompatibleWorkerCustomer");
                masterData.workerCustomerInequalities.put(workerCustomerInequality, workerStationConstraint);
            }


        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addCut(AbstractInequality cut) {
        if (!(cut instanceof WorkerStationInequality))
            throw new IllegalArgumentException("This AbstractCutGenerator can ONLY add WorkerStationInequality");
        WorkerCustomerInequality workerCustomerInequality = (WorkerCustomerInequality) cut;
        this.addCut(workerCustomerInequality);
    }

    @Override
    public List<AbstractInequality> getCuts() {
        return new ArrayList<>(masterData.workerCustomerInequalities.keySet());
    }

    @Override
    public void close() {

    }
}
