package benders;

import benders.cg.column.AssignmentColumn_true;
import benders.master.Mip;
import ilog.concert.IloException;
import io.Reader;
import ml.dmlc.xgboost4j.java.XGBoostError;
import model.Customer;
import model.Instance;
import model.Scenario;
import model.Station;
import model.Worker;
import org.apache.commons.lang3.tuple.Pair;
import util.GlobalVariable;
import util.Util;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 2022/11/17 20:57
 */
public class BendersSolver {
    public static void main(String[] args) throws IOException, IloException, XGBoostError {
        File file = new File("dataset/instance/instance");

        Instance instance = Reader.readInstance(file, 50, 0, 20, 30, 20, 1);
        //generate all possible columns and calculate the travel cost
        int it = 0;
        Set<Pair<Station, Worker>> pairSWSet = Util.getAvailableSWPair(instance.getStationCandidates(), instance.getWorkers(), instance.getTravelCostMatrix());
        Map<Pair<Station, Worker>, Set<Customer>> unavailableSWPMap = Util.getUnavailablePairSWPMap(pairSWSet, instance.getCustomers(), instance.getTravelCostMatrix());
        GlobalVariable.columns = Util.generateJob(instance.getCustomers(), pairSWSet, unavailableSWPMap, instance, it);
        for (AssignmentColumn_true assignmentColumn_true : GlobalVariable.columns) {
            for (int xi = 0; xi < instance.getScenarios().size(); xi++) {

                Scenario scenario = instance.getScenarios().get(xi);
                short demand = Util.getDemand(assignmentColumn_true.customers, scenario);

                assignmentColumn_true.isDemandsSatisfy[xi] = (demand <= scenario.getWorkerCapacity()[assignmentColumn_true.worker.getIndex()]);
                assignmentColumn_true.demands[xi] = demand ;
            }
        }
//        for(Scenario scenario:instance.getScenarios()){
//            System.out.println(scenario.getDemandTotal());
//        }
        System.out.println(GlobalVariable.columns.size());

        double tp=0;
        for(int xi=0;xi<instance.getScenarios().size();xi++){
            Scenario scenario=instance.getScenarios().get(xi);
            tp+=scenario.getProbability()*scenario.getDemandTotal()*instance.getUnservedPenalty();
        }
        instance.setTotalPenalty(tp);

        GlobalVariable.timeStart=System.currentTimeMillis();
        Mip mip = new Mip(instance);

        long runTime = System.currentTimeMillis();
        System.out.println("Starting branch and bound for " + instance.getName());
        mip.solve();
        runTime = System.currentTimeMillis() - runTime;

        if (mip.isFeasible()) {

            System.out.println("Objective: " + mip.getObjectiveValue());
            System.out.println("Runtime: " + runTime);
            System.out.println("Is optimal: " + mip.isOptimal());
            System.out.println("Bound: " + mip.getLowerBound());
            System.out.println("Nodes: " + mip.getNrOfNodes());
            System.out.println("firstStage: " + mip.mipData.firstStageObj);
            System.out.println("secondStage: " + mip.mipData.secondStageObj);
            System.out.println("firstplussecond: " + (mip.getSecondStageObj()+mip.getFirstStageObj()));
            System.out.println("expectedObj: " + mip.getExpectedObj());
            System.out.println("CVaR: " + mip.getCVaR());


        } else {
            System.out.println("MIP infeasible!");
        }

    }
}
