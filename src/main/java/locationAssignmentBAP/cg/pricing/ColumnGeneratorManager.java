package locationAssignmentBAP.cg.pricing;

import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import model.Customer;
import model.Instance;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import org.jorlib.frameworks.columnGeneration.pricing.PricingProblemBundle;
import util.Constants;
import util.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/7 17:44
 */
public class ColumnGeneratorManager {
    Map<String, double[]> dualCostsMap;
    Instance instance;
    double gap;
    /**
     * Executors
     **/
    private final ExecutorService executor;
    /**
     * Futures
     **/
    private final List<Future<Void>> futures;

    public ColumnGeneratorManager(Map<String, double[]> dualCostsMap, Instance instance, double gap) {
        this.dualCostsMap = dualCostsMap;
        this.instance = instance;
        this.gap = gap;
        this.executor = Executors.newFixedThreadPool(Constants.MAXTHREADS); //Creates a threat pool consisting of MAXTHREADS threats
        this.futures = new ArrayList<>(instance.getWorkers().size());
    }


    public List<AssignmentColumn> generateNewColumns() {


        List<AssignmentColumn> newColumnslist = new ArrayList<>();
        List<ColumnGenerator> columnGenerators=new ArrayList<>();
        for (Worker worker : instance.getWorkers()) {
            ColumnGenerator columnGenerator = new ColumnGenerator(dualCostsMap, instance, gap, worker);
            columnGenerators.add(columnGenerator);
            Future<Void> f = executor.submit(columnGenerator);
            futures.add(f);

        }

        for (Future<Void> f : futures) {
            try {
                f.get(); //get() is a blocking procedure
            } catch (ExecutionException e) {

                e.printStackTrace();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for(ColumnGenerator columnGenerator:columnGenerators){
            newColumnslist.addAll(columnGenerator.getColumns());
        }

        return newColumnslist;
    }


}
