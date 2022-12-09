package benders.expr;

import benders.cg.column.AssignmentColumn_true;
import benders.master.Mip;
import benders.model.Solution;
import ilog.concert.IloException;
import io.Reader;
import ml.dmlc.xgboost4j.java.XGBoostError;
import model.Customer;
import model.Instance;
import model.Scenario;
import model.Station;
import model.Worker;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.random.JDKRandomGenerator;
import util.GlobalVariable;
import util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 2022/12/4 12:57
 */
public class PerformanceRiskNeutral {
    //linear regression model exact pricing
    public String exactLR(Instance instance, List<Scenario> scenarios) throws IloException {
        GlobalVariable.isExact = true;
        GlobalVariable.isDemandRecorded = false;
        Solution solution = null;
        instance.setScenarios(scenarios);
        double tp = 0;
        for (int xi = 0; xi < instance.getScenarios().size(); xi++) {
            Scenario scenario = instance.getScenarios().get(xi);
            tp += scenario.getProbability() * scenario.getDemandTotal() * instance.getUnservedPenalty();
        }
        instance.setTotalPenalty(tp);

        GlobalVariable.timeStart = System.currentTimeMillis();
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
            System.out.println("firstplussecond: " + (mip.mipData.firstStageObj + mip.mipData.secondStageObj));
            System.out.println("expectedObj: " + mip.mipData.expectedObj);
            System.out.println("CVaR: " + mip.mipData.CVaR);
            solution = mip.mipData.solution;

        } else {
            System.out.println("MIP infeasible!");
        }
        GlobalVariable.isExact = false;
        GlobalVariable.isDemandRecorded = true;
        String s = mip.getObjectiveValue() + " " + runTime + " " + mip.getLowerBound() + " " + mip.getNrOfNodes() + " " + mip.mipData.optimalityCuts.size() + " " + mip.mipData.firstStageObj + " " + mip.mipData.secondStageObj + " " + (mip.mipData.firstStageObj + mip.mipData.secondStageObj) + " " + mip.mipData.expectedObj + " " + mip.mipData.CVaR + "\n";
        return s;
    }


    //xgboost enumerate method
    public String saa(Instance instance, List<Scenario> scenarios) throws XGBoostError, IOException, IloException {
        double vs = 0;
        instance.setScenarios(scenarios);
        if (GlobalVariable.ENUMERATE) {
            Set<Pair<Station, Worker>> pairSWSet = Util.getAvailableSWPair(instance.getStationCandidates(), instance.getWorkers(), instance.getTravelCostMatrix());
            Map<Pair<Station, Worker>, Set<Customer>> unavailableSWPMap = Util.getUnavailablePairSWPMap(pairSWSet, instance.getCustomers(), instance.getTravelCostMatrix());
            GlobalVariable.columns = Util.generateJob(instance.getCustomers(), pairSWSet, unavailableSWPMap, instance, 0);
            //generate all possible columns and calculate the travel cost


            for (AssignmentColumn_true assignmentColumn_true : GlobalVariable.columns) {
                assignmentColumn_true.isDemandsSatisfy = new boolean[instance.getScenarios().size()];
                assignmentColumn_true.demands = new short[instance.getScenarios().size()];
                for (int xi = 0; xi < instance.getScenarios().size(); xi++) {

                    Scenario scenario = instance.getScenarios().get(xi);
                    short demand = Util.getDemand(assignmentColumn_true.customers, scenario);

                    assignmentColumn_true.isDemandsSatisfy[xi] = (demand <= scenario.getWorkerCapacity()[assignmentColumn_true.worker.getIndex()]);
                    assignmentColumn_true.demands[xi] = demand;
                }
            }
            System.out.println(GlobalVariable.columns.size());

        }

//        for(Scenario scenario:instance.getScenarios()){
//            System.out.println(scenario.getDemandTotal());
//        }

        double tp = 0;
        if (instance.isMultipleCut()) {
            instance.setPenalties(new double[instance.getScenarios().size()]);
            for (int xi = 0; xi < instance.getScenarios().size(); xi++) {
                Scenario scenario = instance.getScenarios().get(xi);
                double p = scenario.getDemandTotal() * instance.getUnservedPenalty();
                instance.getPenalties()[xi] = p;
                tp += scenario.getProbability() * p;
            }
        } else {
            for (int xi = 0; xi < instance.getScenarios().size(); xi++) {
                Scenario scenario = instance.getScenarios().get(xi);
                tp += scenario.getProbability() * scenario.getDemandTotal() * instance.getUnservedPenalty();
            }
        }

        instance.setTotalPenalty(tp);
        GlobalVariable.timeStart = System.currentTimeMillis();
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
            System.out.println("firstplussecond: " + (mip.mipData.firstStageObj + mip.mipData.secondStageObj));
            System.out.println("expectedObj: " + mip.mipData.expectedObj);
            System.out.println("CVaR: " + mip.mipData.CVaR);
            vs = mip.mipData.firstStageObj + mip.mipData.secondStageObj;
//            solution = mip.mipData.solution;

        } else {
            System.out.println("MIP infeasible!");
        }
        String s = mip.getObjectiveValue() + " " + runTime + " " + mip.isOptimal() + " " + mip.getLowerBound() + " " + mip.getNrOfNodes() + " " + mip.mipData.optimalityCuts.size() + " " + mip.mipData.firstStageObj + " " + mip.mipData.secondStageObj + " " + (mip.mipData.firstStageObj + mip.mipData.secondStageObj) + " " + mip.mipData.expectedObj + " " + mip.mipData.CVaR + "\n";
        if (GlobalVariable.ENUMERATE) {
            GlobalVariable.columns.clear();
        }
        return s;
    }

    //CVaR
    public double saaCVaR(Instance instance, List<Scenario> scenarios, double lambda, double alpha) throws XGBoostError, IOException, IloException {
        double vs = 0;
        instance.setCVaR(true);
        GlobalVariable.lambda = lambda;
        GlobalVariable.alpha = alpha;
        Set<Pair<Station, Worker>> pairSWSet = Util.getAvailableSWPair(instance.getStationCandidates(), instance.getWorkers(), instance.getTravelCostMatrix());
        Map<Pair<Station, Worker>, Set<Customer>> unavailableSWPMap = Util.getUnavailablePairSWPMap(pairSWSet, instance.getCustomers(), instance.getTravelCostMatrix());
        GlobalVariable.columns = Util.generateJob(instance.getCustomers(), pairSWSet, unavailableSWPMap, instance, 0);
        //generate all possible columns and calculate the travel cost

        instance.setScenarios(scenarios);
        for (AssignmentColumn_true assignmentColumn_true : GlobalVariable.columns) {
            assignmentColumn_true.isDemandsSatisfy = new boolean[instance.getScenarios().size()];
            assignmentColumn_true.demands = new short[instance.getScenarios().size()];
            for (int xi = 0; xi < instance.getScenarios().size(); xi++) {

                Scenario scenario = instance.getScenarios().get(xi);
                short demand = Util.getDemand(assignmentColumn_true.customers, scenario);

                assignmentColumn_true.isDemandsSatisfy[xi] = (demand <= scenario.getWorkerCapacity()[assignmentColumn_true.worker.getIndex()]);
                assignmentColumn_true.demands[xi] = demand;
            }
        }
//        for(Scenario scenario:instance.getScenarios()){
//            System.out.println(scenario.getDemandTotal());
//        }
        System.out.println(GlobalVariable.columns.size());

        double tp = 0;
        for (int xi = 0; xi < instance.getScenarios().size(); xi++) {
            Scenario scenario = instance.getScenarios().get(xi);
            tp += scenario.getProbability() * scenario.getDemandTotal() * instance.getUnservedPenalty();
        }
        instance.setTotalPenalty(tp);
        GlobalVariable.timeStart = System.currentTimeMillis();
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
            System.out.println("firstplussecond: " + (mip.mipData.firstStageObj + mip.mipData.secondStageObj));
            System.out.println("expectedObj: " + mip.mipData.expectedObj);
            System.out.println("CVaR: " + mip.mipData.CVaR);
            vs = mip.mipData.firstStageObj + mip.mipData.secondStageObj;
//            solution = mip.mipData.solution;

        } else {
            System.out.println("MIP infeasible!");
        }

        return vs;
    }

    public static void main(String[] args) throws IOException, IloException, XGBoostError {
        PerformanceRiskNeutral performanceRiskNeutral = new PerformanceRiskNeutral();
        File file = new File("dataset/instance/S30_W120_P40new.txt");
        GlobalVariable.isReadMatrix = true;
        BufferedWriter bf = new BufferedWriter(new FileWriter("output/expr/performanceRiskNeutral.txt", true));
        bf.write("Customer Sample Objective Runtime Isoptimal Bound Nodes Cuts firstStage secondStage firstplussecond expectedObj CVaR\n");

        bf.write("method1: cplex RL\n");
        bf.flush();
        for (int i = 1; i <= 4; i++) {
            Instance instance = Reader.readInstance(file, 1, 0, i * 5, i * 10, i * 20, 1);
            JDKRandomGenerator randomGenerator = new JDKRandomGenerator(0);
            for (int j = 1; j <= 4; j++) {
                List<Scenario> scenarios = Util.generateScenarios(instance, 0.5, 0.5, j * 25, randomGenerator);
                //method 1
                String s = i * 10 + " " + j * 25 + " " + performanceRiskNeutral.exactLR(instance, scenarios);
                bf.write(s);
                bf.flush();
            }

        }

        bf.write("method2: enumerate\n");
        bf.flush();
        for (int i = 1; i <= 4; i++) {
            Instance instance = Reader.readInstance(file, 1, 0, i * 5, i * 10, i * 20, 1);
            JDKRandomGenerator randomGenerator = new JDKRandomGenerator(0);
            for (int j = 1; j <= 4; j++) {
                List<Scenario> scenarios = Util.generateScenarios(instance, 0.5, 0.5, j * 25, randomGenerator);
                //method 2
                GlobalVariable.ENUMERATE = false;
                GlobalVariable.isDemandRecorded = false;
                String s = i * 10 + " " + j * 25 + " " + performanceRiskNeutral.saa(instance, scenarios);
                bf.write(s);
                bf.flush();


            }

        }
        bf.write("method3: enumerate memory\n");
        bf.flush();
        for (int i = 1; i <= 4; i++) {
            Instance instance = Reader.readInstance(file, 1, 0, i * 5, i * 10, i * 20, 1);
            JDKRandomGenerator randomGenerator = new JDKRandomGenerator(0);
            for (int j = 1; j <= 4; j++) {
                List<Scenario> scenarios = Util.generateScenarios(instance, 0.5, 0.5, j * 25, randomGenerator);
                //method 3
                GlobalVariable.ENUMERATE = true;
                GlobalVariable.isDemandRecorded = true;
                String s = i * 10 + " " + j * 25 + " " + performanceRiskNeutral.saa(instance, scenarios);
                bf.write(s);
                bf.flush();
            }

        }
        bf.write("method4: enumerate memory multiCut\n");
        bf.flush();
        for (int i = 1; i <= 4; i++) {
            Instance instance = Reader.readInstance(file, 1, 0, i * 5, i * 10, i * 20, 1);
            JDKRandomGenerator randomGenerator = new JDKRandomGenerator(0);
            for (int j = 1; j <= 4; j++) {
                List<Scenario> scenarios = Util.generateScenarios(instance, 0.5, 0.5, j * 25, randomGenerator);
                //method 4
                instance.setMultipleCut(true);
                String s = i * 10 + " " + j * 25 + " " + performanceRiskNeutral.saa(instance, scenarios);
                bf.write(s);
                bf.flush();
            }

        }

        bf.close();

    }

}