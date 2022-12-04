package benders.cg.pricing;

import benders.cg.column.AssignmentColumn_true;
import benders.model.LocationAssignment;
import ilog.concert.IloConstraint;
import io.StopMsgException;

import model.Customer;
import model.StationCandidate;
import org.jgrapht.util.VertexPair;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import util.Constants;
import util.GlobalVariable;
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
public class HeuristicPricingProblemSolver extends AbstractPricingProblemSolver<LocationAssignment, AssignmentColumn_true, PricingProblem> {
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
        incompatibleStations.addAll(dataModel.incompatibleStations);
        fixedStations.addAll(dataModel.fixedStations);
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
    private void generateColumns(StationCandidate stationCandidate, int start, Set<Customer> customers, Set<AssignmentColumn_true> res) {
        int n = dataModel.instance.getCustomers().size();
        int num = dataModel.instance.getWorkerCapacityNum();
        Set<Customer> customers1 = new HashSet<>(customers);
        AssignmentColumn_true column = null;
        if (customers1.size() >= 1) {
            column = generateNewColumn(customers1, stationCandidate);
        }

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
            if (customers.size() >= 4) {
                int a = 0;
            }
            Customer customer = dataModel.instance.getCustomers().get(i);
            Set<Customer> customerAddition = new HashSet<>(2);
            if (!isCompatible(customers, customer, customerAddition)) {
                continue;
            }
            customers.add(customer);
            customerAddition.add(customer);
            generateColumns(stationCandidate, i + 1, customers, res);
            //记得回溯状态啊
            customers.removeAll(customerAddition);
        }
    }

    private boolean isCompatible(Set<Customer> customers, Customer customer, Set<Customer> customerAddition) {
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
    protected List<AssignmentColumn_true> generateNewColumns() throws TimeLimitExceededException {


        if(GlobalVariable.ENUMERATE){
            List<AssignmentColumn_true> newColumns = new ArrayList<>(Constants.columnNumIte);
            for(AssignmentColumn_true assignmentColumn_true:GlobalVariable.columns){
                if (!assignmentColumn_true.worker.equals(pricingProblem.worker)){
                    continue;
                }
                if(GlobalVariable.isDemandRecorded){
                    if (!assignmentColumn_true.isDemandsSatisfy[pricingProblem.scenario.getIndex()]) {
                        continue;
                    }

                    double cost=assignmentColumn_true.demands[pricingProblem.scenario.getIndex()]*(assignmentColumn_true.cost-dataModel.instance.getUnservedPenalty());

                    double recucedCost = getReducedCost(assignmentColumn_true.demands[pricingProblem.scenario.getIndex()], cost, assignmentColumn_true.customers, assignmentColumn_true.stationCandidate);
                    if (recucedCost <= -Constants.precisionForReducedCost) {
                        AssignmentColumn_true column_true=assignmentColumn_true.clone();
                        column_true.cost=cost;
//                    column_true.demand=assignmentColumn_true.demands[pricingProblem.scenario.getIndex()];
                        newColumns.add(column_true);
                        column_true.associatedPricingProblem=pricingProblem;
                    }

                }else {
                    short demand=Util.getDemand(assignmentColumn_true.customers,pricingProblem.scenario);
                    boolean isDemandSatisfy=(demand <= pricingProblem.scenario.getWorkerCapacity()[assignmentColumn_true.worker.getIndex()]);
                    if (!isDemandSatisfy) {
                        continue;
                    }

                    double cost=demand*(assignmentColumn_true.cost-dataModel.instance.getUnservedPenalty());

                    double recucedCost = getReducedCost(demand, cost, assignmentColumn_true.customers, assignmentColumn_true.stationCandidate);
                    if (recucedCost <= -Constants.precisionForReducedCost) {
                        AssignmentColumn_true column_true=assignmentColumn_true.clone();
                        column_true.cost=cost;
                        column_true.demand=demand;
//                    column_true.demand=assignmentColumn_true.demands[pricingProblem.scenario.getIndex()];
                        newColumns.add(column_true);
                        column_true.associatedPricingProblem=pricingProblem;
                    }
                }
                if (newColumns.size() >= Constants.columnNumIte) {
                    return newColumns;
                }
//
//                int demand = Util.getDemand(assignmentColumn_true.customers, pricingProblem.scenario);
//                if (demand > pricingProblem.scenario.getWorkerCapacity()[pricingProblem.worker.getIndex()]) {
//                    continue;
//                }


            }
            return newColumns;
        }

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
        Set<AssignmentColumn_true> newColumns = new HashSet<>();

        if (fixedStations.size() == 1) {
            StationCandidate stationCandidate = fixedStations.get(0);
            Set<Customer> customers = new HashSet<>(fixedCustomer);
            if (customers.size() == dataModel.instance.getWorkerCapacityNum()) {
                AssignmentColumn_true column = generateNewColumn(new HashSet<>(fixedCustomer), stationCandidate);
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
                if (newColumns.size() >= Constants.columnNumIte) {
                    break;
                }
                if (incompatibleStations.contains(stationCandidate)) {
                    continue;
                }
                Set<Customer> customers = new HashSet<>(fixedCustomer);
                if (customers.size() == dataModel.instance.getWorkerCapacityNum()) {
                    AssignmentColumn_true column = generateNewColumn(new HashSet<>(fixedCustomer), stationCandidate);
                    if (column != null) {
                        newColumns.add(column);
                    }
                } else if (customers.size() < dataModel.instance.getWorkerCapacityNum()) {
                    try {
                        generateColumns(stationCandidate, 0, customers, newColumns);
                    } catch (StopMsgException e) {
                    }
                }
            }


        }
        return new ArrayList<>(newColumns);
    }

    private AssignmentColumn_true generateNewColumn(Set<Customer> customers, StationCandidate stationCandidate) {
        AssignmentColumn_true column = null;
        double cost = 0;
        cost = Util.getCost(customers, pricingProblem.worker, stationCandidate, dataModel.instance);

//        try {
//            cost = Util.getCostXGB((Station) stationCandidate,pricingProblem.worker,new ArrayList<>(customers), dataModel.instance.getTravelCostMatrix());
//        } catch (XGBoostError e) {
//            throw new RuntimeException(e);
//        }
        short demand = Util.getDemand(customers, pricingProblem.scenario);
        if (demand > pricingProblem.scenario.getWorkerCapacity()[pricingProblem.worker.getIndex()]) {
            return column;
        }
        double recucedCost = getReducedCost(demand, cost, customers, stationCandidate);
        if (recucedCost <= -Constants.precisionForReducedCost) {
            column = new AssignmentColumn_true(pricingProblem, false, "heuristicPricingSolver", cost, demand, pricingProblem.worker, new HashSet<>(customers), stationCandidate);
        }
        return column;
    }


    public double getReducedCost(int demand, double cost, Set<Customer> customers, StationCandidate stationCandidate) {
        double rc = cost;
        for (Customer customer : customers) {
            rc -= pricingProblem.dualCostsMap.get("oneVisitPerCustomerAtMost_"+pricingProblem.scenario.getIndex())[customer.getIndex()];
        }
        rc -= pricingProblem.dualCostsMap.get("stationCapConstraint_"+pricingProblem.scenario.getIndex())[stationCandidate.getIndex()] * demand;
        rc -= pricingProblem.dualCostsMap.get("oneRoutePerWorkerAtMost_"+pricingProblem.scenario.getIndex())[pricingProblem.worker.getIndex()] * demand;
        return rc;
    }

    @Override
    protected void setObjective() {

    }

    @Override
    public void close() {

    }


}
