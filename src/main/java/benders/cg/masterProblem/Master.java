package benders.cg.masterProblem;

import benders.cg.column.AssignmentColumn_true;
import benders.cg.pricing.PricingProblem;
import benders.model.LocationAssignment;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import model.Customer;
import model.Scenario;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.master.AbstractMaster;
import org.jorlib.frameworks.columnGeneration.master.OptimizationSense;
import org.jorlib.frameworks.columnGeneration.util.OrderedBiMap;
import util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/7 17:59
 */
public class Master extends AbstractMaster<LocationAssignment, AssignmentColumn_true, PricingProblem, LocationAssignmentMasterData> {
    private IloObjective obj; //Objective function
    private IloRange[] oneVisitPerCustomerAtMost; //at most one visit per customer
    private IloRange[] stationCapConstraint; //capacity of station
    private IloRange[] oneRoutePerWorkerAtMost; //each worker carry out one route at most
    private double[] capacity;
    private Scenario scenario;



    public Master(LocationAssignment dataModel, List<PricingProblem> pricingProblems, Scenario scenario) {
        super(dataModel, pricingProblems, OptimizationSense.MINIMIZE);
        this.capacity = dataModel.capacity;
        this.scenario = scenario;
        masterData = this.buildModel();
    }

    @Override
    protected LocationAssignmentMasterData buildModel() {

        int stationNum = dataModel.instance.getStationCandidates().size();
        int workerNum = scenario.getAvailableWorkers().size();
        int customerNum = dataModel.instance.getCustomers().size();

        IloCplex cplex = null; //Create cplex instance
        try {

            cplex = new IloCplex(); //Create cplex instance
            cplex.setOut(null); //Disable cplex output
            cplex.setParam(IloCplex.IntParam.Threads, config.MAXTHREADS); //Set number of threads that may be used by the master
            //Define objective
            obj = cplex.addMinimize();


            //Define constraints


            oneVisitPerCustomerAtMost = new IloRange[customerNum];
            stationCapConstraint = new IloRange[stationNum];
            oneRoutePerWorkerAtMost = new IloRange[workerNum];

            for (int i = 0; i < customerNum; i++) {
                oneVisitPerCustomerAtMost[i] = cplex.addRange(-Double.MAX_VALUE, 1, "atMOVisitPerCustomer_" + i);
            }
            for (int s = 0; s < stationNum; s++) {
                stationCapConstraint[s] = cplex.addRange(-Double.MAX_VALUE, capacity[s], "capacityConstraint_" + s);
            }
            for (int k = 0; k < workerNum; k++) {
                int capacity = scenario.getWorkerCapacity()[k];
                oneRoutePerWorkerAtMost[k] = cplex.addRange(-Double.MAX_VALUE, capacity, "atMORoutePerWorker_" + k);
            }

        } catch (IloException e) {
            e.printStackTrace();
        }
        //Define objective
        //Define a container for the variables
        Map<PricingProblem, OrderedBiMap<AssignmentColumn_true, IloNumVar>> varMap = new LinkedHashMap<>();
        for (PricingProblem pricingProblem : pricingProblems) {
            varMap.put(pricingProblem, new OrderedBiMap<>());
        }
        LocationAssignmentMasterData locationAssignmentMasterData = new LocationAssignmentMasterData(cplex, varMap, pricingProblems);


        return locationAssignmentMasterData;
    }

    /**
     * Method implementing the solve procedure for the master problem
     *
     * @param timeLimit Future point in time by which this method must be finished
     * @return Returns true if successfull (and optimal)
     * @throws TimeLimitExceededException if time limit is exceeded
     */
    @Override
    protected boolean solveMasterProblem(long timeLimit) throws TimeLimitExceededException {
        try {
            //Set time limit
            double timeRemaining = Math.max(1, (timeLimit - System.currentTimeMillis()) / 1000.0);
            masterData.cplex.setParam(IloCplex.DoubleParam.TiLim, timeRemaining); //set time limit in seconds
            //Potentially export the model
//            if (scenario.getIndex() == 0) {
//                if (config.EXPORT_MODEL) {
//                    masterData.cplex.exportModel(config.EXPORT_MASTER_DIR + "master_" + this.getIterationCount() + ".lp");
//                }
//                exportModel("master_" + this.getIterationCount() + ".lp");
//            }
            //Solve the model
            if (!masterData.cplex.solve() || masterData.cplex.getStatus() != IloCplex.Status.Optimal) {
                if (masterData.cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim) //Aborted due to time limit
                    throw new TimeLimitExceededException();
                else
                    throw new RuntimeException("Master problem solve failed! Status: " + masterData.cplex.getStatus());
            } else {
                masterData.objectiveValue = masterData.cplex.getObjValue();
                getSolution();
            }
        } catch (IloException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Extracts information from the master problem which is required by the pricing problems, e.g. the reduced costs/dual values
     *
     * @param pricingProblem pricing problem
     */

    @Override
    public void initializePricingProblem(PricingProblem pricingProblem) {
        try {
            Map<String, double[]> dualMap = new HashMap<>();
            double[] dualC = masterData.cplex.getDuals(oneVisitPerCustomerAtMost);
            double[] dualS = masterData.cplex.getDuals(stationCapConstraint);
            double[] dualW = masterData.cplex.getDuals(oneRoutePerWorkerAtMost);

//                AssignmentColumn[] assignmentColumns = masterData.getVarMapForPricingProblem(pricingProblem).getKeysAsArray(new AssignmentColumn[masterData.getNrColumnsForPricingProblem(pricingProblem)]);
//                IloNumVar[] vars = masterData.getVarMapForPricingProblem(pricingProblem).getValuesAsArray(new IloNumVar[masterData.getNrColumnsForPricingProblem(pricingProblem)]);
//                double[] rc=masterData.cplex.getReducedCosts(vars);
//                for(int i=0;i<rc.length;i++){
//                    if(rc[i]<0){
//                        int a=0;
//                    }
//                }


            dualMap.put("oneVisitPerCustomerAtMost_"+scenario.getIndex(), dualC);
            dualMap.put("stationCapConstraint_"+scenario.getIndex(), dualS);
            dualMap.put("oneRoutePerWorkerAtMost_"+scenario.getIndex(), dualW);

//            pricingProblem.initPricingProblem(dualMap, getSolution());
            pricingProblem.initPricingProblem(dualMap);
        } catch (IloException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void addColumn(AssignmentColumn_true column) {

        try {
            if (column instanceof AssignmentColumn_true) {
                AssignmentColumn_true column_true = (AssignmentColumn_true) column;
                double objCoe = column_true.cost;

                //Set linear coefficient for parcel visited Constraints
                int pConstraintCoe[] = new int[dataModel.instance.getCustomers().size()];
                for (Customer customer : column_true.customers) {
                    int index = customer.getIndex();
                    pConstraintCoe[index] = 1;
                }
                //Set linear coefficient for station Capacity
                int sConstraintCoe[] = new int[dataModel.instance.getStationCandidates().size()];

                sConstraintCoe[column_true.stationCandidate.getIndex()] = column_true.demands[scenario.getIndex()];
                //Set linear coefficient for worker One JobSimple At Most
                int wConstraintCoe[] = new int[dataModel.instance.getWorkers().size()];
                wConstraintCoe[column_true.worker.getIndex()] = column_true.demands[scenario.getIndex()];


                //Register column with objective
                IloColumn iloColumn = masterData.cplex.column(obj, objCoe);

                //Register column with constraints
                for (int i = 0; i < dataModel.instance.getCustomers().size(); i++) {
                    iloColumn = iloColumn.and(masterData.cplex.column(oneVisitPerCustomerAtMost[i], pConstraintCoe[i]));
                }
                for (int i = 0; i < dataModel.instance.getStationCandidates().size(); i++) {
                    iloColumn = iloColumn.and(masterData.cplex.column(stationCapConstraint[i], sConstraintCoe[i]));
                }
                for (int i = 0; i < dataModel.instance.getWorkers().size(); i++) {
                    iloColumn = iloColumn.and(masterData.cplex.column(oneRoutePerWorkerAtMost[i], wConstraintCoe[i]));
                }

                //Create the variable and store it
                IloNumVar var = masterData.cplex.numVar(iloColumn, 0, Double.MAX_VALUE, "w_" + column_true.associatedPricingProblem.worker.getIndex() + "_" + masterData.getNrColumnsForPricingProblem(column_true.associatedPricingProblem));
                masterData.cplex.add(var);
                masterData.addColumn(column_true, var);
            }

        } catch (IloException e) {
            e.printStackTrace();
        }

    }

    @Override
    public List<AssignmentColumn_true> getSolution() {
        List<AssignmentColumn_true> solution = new ArrayList<>();
        try {
            for (PricingProblem pricingProblem : pricingProblems) {
                AssignmentColumn_true[] assignmentColumns = masterData.getVarMapForPricingProblem(pricingProblem).getKeysAsArray(new AssignmentColumn_true[masterData.getNrColumnsForPricingProblem(pricingProblem)]);
                IloNumVar[] vars = masterData.getVarMapForPricingProblem(pricingProblem).getValuesAsArray(new IloNumVar[masterData.getNrColumnsForPricingProblem(pricingProblem)]);

                //Iterate over each column and add it to the solution if it has a non-zero value
                for (int i = 0; i < masterData.getNrColumnsForPricingProblem(pricingProblem); i++) {
                    assignmentColumns[i].value = masterData.cplex.getValue(vars[i]);
                    if (assignmentColumns[i].value >= config.PRECISION) {
                        solution.add(assignmentColumns[i]);
//                        System.out.println(assignmentColumns[i]);
                    }
                }
            }
        } catch (IloException e) {
            e.printStackTrace();
        }

        return solution;
    }

    @Override
    public void printSolution() {
        System.out.println("Master solution:");
        for (AssignmentColumn_true jc : this.getSolution())
            System.out.println(jc);
    }

    @Override
    public void close() {
        masterData.cplex.end();
    }

    /**
     * Export the model to a file
     */
    public void exportModel(String fileName) {
        try {
            masterData.cplex.exportModel(config.EXPORT_MASTER_DIR + fileName);
        } catch (IloException e) {
            e.printStackTrace();
        }
    }


}
