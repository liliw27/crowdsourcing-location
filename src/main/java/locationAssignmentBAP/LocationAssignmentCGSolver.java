package locationAssignmentBAP;

import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.cg.masterProblem.Master;
import locationAssignmentBAP.cg.pricing.HeuristicPricingProblemSolver;
import locationAssignmentBAP.cg.pricing.PricingProblem;
import locationAssignmentBAP.model.LocationAssignment;
import model.Customer;
import model.Instance;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static util.Util.getDemand;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/16 21:01
 */
public class LocationAssignmentCGSolver {
    private final LocationAssignment locationAssignment;
    private int initialObj;
    List<PricingProblem> pricingProblems = new ArrayList<>();
    List<AssignmentColumn> solutionCG;
    int upperBound;
    private double[][] fixedLocationSolution;
    private boolean isFixedLocation;

    private double objectiveValue;
    public LocationAssignmentCGSolver(LocationAssignment locationAssignment) {
        this.locationAssignment = locationAssignment;
    }
    public LocationAssignmentCGSolver(LocationAssignment locationAssignment,double[][] fixedLocationSolution) {
        this.locationAssignment = locationAssignment;
        this.fixedLocationSolution=fixedLocationSolution;
        isFixedLocation=true;
    }

    public Map<String, double[]> getDualCostsMap(){
        return pricingProblems.get(0).dualCostsMap;
    }
    public double getObjectiveValue(){
        return objectiveValue;
    }
    public void solveCG() {
        //Create Pricing problems

        for (Worker worker : locationAssignment.instance.getScenarios().get(0).getAvailableWorkers()) {
            PricingProblem pricingProblem = new PricingProblem(locationAssignment, worker, "pricingProblem_"+worker.getIndex());
            pricingProblems.add(pricingProblem);
        }
        //Create the Master Problem
        GlobalVariable.stationNum=locationAssignment.instance.getStationCandidates().size();
        GlobalVariable.typeNum=locationAssignment.instance.getType().length;
        if (locationAssignment.instance.getLambda() == null) {
            locationAssignment.instance.setLambda(new double[GlobalVariable.stationNum][GlobalVariable.typeNum]);
        }
        Master master;
        if(isFixedLocation){
            master  = new Master(locationAssignment, pricingProblems,fixedLocationSolution,isFixedLocation);
        }else{
           master = new Master(locationAssignment, pricingProblems);
        }

        //Define which solvers to use for the pricing problem
        List<Class<? extends AbstractPricingProblemSolver<LocationAssignment, AssignmentColumn, PricingProblem>>> solvers = new ArrayList<> ();
        solvers.add(HeuristicPricingProblemSolver.class);
//        solvers.add(ExactPricingProblemSolver.class);
        long runTime=System.currentTimeMillis();
        //Optional: Get an initial solution
        List<AssignmentColumn> initSolution = this.getInitialSolution(locationAssignment.instance,pricingProblems);
         upperBound =  initialObj;

        //Optional: Get a lower bound on the optimum solution, e.g. largest clique in the graph
        int lowerBound = this.calculateLowerBound();

        ColGen<LocationAssignment, AssignmentColumn, PricingProblem> cg = new ColGen<>(locationAssignment, master, pricingProblems, solvers, initSolution, upperBound, lowerBound);

        //OPTIONAL: Attach a debugger
//        new SimpleDebugger(cg, true);

        //OPTIONAL: Attach a logger to the Branch-and-Price procedure.
        SimpleCGLogger logger = new SimpleCGLogger(cg, new File("./output/Log/crowdSourcing" + locationAssignment.instance.getName() + ".log"));

        //Solve the Graph Coloring problem through Branch-and-Price
        try {
            cg.solve(System.currentTimeMillis() + 120000L);//7200000L
            System.out.println("run column generation"+(System.currentTimeMillis()-runTime));
        } catch (TimeLimitExceededException e) {
            e.printStackTrace();
        }
        objectiveValue=cg.getObjective();
        System.out.println("================ Solution ================");
        solutionCG = cg.getSolution();
        for(AssignmentColumn assignmentColumn:solutionCG){
            System.out.println(assignmentColumn);
        }
        System.out.println("CG terminated with objective: " + cg.getObjective());
        System.out.println("Number of iterations: " + cg.getNumberOfIterations());
        System.out.println("Time spent on master: " + cg.getMasterSolveTime() + " time spent on pricing: " + cg.getPricingSolveTime());
        System.out.println("upper bound: " + upperBound);
        System.out.println(cg.getObjective() + " " + cg.getNumberOfIterations() + " " + cg.getMasterSolveTime() + " " + cg.getPricingSolveTime());
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
     * @param instance
     * @return Feasible solution.
     */
    public List<AssignmentColumn> getInitialSolution(Instance instance,List<PricingProblem> pricingProblems) {
        List<AssignmentColumn> assignmentColumns = new ArrayList<>();
        int stationIndex = 0;
        int accumulatedOccupiedCapS = 0;
        int workerIndex = 0;
        int accumulatedOccupiedCapW = 0;
        Worker worker = instance.getScenarios().get(0).getAvailableWorkers().get(workerIndex);
        StationCandidate stationCandidate = instance.getStationCandidates().get(stationIndex);
        Set<Customer> customers = new HashSet<>();
        double obj = instance.getStationCandidates().get(stationIndex).getFixedCost() + instance.getStationCandidates().get(stationIndex).getCapacityCost() * instance.getType()[instance.getType().length - 1];
        AssignmentColumn_virtual assignmentColumn_virtual = new AssignmentColumn_virtual(pricingProblems.get(workerIndex), false, "initial",stationIndex,instance.getType().length - 1 );
        assignmentColumns.add(assignmentColumn_virtual);
        for (Customer customer : instance.getCustomers()) {
            int accuS = accumulatedOccupiedCapS + instance.getScenarios().get(0).getCustomerDemand()[customer.getIndex()];
            int accuW = accumulatedOccupiedCapW + instance.getScenarios().get(0).getCustomerDemand()[customer.getIndex()];
            if (accuS <= instance.getType()[instance.getType().length - 1] && accuW <= worker.getCapacity() && customers.size() < instance.getWorkerCapacityNum()) {
                accumulatedOccupiedCapS = accuS;
                accumulatedOccupiedCapW = accuW;
                customers.add(customer);
            } else if (accuS > instance.getType()[instance.getType().length - 1]) {
                double cost = Util.getCost(customers, worker, stationCandidate, instance);

                AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblems.get(workerIndex), false, "initial", cost, getDemand(customers,instance.getScenarios().get(0)), worker, customers, stationCandidate);
                assignmentColumns.add(assignmentColumn_true);
                obj += cost;
                obj += instance.getStationCandidates().get(stationIndex).getFixedCost() + instance.getStationCandidates().get(stationIndex).getCapacityCost() * instance.getType()[instance.getType().length - 1];
                accumulatedOccupiedCapS = instance.getScenarios().get(0).getCustomerDemand()[customer.getIndex()];
                accumulatedOccupiedCapW = instance.getScenarios().get(0).getCustomerDemand()[customer.getIndex()];
                stationIndex++;
                workerIndex++;
                assignmentColumn_virtual = new AssignmentColumn_virtual(pricingProblems.get(workerIndex), true, "initial",stationIndex,instance.getType().length - 1 );
                assignmentColumns.add(assignmentColumn_virtual);
                worker = instance.getScenarios().get(0).getAvailableWorkers().get(workerIndex);
                stationCandidate = instance.getStationCandidates().get(stationIndex);
                customers = new HashSet<>();
                customers.add(customer);
            } else if (accuW > worker.getCapacity() || customers.size() >= instance.getWorkerCapacityNum()) {
                double cost = Util.getCost(customers, worker, stationCandidate, instance);
                AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblems.get(workerIndex), false, "initial", cost,getDemand(customers,instance.getScenarios().get(0)), worker, customers, stationCandidate);
                assignmentColumns.add(assignmentColumn_true);
                obj += cost;
                accumulatedOccupiedCapS = accuS;
                accumulatedOccupiedCapW = instance.getScenarios().get(0).getCustomerDemand()[customer.getIndex()];
                workerIndex++;
                if(workerIndex==instance.getWorkers().size()){
                    break;
                }
                worker = instance.getWorkers().get(workerIndex);
                customers = new HashSet<>();
                customers.add(customer);
            }
            if (customer.getIndex() == instance.getCustomers().size() - 1) {
                double cost = Util.getCost(customers, worker, stationCandidate, instance);
                AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblems.get(workerIndex),  false, "initial", cost,getDemand(customers,instance.getScenarios().get(0)), worker, customers, stationCandidate);
                assignmentColumns.add(assignmentColumn_true);
                obj += cost;
            }

        }
        initialObj = (int)obj;
        return assignmentColumns;
    }


    /**
     * Calculate a lower bound on the chromatic number of a graph, by calculating the largest clique in the graph.
     *
     * @return lower bound
     */
    private int calculateLowerBound() {
        int lowerbound = 0;
        for (Customer customer:locationAssignment.instance.getCustomers()){
            lowerbound-=customer.getUnservedPenalty()*GlobalVariable.daysNum;
        }
        //todo:design a lower bound calculator


        return lowerbound;
    }



}
