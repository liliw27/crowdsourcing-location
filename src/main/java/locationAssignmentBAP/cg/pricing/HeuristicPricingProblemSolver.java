package locationAssignmentBAP.cg.pricing;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import io.StopMsgException;
import locationAssignmentBAP.bap.branching.branchDecisions.DifferentColumn;
import locationAssignmentBAP.bap.branching.branchDecisions.FixWorkerCustomer;
import locationAssignmentBAP.bap.branching.branchDecisions.FixWorkerStation;
import locationAssignmentBAP.bap.branching.branchDecisions.IncompatibleWorkerCustomer;
import locationAssignmentBAP.bap.branching.branchDecisions.IncompatibleWorkerStation;
import locationAssignmentBAP.bap.branching.branchDecisions.RemoveLocation;
import locationAssignmentBAP.bap.branching.branchDecisions.SameColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.model.LocationAssignment;
import model.Customer;
import model.Instance;
import model.StationCandidate;
import org.jgrapht.util.VertexPair;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import util.Constants;
import util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/7 17:44
 */
public class HeuristicPricingProblemSolver extends AbstractPricingProblemSolver<LocationAssignment, AssignmentColumn, PricingProblem> {
    Set<StationCandidate> incompatibleStations = new HashSet<>();
    List<StationCandidate> fixedStations = new ArrayList<>();
    Set<VertexPair<Customer>> sameColumn = new HashSet<>();
    Set<VertexPair<Customer>> differentColumn = new HashSet<>();
    Set<Customer> incompatibleCustomers = new HashSet<>();
    Set<Customer> fixedCustomer = new HashSet<>();
    int count = 0;

    private Map<VertexPair<Customer>, IloConstraint> branchingPairConstraints = new HashMap<>();
//    private Map<Worker, List<IloConstraint>> workerConstraints = new HashMap<>();

    /**
     * Creates a new solver instance for a particular pricing problem
     *
     * @param dataModel      data model
     * @param pricingProblem pricing problem
     */
    public HeuristicPricingProblemSolver(LocationAssignment dataModel, PricingProblem pricingProblem) {
        super(dataModel, pricingProblem);
        this.name = "HeuristicPricingProblemSolver";
    }

    private boolean isCustomerCompatible(Set<Customer> fixedCustomer, Set<Customer> incompatibleCustomers, Set<VertexPair<Customer>> sameColumn, Set<VertexPair<Customer>> differentColumn) {
        for (VertexPair<Customer> pair : differentColumn) {
            if (fixedCustomer.contains(pair.getFirst()) && fixedCustomer.contains(pair.getSecond())) {
                return false;
            }
        }
        for (VertexPair<Customer> pair : sameColumn) {
            if (fixedCustomer.contains(pair.getFirst()) && incompatibleCustomers.contains(pair.getSecond())) {
                return false;
            }
            if (fixedCustomer.contains(pair.getSecond()) && incompatibleCustomers.contains(pair.getFirst())) {
                return false;
            }
        }
        return true;
    }


    /**
     * 回溯求所有组合结果
     *
     * @param start     开始搜索新元素的位置
     * @param customers 当前已经找到的组合
     */
    private void generateColumns(StationCandidate stationCandidate, int start, Set<Customer> customers, Set<AssignmentColumn> res) {
        int n = dataModel.instance.getCustomers().size();
        int num = dataModel.instance.getWorkerCapacityNum();
        Set<Customer> customers1 = new HashSet<>(customers);
        AssignmentColumn column = generateNewColumn(customers1, stationCandidate);
        if (column != null) {
            res.add(column);
            if (res.size() >= Constants.columnNumIte) {
                throw new StopMsgException();
            }
            return;
        }
        if (customers.size() >= num) {
            return;
        }

        //通过终止条件，进行剪枝优化，避免无效的递归
        //c中还剩 k - c.size()个空位，所以[ i ... n]中至少要有k-c.size()个元素
        //所以i最多为 n - (k - c.size()) + 1

        for (int i = start; i <= n - (num - customers.size()); i++) {
            if(customers.size()>=4){
                int a=0;
            }
            Customer customer = dataModel.instance.getCustomers().get(i);
            Set<Customer> customerAddition=new HashSet<>(2);
            if (!isCompatible(customers, customer,customerAddition)) {
                continue;
            }
            customers.add(customer);
            customerAddition.add(customer);
            generateColumns(stationCandidate, i + 1, customers, res);
            //记得回溯状态啊
            customers.removeAll(customerAddition);
        }
    }

    private boolean isCompatible(Set<Customer> customers, Customer customer,Set<Customer> customerAddition) {
        Set<Customer> customerSet = new HashSet<>(customers);
        if (incompatibleCustomers.contains(customer)) {
            return false;
        }
        if (customerSet.contains(customer)) {
            return false;
        }
        //check sameColumn branching
        for (VertexPair<Customer> pair : sameColumn) {
            if (customers.size() + 2 > dataModel.instance.getWorkerCapacityNum() && (pair.getFirst().equals(customer) || pair.getSecond().equals(customer))) {
                return false;
            }
            if (pair.getFirst().equals(customer)) {
                customerAddition.add(pair.getSecond());
                customers.add(pair.getSecond());
            } else if (pair.getSecond().equals(customer)) {
                customers.add(pair.getFirst());
                customerAddition.add(pair.getFirst());
            }
        }
        //check differentColumn branching

        for (VertexPair<Customer> pair : differentColumn) {
            if (customerSet.contains(pair.getFirst()) && customer.equals(pair.getSecond())) {
                return false;
            }
            if (customerSet.contains(pair.getSecond()) && customer.equals(pair.getFirst())) {
                return false;
            }
        }
        return true;
    }


    @Override
    protected List<AssignmentColumn> generateNewColumns() throws TimeLimitExceededException {

        if (incompatibleStations.size() >= dataModel.instance.getStationCandidates().size()) {
            return new ArrayList<>();
        }
        if (fixedStations.size() > 1) {
            return new ArrayList<>();
        }
        if (!isCustomerCompatible(fixedCustomer, incompatibleCustomers, sameColumn, differentColumn)) {
            return new ArrayList<>();
        }
        if (fixedCustomer.size() > dataModel.instance.getWorkerCapacityNum()) {
            return new ArrayList<>();
        }
        Set<AssignmentColumn> newColumns = new HashSet<>();

        if (fixedStations.size() == 1) {
            StationCandidate stationCandidate = fixedStations.get(0);
            Set<Customer> customers = new HashSet<>(fixedCustomer);
            if (customers.size() == dataModel.instance.getWorkerCapacityNum()) {
                AssignmentColumn column = generateNewColumn(new HashSet<>(fixedCustomer), stationCandidate);
                if (column != null) {
                    newColumns.add(column);
                }
                return new ArrayList<>(newColumns);
            } else {
                try {
                    generateColumns(stationCandidate, 0, customers, newColumns);
                } catch (StopMsgException e) {
                }
            }

        } else {
            for (StationCandidate stationCandidate : dataModel.instance.getStationCandidates()) {
                if(newColumns.size()>=Constants.columnNumIte){
                    break;
                }
                if (incompatibleStations.contains(stationCandidate)) {
                    continue;
                }
                Set<Customer> customers = new HashSet<>(fixedCustomer);
                if (customers.size() == dataModel.instance.getWorkerCapacityNum()) {
                    AssignmentColumn column = generateNewColumn(new HashSet<>(fixedCustomer), stationCandidate);
                    if (column != null) {
                        newColumns.add(column);
                    }
                } else if(customers.size() < dataModel.instance.getWorkerCapacityNum()){
                    try {
                        generateColumns(stationCandidate, 0, customers, newColumns);
                    } catch (StopMsgException e) {
                    }
                }
            }




        }
        return new ArrayList<>(newColumns);
    }

    private AssignmentColumn generateNewColumn(Set<Customer> customers, StationCandidate stationCandidate) {
        AssignmentColumn column = null;
        double cost = Util.getCost(customers, pricingProblem.worker, stationCandidate, dataModel.instance);
        int demand = Util.getDemand(customers, dataModel.instance.getScenarios().get(0));
        double recucedCost = getReducedCost(demand, cost, customers, stationCandidate, dataModel.instance);
        if (recucedCost <= -Constants.precisionForReducedCost) {
            column = new AssignmentColumn_true(pricingProblem, false, "heuristicPricingSolver", cost, demand, pricingProblem.worker, new HashSet<>(customers), stationCandidate);
        }
        return column;
    }


    public double getReducedCost(int demand, double cost, Set<Customer> customers, StationCandidate stationCandidate, Instance instance) {
        double rc = cost;
        for (Customer customer : customers) {
            rc -= pricingProblem.dualCostsMap.get("oneVisitPerCustomerAtMost")[customer.getIndex()];
            rc -= pricingProblem.dualWC[pricingProblem.worker.getIndex()][customer.getIndex()];
        }
        rc -= pricingProblem.dualCostsMap.get("stationCapConstraint")[stationCandidate.getIndex()] * demand;
        rc -= pricingProblem.dualWS[pricingProblem.worker.getIndex()][stationCandidate.getIndex()];
        rc -= pricingProblem.dualCostsMap.get("oneRoutePerWorkerAtMost")[pricingProblem.worker.getIndex()];
        return rc;
    }

    @Override
    protected void setObjective() {

    }

    @Override
    public void close() {

    }

    /**
     * Listen to branching decisions. The pricing problem is changed by the branching decisions.
     *
     * @param bd BranchingDecision
     */
    @Override
    public void branchingDecisionPerformed(BranchingDecision bd) {
        if (bd instanceof RemoveLocation) {
            RemoveLocation removeLocation = (RemoveLocation) bd;
            incompatibleStations.add(dataModel.instance.getStationCandidates().get(removeLocation.locationIndex));
        } else if (bd instanceof FixWorkerStation) {
            FixWorkerStation fixWorkerStation = (FixWorkerStation) bd;
            if (pricingProblem.worker.equals(fixWorkerStation.worker)) {
                fixedStations.add(fixWorkerStation.stationCandidate);
            }
        } else if (bd instanceof IncompatibleWorkerStation) {
            IncompatibleWorkerStation incompatibleWorkerStation = (IncompatibleWorkerStation) bd;
            if (pricingProblem.worker.equals(incompatibleWorkerStation.worker)) {
                incompatibleStations.add(incompatibleWorkerStation.stationCandidate);
            }
        } else if (bd instanceof FixWorkerCustomer) {
            FixWorkerCustomer fixWorkerCustomer = (FixWorkerCustomer) bd;
            if (pricingProblem.worker.equals(fixWorkerCustomer.worker)) {
                fixedCustomer.add(fixWorkerCustomer.customer);
            }
        } else if (bd instanceof IncompatibleWorkerCustomer) {
            IncompatibleWorkerCustomer incompatibleWorkerCustomer = (IncompatibleWorkerCustomer) bd;
            if (pricingProblem.worker.equals(incompatibleWorkerCustomer.worker)) {
                incompatibleCustomers.add(incompatibleWorkerCustomer.customer);
            }
        } else if (bd instanceof SameColumn) { //Ensure that two vertices appear together in an independent set
            SameColumn sameColorDecision = (SameColumn) bd;
            sameColumn.add(sameColorDecision.vertexPair);
        } else if (bd instanceof DifferentColumn) { //Ensure that two vertices do NOT appear together in an independent set.
            DifferentColumn differentColorDecision = (DifferentColumn) bd;
            differentColumn.add(differentColorDecision.vertexPair);
        }


    }

    /**
     * When the Branch-and-Price algorithm backtracks, branching decisions are reversed.
     *
     * @param bd BranchingDecision
     */
    @Override
    public void branchingDecisionReversed(BranchingDecision bd) {
        if (bd instanceof RemoveLocation) {
            RemoveLocation removeLocation = (RemoveLocation) bd;
            incompatibleStations.remove(dataModel.instance.getStationCandidates().get(removeLocation.locationIndex));
        } else if (bd instanceof FixWorkerStation) {
            FixWorkerStation fixWorkerStation = (FixWorkerStation) bd;
            if (pricingProblem.worker.equals(fixWorkerStation.worker)) {
                fixedStations.remove(fixWorkerStation.stationCandidate);
            }
        } else if (bd instanceof IncompatibleWorkerStation) {
            IncompatibleWorkerStation incompatibleWorkerStation = (IncompatibleWorkerStation) bd;
            if (pricingProblem.worker.equals(incompatibleWorkerStation.worker)) {
                incompatibleStations.remove(incompatibleWorkerStation.stationCandidate);
            }
        } else if (bd instanceof FixWorkerCustomer) {
            FixWorkerCustomer fixWorkerCustomer = (FixWorkerCustomer) bd;
            if (pricingProblem.worker.equals(fixWorkerCustomer.worker)) {
                fixedCustomer.remove(fixWorkerCustomer.customer);
            }
        } else if (bd instanceof IncompatibleWorkerCustomer) {
            IncompatibleWorkerCustomer incompatibleWorkerCustomer = (IncompatibleWorkerCustomer) bd;
            if (pricingProblem.worker.equals(incompatibleWorkerCustomer.worker)) {
                incompatibleCustomers.remove(incompatibleWorkerCustomer.customer);
            }
        } else if (bd instanceof SameColumn) { //Ensure that two vertices appear together in an independent set
            SameColumn sameColorDecision = (SameColumn) bd;
            sameColumn.remove(sameColorDecision.vertexPair);
        } else if (bd instanceof DifferentColumn) { //Ensure that two vertices do NOT appear together in an independent set.
            DifferentColumn differentColorDecision = (DifferentColumn) bd;
            differentColumn.remove(differentColorDecision.vertexPair);
        }

    }
}
