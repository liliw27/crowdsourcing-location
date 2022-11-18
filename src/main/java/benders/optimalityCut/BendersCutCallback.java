package benders.optimalityCut;

import benders.cg.LocationAssignmentCGSolver;
import benders.cg.column.AssignmentColumn;
import benders.master.MipData;
import benders.model.LocationAssignment;
import ilog.concert.IloException;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;


import model.Instance;
import model.Scenario;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;
import util.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Wang Li
 * @description
 * @date 2022/11/17 01:27
 */
public class BendersCutCallback extends IloCplex.LazyConstraintCallback{
    private final Instance dataModel;
    private final MipData mipData;
    private final ExecutorService executor;
    /**
     * Futures
     **/
    private final List<Future<Void>> futures;
    private final List<IloRange> optimalityCuts;
    private int iter;

    public BendersCutCallback(Instance dataModel, MipData mipData) {
        this.dataModel=dataModel;
        this.mipData = mipData;
        this.executor = Executors.newFixedThreadPool(Constants.MAXTHREADS); //Creates a threat pool consisting of MAXTHREADS threats
        this.futures = new ArrayList<>(dataModel.getScenarios().size());
        this.optimalityCuts=new ArrayList<>();
        iter=0;
    }

    @Override
    protected void main() throws IloException {
        //for each scenario, solve cg to get the optimal solution of the subProblem and the resulting optimality cut
        long currentT=System.currentTimeMillis();

        double[] objForEachScenario=new double[dataModel.getScenarios().size()];
        Map<String, double[]>[] dualCostsMaps=new Map[dataModel.getScenarios().size()];
        List<LocationAssignmentCGSolver> cgSolvers=new ArrayList<>();
//        double[] capacityvalues=this.getValues(mipData.varsCapacity);
        int[] capacity=new int[dataModel.getStationCandidates().size()];
        for(int i=0;i<capacity.length;i++){
            capacity[i]= MathProgrammingUtil.doubleToInt(this.getValue(mipData.varsCapacity[i]));
        }
        System.out.println(">>>>>>>>>>>>iter<<<<<<<<<<<<"+iter);
        System.out.println("capacity: "+ Arrays.toString(capacity));
        iter++;
        for (Scenario scenario : dataModel.getScenarios()) {
            LocationAssignment locationAssignment=new LocationAssignment(dataModel,capacity);
            LocationAssignmentCGSolver cgSolver=new LocationAssignmentCGSolver(locationAssignment,scenario);
            cgSolvers.add(cgSolver);
//            cgSolver.solveCG();
            Future<Void> f = executor.submit(cgSolver);
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
        for(int i=0;i<cgSolvers.size();i++){
            LocationAssignmentCGSolver cgSolver=cgSolvers.get(i);
            objForEachScenario[i]=cgSolver.getObjectiveValue();
            dualCostsMaps[i]=cgSolver.getDualCostsMap();
        }
        mipData.objForEachScenario=objForEachScenario;
        mipData.dualCostsMaps=dualCostsMaps;
        double[] valuesQ=null;
        double valueQ=0;
        if(dataModel.isMultipleCut()){
             valuesQ=this.getValues(mipData.varsQ);
        }else{
            valueQ=this.getValue(mipData.varQ);
        }
        OptimalityCutGenerator cutGen=new OptimalityCutGenerator(dataModel,mipData,valuesQ,valueQ,optimalityCuts);
        List<IloRange> optimalityCuts = cutGen.generateInqualities ( );
        for (IloRange optimalityCut : optimalityCuts)
        {
            this.add (optimalityCut);
        }
        long time=System.currentTimeMillis()-currentT;
        System.out.println("cut generation time: "+time);
//        executor.shutdownNow();
    }
}
