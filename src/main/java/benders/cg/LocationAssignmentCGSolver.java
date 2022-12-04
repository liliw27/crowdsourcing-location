package benders.cg;

import benders.cg.column.AssignmentColumn_true;
import benders.cg.masterProblem.Master;
import benders.cg.pricing.ExactPricingProblemSolver;
import benders.cg.pricing.HeuristicPricingProblemSolver;
import benders.cg.pricing.PricingProblem;
import benders.model.LocationAssignment;
import model.Customer;
import model.Scenario;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.colgenMain.ColGen;
import org.jorlib.frameworks.columnGeneration.io.SimpleCGLogger;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import util.GlobalVariable;
import util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static util.Util.getDemand;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/16 21:01
 */
public class LocationAssignmentCGSolver implements Callable<Void> {
    private final LocationAssignment locationAssignment;
    private int initialObj;
    List<PricingProblem> pricingProblems = new ArrayList<>();
    List<AssignmentColumn_true> solutionCG;
    int upperBound;
    Scenario scenario;


    private double objectiveValue;

    public LocationAssignmentCGSolver(LocationAssignment locationAssignment, Scenario scenario) {
        this.locationAssignment = locationAssignment;
        this.scenario = scenario;
    }

    public Map<String, double[]> getDualCostsMap() {
        return pricingProblems.get(0).dualCostsMap;
//        return master.dualMap;
    }

    public double getObjectiveValue() {
        return objectiveValue;
    }

    public void solveCG() {
        //Create Pricing problems

        for (Worker worker : scenario.getAvailableWorkers()) {
            PricingProblem pricingProblem = new PricingProblem(locationAssignment, worker, "pricingProblem_" + worker.getIndex(), scenario);
            pricingProblems.add(pricingProblem);
        }
        //Create the Master Problem
        GlobalVariable.stationNum = locationAssignment.instance.getStationCandidates().size();
        GlobalVariable.typeNum = locationAssignment.instance.getType().length;
        Master master = new Master(locationAssignment, pricingProblems, scenario);
        //Define which solvers to use for the pricing problem
        List<Class<? extends AbstractPricingProblemSolver<LocationAssignment, AssignmentColumn_true, PricingProblem>>> solvers = new ArrayList<>();
        if (GlobalVariable.isExact) {
            solvers.add(ExactPricingProblemSolver.class);
        } else {
            solvers.add(HeuristicPricingProblemSolver.class);

        }
        long runTime = System.currentTimeMillis();
        //Optional: Get an initial solution
        List<AssignmentColumn_true> initSolution = this.getInitialSolution(locationAssignment, pricingProblems);
        upperBound = initialObj;

        //Optional: Get a lower bound on the optimum solution, e.g. largest clique in the graph
        int lowerBound = this.calculateLowerBound();

        ColGen<LocationAssignment, AssignmentColumn_true, PricingProblem> cg = new ColGen<>(locationAssignment, master, pricingProblems, solvers, initSolution, upperBound, lowerBound);

        //OPTIONAL: Attach a debugger
//        new SimpleDebugger(cg, true);

        //OPTIONAL: Attach a logger to the Branch-and-Price procedure.
        SimpleCGLogger logger = new SimpleCGLogger(cg, new File("./output/Log/crowdSourcing" + locationAssignment.instance.getName() + ".log"));

        //Solve the Graph Coloring problem through Branch-and-Price
        try {
            cg.solve(System.currentTimeMillis() + 1200000L);//7200000L
//            System.out.println("run column generation"+(System.currentTimeMillis()-runTime));
        } catch (TimeLimitExceededException e) {
            e.printStackTrace();
        }
        objectiveValue = cg.getObjective();
//        System.out.println("================ Solution ================");
        solutionCG = cg.getSolution();
        for (AssignmentColumn_true assignmentColumn : solutionCG) {
            System.out.println(assignmentColumn);
        }
//        System.out.println("CG"+scenario.getIndex()+" terminated with objective: " + cg.getObjective());
//        System.out.println("Number of iterations: " + cg.getNumberOfIterations());
//        System.out.println("Time spent on master: " + cg.getMasterSolveTime() + " time spent on pricing: " + cg.getPricingSolveTime());
//        System.out.println("upper bound: " + upperBound);

        System.out.println("CG for scenario " + scenario.getIndex() + "; obj: " + cg.getObjective() + "; iter: " + cg.getNumberOfIterations() + "; masterTime: " + cg.getMasterSolveTime() + ";pricingTime: " + cg.getPricingSolveTime());
        String s = "objective and solving time: " + cg.getObjective() + " " + (cg.getMasterSolveTime() + cg.getPricingSolveTime()) + "\niteration number: " + cg.getNumberOfIterations() + "\nmaster solve time: " + cg.getMasterSolveTime() + "\npricing solve time: " + cg.getPricingSolveTime();
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("./output/solutions/prodCG/" + locationAssignment.instance.getName() + ".txt"));
            writer.write(s);
            writer.flush();
        } catch (IOException e) {
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
            }
        }

        //Clean up:
        cg.close(); //This closes both the master and pricing problems

    }


    //------------------ Helper methods -----------------

    /**
     * Calculate a feasible solution using a greedy algorithm.
     *
     * @param locationAssignment
     * @return Feasible solution.
     */
    public List<AssignmentColumn_true> getInitialSolution(LocationAssignment locationAssignment, List<PricingProblem> pricingProblems) {

        //todo
        List<AssignmentColumn_true> assignmentColumns = new ArrayList<>();
        int stationIndex = 0;
        int accumulatedOccupiedCapS = 0;
        int workerIndex = 0;
        int accumulatedOccupiedCapW = 0;
        Worker worker = scenario.getAvailableWorkers().get(workerIndex);
        StationCandidate stationCandidate = locationAssignment.instance.getStationCandidates().get(stationIndex);
        Set<Customer> customers = new HashSet<>();
        double obj = stationCandidate.getFixedCost() + stationCandidate.getCapacityCost() * locationAssignment.capacity[stationCandidate.getIndex()];

        for (Customer customer : locationAssignment.instance.getCustomers()) {
            int accuS = accumulatedOccupiedCapS + scenario.getCustomerDemand()[customer.getIndex()];
            int accuW = accumulatedOccupiedCapW + scenario.getCustomerDemand()[customer.getIndex()];
//            if (accuS <= locationAssignment.capacity[stationCandidate.getIndex()] && accuW <= scenario.getWorkerCapacity()[worker.getIndex()] && customers.size() < locationAssignment.instance.getWorkerCapacityNum()) {
            if (accuS <= 100 && accuW <= scenario.getWorkerCapacity()[worker.getIndex()] && customers.size() < locationAssignment.instance.getWorkerCapacityNum()) {
                accumulatedOccupiedCapS = accuS;
                accumulatedOccupiedCapW = accuW;
                customers.add(customer);
            } else if (accuS > locationAssignment.capacity[stationCandidate.getIndex()]) {
                if (customers.size() > 0) {
                    double cost = Util.getCostE(customers, worker, stationCandidate, locationAssignment.instance, scenario);
                    AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblems.get(workerIndex), false, "initial", cost, getDemand(customers, scenario), worker, customers, stationCandidate);
                    assignmentColumns.add(assignmentColumn_true);
                    obj += cost;
                    obj += stationCandidate.getFixedCost() + stationCandidate.getCapacityCost() * locationAssignment.capacity[stationCandidate.getIndex()];
                    accumulatedOccupiedCapS = scenario.getCustomerDemand()[customer.getIndex()];
                    accumulatedOccupiedCapW = scenario.getCustomerDemand()[customer.getIndex()];
                }
                stationIndex++;
                workerIndex++;
                if (workerIndex == scenario.getAvailableWorkers().size()) {
                    break;
                }
                if (stationIndex == locationAssignment.instance.getStationCandidates().size()) {
                    break;
                }
                worker = scenario.getAvailableWorkers().get(workerIndex);
                stationCandidate = locationAssignment.instance.getStationCandidates().get(stationIndex);
                customers = new HashSet<>();
                customers.add(customer);
            } else if (accuW > scenario.getWorkerCapacity()[worker.getIndex()] || customers.size() >= locationAssignment.instance.getWorkerCapacityNum()) {
                if (customers.size() > 0) {
                    double cost = Util.getCostE(customers, worker, stationCandidate, locationAssignment.instance, scenario);
                    AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblems.get(workerIndex), false, "initial", cost, getDemand(customers, scenario), worker, customers, stationCandidate);
                    assignmentColumns.add(assignmentColumn_true);
                    obj += cost;
                    accumulatedOccupiedCapS = accuS;
                    accumulatedOccupiedCapW = scenario.getCustomerDemand()[customer.getIndex()];
                }
                workerIndex++;
                if (workerIndex == scenario.getAvailableWorkers().size()) {
                    break;
                }
                worker = scenario.getAvailableWorkers().get(workerIndex);
                customers = new HashSet<>();
                customers.add(customer);
            }
            if (customer.getIndex() == locationAssignment.instance.getCustomers().size() - 1) {
                double cost = Util.getCostE(customers, worker, stationCandidate, locationAssignment.instance, scenario);
                AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblems.get(workerIndex), false, "initial", cost, getDemand(customers, scenario), worker, customers, stationCandidate);
                assignmentColumns.add(assignmentColumn_true);
                obj += cost;
            }

        }
        if (GlobalVariable.isDemandRecorded) {
            for (AssignmentColumn_true assignmentColumn_true : assignmentColumns) {
                assignmentColumn_true.isDemandsSatisfy = new boolean[locationAssignment.instance.getScenarios().size()];
                assignmentColumn_true.demands = new short[locationAssignment.instance.getScenarios().size()];
                for (int xi = 0; xi < locationAssignment.instance.getScenarios().size(); xi++) {
                    Scenario scenario = locationAssignment.instance.getScenarios().get(xi);
                    short demand = Util.getDemand(assignmentColumn_true.customers, scenario);

                    assignmentColumn_true.isDemandsSatisfy[xi] = (demand <= scenario.getWorkerCapacity()[assignmentColumn_true.worker.getIndex()]);
                    assignmentColumn_true.demands[xi] = demand;
                }
            }
        }

        initialObj = (int) obj;
        return assignmentColumns;
    }


    /**
     * Calculate a lower bound on the chromatic number of a graph, by calculating the largest clique in the graph.
     *
     * @return lower bound
     */
    private int calculateLowerBound() {
        int lowerbound = 0;
        for (Customer customer : locationAssignment.instance.getCustomers()) {
            lowerbound -= scenario.getDemandTotal() * customer.getUnservedPenalty() * GlobalVariable.daysNum;
        }
        //todo:design a lower bound calculator
        return lowerbound;
    }


    @Override
    public Void call() throws Exception {
        solveCG();
        return null;
    }
}
