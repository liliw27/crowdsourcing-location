package locationAssignmentBAP.cg.pricing;

import io.StopMsgException;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import model.Customer;
import model.Instance;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import util.Constants;
import util.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/7 17:44
 */
public class ColumnGenerator implements Callable<Void> {
    private Map<String, double[]> dualCostsMap;
    private Instance instance;
    private double gap;
    private Worker worker;
    private List<AssignmentColumn> columns;

    public ColumnGenerator(Map<String, double[]> dualCostsMap, Instance instance, double gap, Worker worker) {
        this.dualCostsMap = dualCostsMap;
        this.instance = instance;
        this.gap = gap;
        this.worker = worker;
        this.columns = new ArrayList<>();
    }


    public void generateNewColumns() {
        for (StationCandidate stationCandidate : instance.getStationCandidates()) {
            List<AssignmentColumn> newColumns = new ArrayList<>();
            List<Customer> customers = new ArrayList<>();
            generateColumns(0, stationCandidate, worker, customers, newColumns);
            columns.addAll(newColumns);
        }
    }

    private void generateColumns(int start, StationCandidate stationCandidate, Worker worker, List<Customer> customers, List<AssignmentColumn> res) {
        int n = instance.getCustomers().size();
        int num = instance.getWorkerCapacityNum();
        Set<Customer> customers1 = new HashSet<>(customers);
        AssignmentColumn column = generateNewColumn(customers1, worker, stationCandidate);
        if (column != null) {
            res.add(column);
        }
        if (customers.size() >= num) {
            return;
        }

        //通过终止条件，进行剪枝优化，避免无效的递归
        //c中还剩 k - c.size()个空位，所以[ i ... n]中至少要有k-c.size()个元素
        //所以i最多为 n - (k - c.size()) + 1

        for (int i = start; i <= n - (num - customers.size()); i++) {
            Customer customer = instance.getCustomers().get(i);

            customers.add(customer);
            generateColumns(i + 1, stationCandidate, worker, customers, res);
            //记得回溯状态啊
            customers.remove(customers.size() - 1);
        }
    }

    private AssignmentColumn generateNewColumn(Set<Customer> customers, Worker worker, StationCandidate stationCandidate) {
        AssignmentColumn column = null;
        double cost = Util.getCost(customers, worker, stationCandidate, instance);
        int demand = Util.getDemand(customers, instance.getScenarios().get(0));
        double recucedCost = getReducedCost(demand, cost, customers, stationCandidate, worker);
        if (recucedCost <= gap) {
            column = new AssignmentColumn_true(null, false, "heuristicPricingSolver", cost, demand, worker, new HashSet<>(customers), stationCandidate, recucedCost);
        }
        return column;
    }

    private double getReducedCost(int demand, double cost, Set<Customer> customers, StationCandidate stationCandidate, Worker worker) {
        double rc = cost;
        for (Customer customer : customers) {
            rc -= dualCostsMap.get("oneVisitPerCustomerAtMost")[customer.getIndex()];
//            rc -= pricingProblem.dualWC[pricingProblem.worker.getIndex()][customer.getIndex()];
        }
        rc -= dualCostsMap.get("stationCapConstraint")[stationCandidate.getIndex()] * demand;
//        rc -= pricingProblem.dualWS[pricingProblem.worker.getIndex()][stationCandidate.getIndex()];
        rc -= dualCostsMap.get("oneRoutePerWorkerAtMost")[worker.getIndex()];
        return rc;
    }


    @Override
    public Void call() {
        columns.clear();
        this.generateNewColumns();
        return null;
    }
    public List<AssignmentColumn> getColumns(){
        return columns;
    }
}
