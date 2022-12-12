package benders.expr;

import benders.cg.LocationAssignmentCGSolver;
import benders.cg.column.AssignmentColumn_true;
import benders.master.Mip;
import benders.model.LocationAssignment;
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
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import util.Constants;
import util.GlobalVariable;
import util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Wang Li
 * @description the cost difference for the solutions obtained from the stocastic model and the deterministic model, that is
 * built by the expected scenario
 * @date 2022/12/4 20:35
 */
public class VSS {
    Solution solution = null;


    public double vs(Instance instance, List<Scenario> scenarios) throws XGBoostError, IOException, IloException {
        double vs = 0;
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
            solution = mip.mipData.solution;

        } else {
            System.out.println("MIP infeasible!");
        }

        return vs;
    }

    public double vSingleScenario(Instance instance, Scenario scenario) throws IloException, XGBoostError, IOException {
        double vdet = 0;
        List<Scenario> scenarios = Collections.singletonList(scenario);
//        Set<Pair<Station, Worker>> pairSWSet = Util.getAvailableSWPair(instance.getStationCandidates(), instance.getWorkers(), instance.getTravelCostMatrix());
//        Map<Pair<Station, Worker>, Set<Customer>> unavailableSWPMap = Util.getUnavailablePairSWPMap(pairSWSet, instance.getCustomers(), instance.getTravelCostMatrix());
//        GlobalVariable.columns = Util.generateJob(instance.getCustomers(), pairSWSet, unavailableSWPMap, instance, 0);

        instance.setScenarios(scenarios);
        for (AssignmentColumn_true assignmentColumn_true : GlobalVariable.columns) {
            assignmentColumn_true.isDemandsSatisfy = new boolean[instance.getScenarios().size()];
            assignmentColumn_true.demands = new short[instance.getScenarios().size()];
            for (int xi = 0; xi < instance.getScenarios().size(); xi++) {

                Scenario scenario0 = instance.getScenarios().get(xi);
                short demand = Util.getDemand(assignmentColumn_true.customers, scenario0);

                assignmentColumn_true.isDemandsSatisfy[xi] = (demand <= scenario0.getWorkerCapacity()[assignmentColumn_true.worker.getIndex()]);
                assignmentColumn_true.demands[xi] = demand;
            }
        }

        double tp = 0;
        for (int xi = 0; xi < instance.getScenarios().size(); xi++) {
            Scenario scenario0 = instance.getScenarios().get(xi);
            tp += scenario0.getProbability() * scenario0.getDemandTotal() * instance.getUnservedPenalty();
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
            vdet = mip.mipData.firstStageObj + mip.mipData.secondStageObj;
            solution = mip.mipData.solution;

        } else {
            System.out.println("MIP infeasible!");
        }


        return vdet;
    }

    public Scenario getDeterminantScenario(Instance instance) {
        List<Worker> availableWorkers = new ArrayList<>();
        Scenario scenario = new Scenario();
        int[] isWorkerAvailable = new int[instance.getWorkers().size()];
        int[] customerDemand = new int[instance.getCustomers().size()];
        int[] workerCapacity = new int[instance.getWorkers().size()];
        Arrays.fill(isWorkerAvailable, 1);
        availableWorkers.addAll(instance.getWorkers());
        int totalD = 0;
        for (int j = 0; j < instance.getCustomers().size(); j++) {
            customerDemand[j] = instance.getCustomers().get(j).getDemandExpected();
            totalD += customerDemand[j];
        }
        for (int j = 0; j < instance.getWorkers().size(); j++) {
            workerCapacity[j] = instance.getWorkers().get(j).getCapacity();
        }


        scenario.setIndex(0);
        scenario.setIsWorkerAvailable(isWorkerAvailable);
        scenario.setCustomerDemand(customerDemand);
        scenario.setAvailableWorkers(availableWorkers);
        scenario.setWorkerCapacity(workerCapacity);
        scenario.setProbability(1);
        scenario.setDemandTotal(totalD);
        return scenario;
    }


    public static void main(String[] args) throws IOException, XGBoostError, IloException {
        VSS vss = new VSS();
        File file = new File("dataset/instance/S30_W120_P40new.txt");
        GlobalVariable.isReadMatrix = true;


        Instance instance = Reader.readInstance(file, 50, 0, 10, 20, 40, 1);

        JDKRandomGenerator randomGenerator = new JDKRandomGenerator(17);
        List<Scenario> scenarios = Util.generateScenarios(instance, 0.5, 0.5, 50, randomGenerator);
        instance.setMultipleCut(true);
        GlobalVariable.isDemandTricky=true;
        double vs = vss.vs(instance, scenarios);
        Scenario scenario = vss.getDeterminantScenario(instance);
        double vDET0 = vss.vSingleScenario(instance, scenario);
        double vDET =Util.evaluate(instance, vss.solution, scenarios);
        double vDET2 =0;
        for(int xi=0;xi<scenarios.size();xi++){
            Scenario scenario2=scenarios.get(xi);
            double pro=scenario2.getProbability();
            scenario2.setIndex(0);
            scenario2.setProbability(1);
            double vDET02= vss.vSingleScenario(instance, scenario2);
            scenario2.setIndex(xi);
            scenario2.setProbability(pro);
            vDET2+= Util.evaluate(instance, vss.solution, scenarios);
        }
        vDET2/=scenarios.size();
        System.out.println("vs: " + vs);
        System.out.println("vDET: " + vDET);
        System.out.println("vDET2: " + vDET);
        double gap = (vDET - vs) / vDET * 100;
        double gap2 = (vDET2 - vs) / vDET2 * 100;
        System.out.println("relative gap: " + gap);
        System.out.println("relative gap2: " + gap2);
    }

}
