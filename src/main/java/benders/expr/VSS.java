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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
    double vs = 0;


    public String vs(Instance instance, List<Scenario> scenarios) throws XGBoostError, IOException, IloException {
//        double vs = 0;
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

        int stationNum = 0;
        double capacity = 0;
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
            vs = mip.getFirstStageObj() + mip.getSecondStageObj();
            List<Station> stations = mip.getSolution();
            stationNum = stations.size();
            solution = new Solution(stations, instance.getStationCandidates().size());
            for (Station station : stations) {
                capacity += station.getCapacity();
            }


        } else {
            System.out.println("MIP infeasible!");
        }
        String evaluate = Util.evaluateSensitivity(instance, solution, scenarios);
        double gap = (mip.getObjectiveValue() - mip.getLowerBound()) / mip.getObjectiveValue() * 100;
        String s = stationNum + " " + capacity + " " + evaluate + " " + mip.getFirstStageObj() + " " + mip.getSecondStageObj() + " " + vs + " " + runTime + " " + gap + " ";
        mip.mipData.cplex.end();
        return s;
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
//            vdet = mip.mipData.firstStageObj + mip.mipData.secondStageObj;
//            solution = mip.mipData.solution;
            vdet = mip.getFirstStageObj() + mip.getSecondStageObj();
            solution = new Solution(mip.getSolution(), instance.getStationCandidates().size());

        } else {
            System.out.println("MIP infeasible!");
        }

        mip.mipData.cplex.end();
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


        Instance instance = Reader.readInstance(file, 50, 0, 10, 20, 40, 0.5);

        JDKRandomGenerator randomGenerator = new JDKRandomGenerator(17);
        for (int i = 0; i <= 4; i++) {
            List<Scenario> scenarios = Util.generateScenarios(instance, i * 0.25, i * 0.25, 50, randomGenerator);
            instance.setMultipleCut(true);
            GlobalVariable.isDemandTricky = true;
            String vs = vss.vs(instance, scenarios);


            Scenario scenario = vss.getDeterminantScenario(instance);
            double vDET0 = vss.vSingleScenario(instance, scenario);
            double vDET = Util.evaluate(instance, vss.solution, scenarios);
            double vDET2 = 0;
            double vPI = 0;
            double vMax = -Double.MAX_VALUE;
            double vMin = Double.MAX_VALUE;

            for (int xi = 0; xi < scenarios.size(); xi++) {
                Scenario scenario2 = scenarios.get(xi);
                double pro = scenario2.getProbability();
                scenario2.setIndex(0);
                scenario2.setProbability(1);
                vPI += vss.vSingleScenario(instance, scenario2);
                scenario2.setIndex(xi);
                scenario2.setProbability(pro);
                double vd = Util.evaluate(instance, vss.solution, scenarios);
                if (vMax < vd) {
                    vMax = vd;
                }
                if (vMin > vd) {
                    vMin = vd;
                }
                vDET2 += vd;
            }
            vDET2 /= scenarios.size();
            vPI /= scenarios.size();
            System.out.println("vs: " + vs);
            System.out.println("vPI: " + vPI);
            System.out.println("vDET: " + vDET);
            System.out.println("vDET2: " + vDET);
            System.out.println("vMax: " + vMax);
            System.out.println("vMin: " + vMin);
            double gap = (vDET - vss.vs) / vDET * 100;
            double gap2 = (vDET2 - vss.vs) / vDET2 * 100;
            double gap4 = (vMax - vss.vs) / vMax * 100;
            double gap5 = (vMin - vss.vs) / vMin * 100;
            double gap3 = (vss.vs - vPI) / vss.vs * 100;
            System.out.println("relative gap: " + gap);
            System.out.println("relative gap2: " + gap2);
            System.out.println("relative gap3: " + gap3);

            String s = vPI + " " + gap3 + " " + vDET + " " + gap + " " + vDET2 + " " + gap2 + " " + vMax + " " + gap4 + " " + vMin + " " + gap5 + " ";
            s += vs+"\n";
            BufferedWriter bf = new BufferedWriter(new FileWriter("output/expr/vss.txt", true));
            bf.write(s);
            bf.flush();
        }

    }

}
