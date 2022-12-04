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
import model.StationCandidate;
import model.Worker;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.random.AbstractRandomGenerator;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import util.Constants;
import util.GlobalVariable;
import util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
 * @date 2022/11/28 22:35
 */
public class SampleSize {

    int N = 200;
    int M = 25;
//    List<Scenario>[] scenarioBatches = new List<>[M];


    Solution[] solutionsForLower = new Solution[M];

    double LNM;
    double lb1;
    double lb2;

    double UB;
    double ub1;
    double ub2;
    JDKRandomGenerator randomGenerator = new JDKRandomGenerator(0);

    public List<Scenario>[] getScenarioBatches(Instance instance, int N) {
        List<Scenario>[] scenarioBatches = new ArrayList[M];
        double coeC = 0.5;
        double coeW = 0.5;
        GammaDistribution[] gammacustomers = new GammaDistribution[instance.getCustomers().size()];
        GammaDistribution[] gammaworkers = new GammaDistribution[instance.getWorkers().size()];
        for (Customer customer : instance.getCustomers()) {
            double shape = 1 / (coeC * coeC);
            double scale = coeC * coeC * customer.getDemandExpected();
            gammacustomers[customer.getIndex()] = new GammaDistribution(randomGenerator, shape, scale);
        }
        for (Worker worker : instance.getWorkers()) {
            double shape = 1 / (coeW * coeW);
            double scale = coeW * coeW * worker.getCapacity();
            gammaworkers[worker.getIndex()] = new GammaDistribution(randomGenerator, shape, scale);
        }
        for (int m = 0; m < M; m++) {
            List<Scenario> scenarios = new ArrayList<>();
            scenarioBatches[m] = scenarios;
            double prob = 1.0 / N;
            for (int i = 0; i < N; i++) {
                List<Worker> availableWorkers = new ArrayList<>();
                Scenario scenario = new Scenario();
                int[] isWorkerAvailable = new int[instance.getWorkers().size()];
                int[] customerDemand = new int[instance.getCustomers().size()];
                int[] workerCapacity = new int[instance.getWorkers().size()];
                Arrays.fill(isWorkerAvailable, 1);
                availableWorkers.addAll(instance.getWorkers());
                int maxValue = Integer.MIN_VALUE;
                int totalD = 0;
                for (int j = 0; j < instance.getCustomers().size(); j++) {
                    customerDemand[j] = (int) gammacustomers[j].sample();
                    totalD += customerDemand[j];
                }
                for (int j = 0; j < instance.getWorkers().size(); j++) {
                    workerCapacity[j] = (int) gammaworkers[j].sample();
                }


                scenario.setIndex(i);
                scenario.setIsWorkerAvailable(isWorkerAvailable);
                scenario.setCustomerDemand(customerDemand);
                scenario.setAvailableWorkers(availableWorkers);
                scenario.setWorkerCapacity(workerCapacity);
                scenario.setProbability(prob);
                scenario.setDemandTotal(totalD);
                scenarios.add(scenario);
            }

        }
        return scenarioBatches;
    }


    public List<Scenario> getScenarioEvaluation(Instance instance) {
        List<Scenario> scenarioEvaluation = new ArrayList<>();
        double coeC = 0.5;
        double coeW = 0.5;
        GammaDistribution[] gammacustomers = new GammaDistribution[instance.getCustomers().size()];
        GammaDistribution[] gammaworkers = new GammaDistribution[instance.getWorkers().size()];
        for (Customer customer : instance.getCustomers()) {
            double shape = 1 / (coeC * coeC);
            double scale = coeC * coeC * customer.getDemandExpected();
            gammacustomers[customer.getIndex()] = new GammaDistribution(randomGenerator, shape, scale);
        }
        for (Worker worker : instance.getWorkers()) {
            double shape = 1 / (coeW * coeW);
            double scale = coeW * coeW * worker.getCapacity();
            gammaworkers[worker.getIndex()] = new GammaDistribution(randomGenerator, shape, scale);
        }
        for (int m = 0; m < 10000; m++) {

            double prob = 1.0 / 10000;

            List<Worker> availableWorkers = new ArrayList<>();
            Scenario scenario = new Scenario();
            int[] isWorkerAvailable = new int[instance.getWorkers().size()];
            int[] customerDemand = new int[instance.getCustomers().size()];
            int[] workerCapacity = new int[instance.getWorkers().size()];
            Arrays.fill(isWorkerAvailable, 1);
            availableWorkers.addAll(instance.getWorkers());
            int maxValue = Integer.MIN_VALUE;
            int totalD = 0;
            for (int j = 0; j < instance.getCustomers().size(); j++) {
                customerDemand[j] = (int) gammacustomers[j].sample();
                totalD += customerDemand[j];
            }
            for (int j = 0; j < instance.getWorkers().size(); j++) {
                workerCapacity[j] = (int) gammaworkers[j].sample();
            }


            scenario.setIndex(m);
            scenario.setIsWorkerAvailable(isWorkerAvailable);
            scenario.setCustomerDemand(customerDemand);
            scenario.setAvailableWorkers(availableWorkers);
            scenario.setWorkerCapacity(workerCapacity);
            scenario.setProbability(prob);
            scenario.setDemandTotal(totalD);
            scenarioEvaluation.add(scenario);


        }
        return scenarioEvaluation;
    }


    public void getLowerBound(Instance instance) throws XGBoostError, IOException, IloException {
        Set<Pair<Station, Worker>> pairSWSet = Util.getAvailableSWPair(instance.getStationCandidates(), instance.getWorkers(), instance.getTravelCostMatrix());
        Map<Pair<Station, Worker>, Set<Customer>> unavailableSWPMap = Util.getUnavailablePairSWPMap(pairSWSet, instance.getCustomers(), instance.getTravelCostMatrix());
        GlobalVariable.columns = Util.generateJob(instance.getCustomers(), pairSWSet, unavailableSWPMap, instance, 0);
        List<Scenario>[] scenarioBatches = getScenarioBatches(instance, N);
        double[] lbForEachM = new double[M];
        //generate all possible columns and calculate the travel cost
        for (int m = 0; m < M; m++) {
            instance.setScenarios(scenarioBatches[m]);
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
                lbForEachM[m] = mip.mipData.firstStageObj + mip.mipData.secondStageObj;
                solutionsForLower[m] = mip.mipData.solution;

            } else {
                System.out.println("MIP infeasible!");
            }

        }
        double avg = 0;
        for (int m = 0; m < M; m++) {
            avg += lbForEachM[m];
        }
        avg = avg / M;
        LNM = avg;

        double std = 0;

        for (int m = 0; m < M; m++) {
            std += (lbForEachM[m] - avg) * (lbForEachM[m] - avg);
        }
        std = std / M;
        std = std / (M - 1);
        std = Math.sqrt(std);
        double zscore = 1.96;
        lb1 = LNM - zscore * std;
        lb2 = LNM + zscore * std;
    }


    public Solution getEvaluatedSolution(Instance instance) throws XGBoostError, IOException, IloException {
        Solution solutionBest = solutionsForLower[0];
        double bestObj = Double.MAX_VALUE;
        List<Scenario>[] scenarioBatchesEval = getScenarioBatches(instance, 2 * N);
        for (int i = 0; i < M; i++) {

            getUpperBound(instance, solutionsForLower[i], scenarioBatchesEval[i]);

            if (bestObj > UB) {
                bestObj = UB;
                solutionBest = solutionsForLower[i];
            }
        }
        return solutionBest;
    }

    public void getUpperBound(Instance instance, Solution solution, List<Scenario> scenarioEvaluation) throws XGBoostError, IOException, IloException {
        instance.setScenarios(scenarioEvaluation);
        ExecutorService executor = Executors.newFixedThreadPool(Constants.MAXTHREADS);
        List<Future<Void>> futures = new ArrayList<>(scenarioEvaluation.size());
        //generate all possible columns and calculate the travel cost
        List<LocationAssignmentCGSolver> cgSolvers = new ArrayList<>(scenarioEvaluation.size());
        double[] objForEachScenario = new double[scenarioEvaluation.size()];
        for (int m = 0; m < scenarioEvaluation.size(); m++) {
            Scenario scenario = scenarioEvaluation.get(m);

            LocationAssignment locationAssignment = new LocationAssignment(instance, solution.getCapacity());
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
        double avg = 0;

        for (int i = 0; i < cgSolvers.size(); i++) {
            LocationAssignmentCGSolver cgSolver = cgSolvers.get(i);
            objForEachScenario[i] = cgSolver.getObjectiveValue() + instance.getUnservedPenalty() * scenarioEvaluation.get(i).getDemandTotal();
            avg += objForEachScenario[i];
        }
        avg = avg / scenarioEvaluation.size();


        double std = 0;

        for (int m = 0; m < M; m++) {
            std += (objForEachScenario[m] - avg) * (objForEachScenario[m] - avg);
        }
        std = std / scenarioEvaluation.size();
        std = std / (scenarioEvaluation.size() - 1);
        std = Math.sqrt(std);
        double zscore = 1.96;
        UB = solution.getFirstStageObj() + avg;
        ub1 = UB - zscore * std;
        ub2 = UB + zscore * std;
    }

    public double getGap() {
        double gap = (ub2 - lb1) / ub2 * 100;
        return gap;
    }

    public static void main(String[] args) throws IOException, XGBoostError, IloException {
        SampleSize sampleSize = new SampleSize();
        File file = new File("dataset/instance/S30_W120_P40new.txt");
        GlobalVariable.isReadMatrix = true;
        BufferedWriter bf = new BufferedWriter(new FileWriter("output/expr/sampleSize.txt", true));
        bf.write("Customer Sample LNB lb1 lb2 UB ub1 ub2 gap\n");

        for (int i = 1; i <= 3; i++) {
            Instance instance = Reader.readInstance(file, 50, 0, 5 * i, 10 * i, 20 * i, 1);

            for (int j = 1; j <= 4; j++) {
                sampleSize.N = j * 25;
                instance.setMultipleCut(true);
                sampleSize.getLowerBound(instance);
                GlobalVariable.isDemandRecorded = false;
                Solution solution = sampleSize.getEvaluatedSolution(instance);
                List<Scenario> scenarioEvaluation = sampleSize.getScenarioEvaluation(instance);
                sampleSize.getUpperBound(instance, solution, scenarioEvaluation);
                double gap = sampleSize.getGap();
                String s = i * 10 + " " + j * 25 + " " + sampleSize.LNM + " " + sampleSize.lb1 + " " + sampleSize.lb2 + " " + sampleSize.UB + " " + sampleSize.ub1 + " " + sampleSize.ub2 + " " + gap;
                bf.write(s);
                bf.flush();
            }
        }


    }

}

