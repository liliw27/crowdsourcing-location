package locationAssignmentBAP;

import FWPH.model.SolutionValue;
import io.Reader;
import locationAssignmentBAP.bap.BranchAndPrice;
import locationAssignmentBAP.bap.branching.BranchOnLocationVar;
import locationAssignmentBAP.bap.branching.BranchOnStationWorkerPair;
import locationAssignmentBAP.bap.branching.BranchOnVertexPair;
import locationAssignmentBAP.bap.branching.BranchOnWorkerCustomerPair;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.cg.masterProblem.Master;
import locationAssignmentBAP.cg.pricing.ExactPricingProblemSolver;
import locationAssignmentBAP.cg.pricing.HeuristicPricingProblemSolver;
import locationAssignmentBAP.cg.pricing.PricingProblem;
import locationAssignmentBAP.model.LocationAssignment;
import model.Customer;
import model.Instance;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.io.SimpleBAPLogger;
import org.jorlib.frameworks.columnGeneration.io.SimpleDebugger;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import util.GlobalVariable;
import FWPH.FWPHUtil;
import util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
public class LocationAssignmentSolver {
    private final LocationAssignment locationAssignment;
    private int initialObj;
    public double upperBound;
    public double lowerBound;
    public BranchAndPrice bap;
    public Map<String, double[]> dualSolution;
    public boolean isOptimal;


    public LocationAssignmentSolver(LocationAssignment locationAssignment) {
        this.locationAssignment = locationAssignment;
    }

    public SolutionValue solveInstance() {
        //Create Pricing problems
        List<PricingProblem> pricingProblems = new ArrayList<>();
        for (Worker worker : locationAssignment.instance.getScenarios().get(0).getAvailableWorkers()) {
            PricingProblem pricingProblem = new PricingProblem(locationAssignment, worker, "pricingProblem_" + worker.getIndex());
            pricingProblems.add(pricingProblem);
        }
        //Create the Master Problem
        GlobalVariable.stationNum = locationAssignment.instance.getStationCandidates().size();
        GlobalVariable.typeNum = locationAssignment.instance.getType().length;
        if (locationAssignment.instance.getLambda() == null) {
            locationAssignment.instance.setLambda(new double[GlobalVariable.stationNum][GlobalVariable.typeNum]);
        }
        Master master = new Master(locationAssignment, pricingProblems);

        //Define which solvers to use for the pricing problem
        List<Class<? extends AbstractPricingProblemSolver<LocationAssignment, AssignmentColumn, PricingProblem>>> solvers = new ArrayList<>();
        solvers.add(HeuristicPricingProblemSolver.class);
//        solvers.add(ExactPricingProblemSolver.class);

        //Optional: Get an initial solution
        List<AssignmentColumn> initSolution = this.getInitialSolution(locationAssignment.instance, pricingProblems);
        upperBound = initialObj;

        //Optional: Get a lower bound on the optimum solution, e.g. largest clique in the graph
        lowerBound = this.calculateLowerBound();

        //Define Branch creators
        List<AbstractBranchCreator<LocationAssignment, AssignmentColumn, PricingProblem>> branchCreators = new ArrayList<>();
        BranchOnLocationVar branchOnLocationVar = new BranchOnLocationVar(locationAssignment, pricingProblems);
        BranchOnVertexPair branchOnVertexPair = new BranchOnVertexPair(locationAssignment, pricingProblems);
        BranchOnStationWorkerPair branchOnStationWorkerPair = new BranchOnStationWorkerPair(locationAssignment, pricingProblems);
        BranchOnWorkerCustomerPair branchOnWorkerCustomerPair = new BranchOnWorkerCustomerPair(locationAssignment, pricingProblems);

        branchCreators.add(branchOnLocationVar);
        branchCreators.add(branchOnVertexPair);
        branchCreators.add(branchOnStationWorkerPair);
        branchCreators.add(branchOnWorkerCustomerPair);


//        Collections.singletonList(new BranchOnVertexPair(locationAssignment, pricingProblem))

        //Create a Branch-and-Price instance, and provide the initial solution as a warm-start
        bap = new BranchAndPrice(locationAssignment, master, pricingProblems, solvers, branchCreators, (int) upperBound, (int) lowerBound, initSolution);
//        bap.warmStart(upperBound, initSolution);

        //OPTIONAL: Attach a debugger
//        new SimpleDebugger(bap, true);

        //OPTIONAL: Attach a logger to the Branch-and-Price procedure.
        new SimpleBAPLogger(bap, new File("./output/coloring.log"));

        //Solve the Graph Coloring problem through Branch-and-Price
        bap.runBranchAndPrice(System.currentTimeMillis() + 120000L); //8000000L


        //Print solution:
        System.out.println("================ Solution ================");
        System.out.println("BAP terminated with objective : " + bap.getObjective());
        System.out.println("Total Number of iterations: " + bap.getTotalNrIterations());
        System.out.println("Total Number of processed nodes: " + bap.getNumberOfProcessedNodes());
        System.out.println("Total Time spent on master problems: " + bap.getMasterSolveTime() + " Total time spent on pricing problems: " + bap.getPricingSolveTime());
        if (bap.hasSolution()) {
            System.out.println("Solution is optimal: " + bap.isOptimal());
            this.isOptimal = bap.isOptimal();
            System.out.println("Columns (only non-zero columns are returned):");
            List<AssignmentColumn> solution = bap.getSolution();
            for (AssignmentColumn column : solution)
                System.out.println(column);
            if (!bap.isOptimal()) {
                lowerBound = bap.getBound();
                upperBound = bap.getObjective();
                double g =Math.abs ((bap.getObjective() - bap.getBound()) / bap.getBound());
                System.out.println("Bound:" + bap.getBound() + "gap:" + g);
                dualSolution = bap.getDualSolution();
            } else {
                lowerBound = bap.getObjective();
                upperBound = bap.getObjective();
            }
        }
        SolutionValue solutionValue = new SolutionValue();
        double obj = bap.getObjective();
        double objFirst = 0;
        double objSecond = 0;
        double[][] x = new double[locationAssignment.instance.getStationCandidates().size()][locationAssignment.instance.getType().length];
        for (AssignmentColumn column : bap.getSolution()) {
            if (column instanceof AssignmentColumn_true) {
                objSecond += ((AssignmentColumn_true) column).cost;

            } else {
                AssignmentColumn_virtual column_virtual = (AssignmentColumn_virtual) column;
                StationCandidate stationCandidate = locationAssignment.instance.getStationCandidates().get(column_virtual.stationIndex);
                objFirst += stationCandidate.getFixedCost() + stationCandidate.getCapacityCost() * locationAssignment.instance.getType()[column_virtual.type];
                x[column_virtual.stationIndex][column_virtual.type] = 1;
            }
        }


//        if(gap> Constants.EPSILON){
//           throw new RuntimeException("the objective value are not coincident with the summation of the obj values of two stages, with gap:"+gap);
//        }
        solutionValue.setObjFirst(objFirst);
        solutionValue.setObjSecond(objSecond);
        solutionValue.setObj(objFirst + objSecond);
        solutionValue.setX(x);

        //Clean up:
        bap.close(); //Close master and pricing problems
        return solutionValue;
    }

    public static void main(String[] args) throws IOException {

        File file = new File("dataset/instance/instance");
        Instance instance = Reader.readInstance(file, 5, 0, 5, 30, 15, 1);

//        Instance instance = Reader.readInstance(file, 5, 0, 5, 20, 2, 1);
        //get instances for each scenario
        List<Instance> instanceList = FWPHUtil.getInstances(instance);
        for (int i = 0; i < 1; i++) {
            Instance instance1 = instanceList.get(i);

            LocationAssignment locationAssignment1 = new LocationAssignment(instance1);
            LocationAssignmentSolver locationAssignmentSolver = new LocationAssignmentSolver(locationAssignment1);
            SolutionValue solutionValue = locationAssignmentSolver.solveInstance();

//            try {
//                if (instance1.getLambda() == null) {
//                    instance1.setLambda(new double[instance1.getStationCandidates().size()][instance1.getType().length]);
//                }
//                SolutionValue solutionValue= FWPHUtil.solveInstance(instance1);
//            } catch (IloException e) {
//                throw new RuntimeException(e);
//            }
        }
//        ColoringGraph coloringGraph=new ColoringGraph("./data/graphColoring/myciel3.col"); //Optimal: 4

//        new ChromaticNumberCalculator(coloringGraph);
    }


    //------------------ Helper methods -----------------

    /**
     * Calculate a feasible solution using a greedy algorithm.
     *
     * @param instance
     * @return Feasible solution.
     */
    public List<AssignmentColumn> getInitialSolution(Instance instance, List<PricingProblem> pricingProblems) {
        List<AssignmentColumn> assignmentColumns = new ArrayList<>();
        int stationIndex = 0;
        int accumulatedOccupiedCapS = 0;
        int workerIndex = 0;
        int accumulatedOccupiedCapW = 0;
        Worker worker = instance.getScenarios().get(0).getAvailableWorkers().get(workerIndex);
        StationCandidate stationCandidate = instance.getStationCandidates().get(stationIndex);
        Set<Customer> customers = new HashSet<>();
        double obj = instance.getStationCandidates().get(stationIndex).getFixedCost() + instance.getStationCandidates().get(stationIndex).getCapacityCost() * instance.getType()[instance.getType().length - 1];
        AssignmentColumn_virtual assignmentColumn_virtual = new AssignmentColumn_virtual(pricingProblems.get(workerIndex), false, "initial", stationIndex, instance.getType().length - 1);
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

                AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblems.get(workerIndex), false, "initial", cost, getDemand(customers, instance.getScenarios().get(0)), worker, customers, stationCandidate);
                assignmentColumns.add(assignmentColumn_true);
                obj += cost;
                obj += instance.getStationCandidates().get(stationIndex).getFixedCost() + instance.getStationCandidates().get(stationIndex).getCapacityCost() * instance.getType()[instance.getType().length - 1];
                accumulatedOccupiedCapS = instance.getScenarios().get(0).getCustomerDemand()[customer.getIndex()];
                accumulatedOccupiedCapW = instance.getScenarios().get(0).getCustomerDemand()[customer.getIndex()];
                stationIndex++;
                workerIndex++;
                assignmentColumn_virtual = new AssignmentColumn_virtual(pricingProblems.get(workerIndex), true, "initial", stationIndex, instance.getType().length - 1);
                assignmentColumns.add(assignmentColumn_virtual);
                worker = instance.getScenarios().get(0).getAvailableWorkers().get(workerIndex);
                stationCandidate = instance.getStationCandidates().get(stationIndex);
                customers = new HashSet<>();
                customers.add(customer);
            } else if (accuW > worker.getCapacity() || customers.size() >= instance.getWorkerCapacityNum()) {
                double cost = Util.getCost(customers, worker, stationCandidate, instance);
                AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblems.get(workerIndex), false, "initial", cost, getDemand(customers, instance.getScenarios().get(0)), worker, customers, stationCandidate);
                assignmentColumns.add(assignmentColumn_true);
                obj += cost;
                accumulatedOccupiedCapS = accuS;
                accumulatedOccupiedCapW = instance.getScenarios().get(0).getCustomerDemand()[customer.getIndex()];
                workerIndex++;
                if (workerIndex == instance.getWorkers().size()) {
                    break;
                }
                worker = instance.getWorkers().get(workerIndex);
                customers = new HashSet<>();
                customers.add(customer);
            }
            if (customer.getIndex() == instance.getCustomers().size() - 1) {
                double cost = Util.getCost(customers, worker, stationCandidate, instance);
                AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblems.get(workerIndex), false, "initial", cost, getDemand(customers, instance.getScenarios().get(0)), worker, customers, stationCandidate);
                assignmentColumns.add(assignmentColumn_true);
                obj += cost;
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
            lowerbound -= customer.getUnservedPenalty() * GlobalVariable.daysNum;
        }
        //todo:design a lower bound calculator


        return lowerbound;
    }

}
