package benders.optimalityCut;

import benders.cg.LocationAssignmentCGSolver;
import benders.master.MipData;
import benders.model.LocationAssignment;
import benders.model.Solution;
import ilog.concert.IloException;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;


import io.StopMsgException;
import model.Instance;
import model.Scenario;
import model.Station;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;
import util.Constants;
import util.GlobalVariable;
import util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
public class BendersCutCallback extends IloCplex.LazyConstraintCallback {
    private final Instance dataModel;
    private final MipData mipData;
    private final ExecutorService executor;
    /**
     * Futures
     **/
    private final List<Future<Void>> futures;
    private final List<IloRange> optimalityCuts;
    private int iter;
//    private double obj=Double.MAX_VALUE;

    public BendersCutCallback(Instance dataModel, MipData mipData) {
        this.dataModel = dataModel;
        this.mipData = mipData;
        this.executor = Executors.newFixedThreadPool(Constants.MAXTHREADS); //Creates a threat pool consisting of MAXTHREADS threats
        this.futures = new ArrayList<>(dataModel.getScenarios().size());
        this.optimalityCuts = mipData.optimalityCuts;
        iter = 0;
    }

    @Override
    protected void main() throws IloException {
        //for each scenario, solve cg to get the optimal solution of the subProblem and the resulting optimality cut
        long currentT = System.currentTimeMillis();

        double[] objForEachScenario = new double[dataModel.getScenarios().size()];
        Map<String, double[]>[] dualCostsMaps = new Map[dataModel.getScenarios().size()];
        List<LocationAssignmentCGSolver> cgSolvers = new ArrayList<>();
//        double[] capacityvalues=this.getValues(mipData.varsCapacity);
        double[] capacity = new double[dataModel.getStationCandidates().size()];
        for (int i = 0; i < capacity.length; i++) {
            capacity[i] = this.getValue(mipData.varsCapacity[i]);
            if (capacity[i] < Constants.EPSILON) {
                capacity[i] = 0;
            }
        }
        System.out.println(">>>>>>>>>>>>iter<<<<<<<<<<<<" + iter);
        System.out.println("capacity: " + Arrays.toString(capacity));
//        if(GlobalVariable.obj>this.getObjValue()){
//            GlobalVariable.obj=this.getObjValue();
//        }

        iter++;
        for (int i = 0; i < dataModel.getScenarios().size(); i++) {
            Scenario scenario = dataModel.getScenarios().get(i);
            LocationAssignment locationAssignment = new LocationAssignment(dataModel, capacity);
            LocationAssignmentCGSolver cgSolver = new LocationAssignmentCGSolver(locationAssignment, scenario);
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

        for (int i = 0; i < cgSolvers.size(); i++) {
            LocationAssignmentCGSolver cgSolver = cgSolvers.get(i);
            objForEachScenario[i] = cgSolver.getObjectiveValue() + dataModel.getUnservedPenalty() * dataModel.getScenarios().get(i).getDemandTotal();
//            objForEachScenario[i] = cgSolver.getObjectiveValue() ;
            dualCostsMaps[i] = cgSolver.getDualCostsMap();
        }

        double firstStageObj = 0;
        for (int s = 0; s < dataModel.getStationCandidates().size(); s++) {
            boolean isSelected = MathProgrammingUtil.doubleToBoolean(this.getValue(mipData.varsLocation[s]));
            if (isSelected) {
                firstStageObj += dataModel.getStationCandidates().get(s).getFixedCost();
                firstStageObj += dataModel.getStationCandidates().get(s).getCapacityCost() * this.getValue(mipData.varsCapacity[s]);
            }
        }
        double secondStageObj;
        double q = 0;
        double valueZ = 0;
        double cvar = 0;
        double valuet=0;
        if (dataModel.isCVaR()) {
            List<Double> objForEachScenarioList=new ArrayList<>();
            for(Scenario scenario : dataModel.getScenarios()){
                objForEachScenarioList.add(objForEachScenario[scenario.getIndex()]);
            }
            Collections.sort(objForEachScenarioList);
            valueZ = Util.percentile(objForEachScenarioList,GlobalVariable.alpha*100);
            for (Scenario scenario : dataModel.getScenarios()) {
                q += objForEachScenario[scenario.getIndex()] * scenario.getProbability();
                cvar += Math.max(0, objForEachScenario[scenario.getIndex()] - valueZ) * scenario.getProbability()/(1 - GlobalVariable.alpha);
            }
            cvar+=valueZ;
            secondStageObj = GlobalVariable.lambda * cvar+ (1 - GlobalVariable.lambda) * q;
        } else {
            for (Scenario scenario : dataModel.getScenarios()) {
                q += objForEachScenario[scenario.getIndex()] * scenario.getProbability();
            }
            secondStageObj = q;
        }

        double upperBound = firstStageObj + secondStageObj;
        if (upperBound < mipData.firstPlusSecondObj) {
            mipData.firstPlusSecondObj = upperBound;
            mipData.firstStageObj = firstStageObj;
            mipData.secondStageObj = secondStageObj;
            mipData.expectedObj = q;
            mipData.CVaR = cvar;
            mipData.solution = getSolution();
        }


        mipData.objForEachScenario = objForEachScenario;
        mipData.dualCostsMaps = dualCostsMaps;
        double[] valuesQ = null;
        double valueQ = 0;

        if (dataModel.isMultipleCut()) {
            valuesQ = this.getValues(mipData.varsQ);
        } else {
            valueQ = this.getValue(mipData.varQ);
//            valuet = this.getValue(mipData.vart);
        }
        System.out.println("###########################upperbound: " + upperBound + "; lowerbound: " + this.getObjValue() + "; bestObj: " + this.getBestObjValue() + "#######################");
//        System.out.println("the number of nodes processed so far in the active branch-and-cut search: " + this.getNnodes());
//        System.out.println("the number of nodes remaining to be processed: " + this.getNremainingNodes());
//        System.out.println("totalpenalty: " + dataModel.getTotalPenalty());
        if(upperBound-Constants.EPSILON<=this.getObjValue()){
            return;
        }

        if (System.currentTimeMillis() - GlobalVariable.timeStart > GlobalVariable.timeLimit) {
            System.out.println("TIME OUT!!!!!");
            return;
        }

//        valueZ=this.getValue(mipData.varz);
        OptimalityCutGenerator cutGen = new OptimalityCutGenerator(dataModel, mipData, valuesQ, valueQ, valueZ,valuet, optimalityCuts, q);
        List<IloRange> optimalityCuts = cutGen.generateInqualities();
        for (IloRange optimalityCut : optimalityCuts) {
            this.add(optimalityCut);
        }

        long time = System.currentTimeMillis() - currentT;
//        System.out.println("cut generation time: " + time);


//        executor.shutdownNow();
    }


    public Solution getSolution() {
        List<Station> stations = new ArrayList<>();
        Solution solution = null;
        try {
            for (int s = 0; s < dataModel.getStationCandidates().size(); s++) {
                double value = this.getValue(mipData.varsLocation[s]);
                if (MathProgrammingUtil.doubleToBoolean(value)) {
                    Station station = (Station) dataModel.getStationCandidates().get(s);
                    station.setCapacity(this.getValue(mipData.varsCapacity[s]));
                    System.out.println(station);
                    stations.add(station);
                }
            }
            solution = new Solution(stations, dataModel.getStationCandidates().size());
//            mipData.cplex.end();
        } catch (IloException e) {
            e.printStackTrace();
        }

        return solution;
    }
}
