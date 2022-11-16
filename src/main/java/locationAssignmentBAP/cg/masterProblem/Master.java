package locationAssignmentBAP.cg.masterProblem;

import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import locationAssignmentBAP.bap.branching.branchDecisions.FixLocationWithType;
import locationAssignmentBAP.bap.branching.branchDecisions.FixWorkerCustomer;
import locationAssignmentBAP.bap.branching.branchDecisions.FixWorkerStation;
import locationAssignmentBAP.bap.branching.branchDecisions.IncompatibleWorkerCustomer;
import locationAssignmentBAP.bap.branching.branchDecisions.IncompatibleWorkerStation;
import locationAssignmentBAP.bap.branching.branchDecisions.RemoveLocation;
import locationAssignmentBAP.cg.branchingCut.WorkerCustomerInequality;
import locationAssignmentBAP.cg.branchingCut.WorkerCustomerInequalityGenerator;
import locationAssignmentBAP.cg.branchingCut.WorkerStationInequality;
import locationAssignmentBAP.cg.branchingCut.WorkerStationInequalityGenerator;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.cg.pricing.PricingProblem;
import locationAssignmentBAP.model.LocationAssignment;
import model.Customer;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
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
public class Master extends AbstractMaster<LocationAssignment, AssignmentColumn, PricingProblem, LocationAssignmentMasterData> {
    private IloObjective obj; //Objective function
    private IloRange[] oneTypePerStationAtMost; //at most one type chosen per station
    private IloRange[] oneVisitPerCustomerAtMost; //at most one visit per customer
    private IloRange[] stationCapConstraint; //capacity of station
    private IloRange[] oneRoutePerWorkerAtMost; //each worker carry out one route at most
    private IloNumVar[][] locationVars;


    public Master(LocationAssignment dataModel, List<PricingProblem> pricingProblems) {
        super(dataModel, pricingProblems, OptimizationSense.MINIMIZE);
        locationVars = Util.getVar();
        masterData=this.buildModel();
    }
    public Master(LocationAssignment dataModel, List<PricingProblem> pricingProblems,double[][] fixedLocationSolution,boolean isFixedLocation) {
        super(dataModel, pricingProblems, OptimizationSense.MINIMIZE);
        if(isFixedLocation){
            locationVars = Util.getVar(fixedLocationSolution);
        }else {
            locationVars = Util.getVar();
        }
        masterData=this.buildModel();
    }

    @Override
    protected LocationAssignmentMasterData buildModel() {

        int stationNum = dataModel.instance.getStationCandidates().size();
        int typeNum = dataModel.instance.getType().length;
        int workerNum = dataModel.instance.getScenarios().get(0).getAvailableWorkers().size();
        int customerNum = dataModel.instance.getCustomers().size();
        IloNumVar[][] locationVars = new IloNumVar[stationNum][typeNum];
        IloCplex cplex = null; //Create cplex instance
        try {

            cplex = new IloCplex(); //Create cplex instance
            cplex.setOut(null); //Disable cplex output
            cplex.setParam(IloCplex.IntParam.Threads, config.MAXTHREADS); //Set number of threads that may be used by the master
            //Define objective
            obj = cplex.addMinimize();
            IloLinearNumExpr expr = cplex.linearNumExpr();

            for (int s = 0; s < stationNum; s++) {
                for (int t = 0; t < typeNum; t++) {
                    locationVars[s][t] = cplex.numVar(this.locationVars[s][t].getLB(), this.locationVars[s][t].getUB(), "y_" + s + "_" + t);
                }
            }
            for (int s = 0; s < stationNum; s++) {
                StationCandidate stationCandidate = dataModel.instance.getStationCandidates().get(s);
                for (int t = 0; t < typeNum; t++) {
                    double coe = stationCandidate.getFixedCost() + stationCandidate.getCapacityCost() * dataModel.instance.getType()[t];
                    expr.addTerm(coe + dataModel.instance.getLambda()[s][t], locationVars[s][t]);
                }
            }
            obj.setExpr(expr);

            //Define constraints

            oneTypePerStationAtMost = new IloRange[stationNum];
            oneVisitPerCustomerAtMost = new IloRange[customerNum];
            stationCapConstraint = new IloRange[stationNum];
            oneRoutePerWorkerAtMost = new IloRange[workerNum];


            for (int s = 0; s < stationNum; s++) {
                oneTypePerStationAtMost[s] = cplex.addRange(0, 1, "atMOTypePerStation_" + s);
                expr = cplex.linearNumExpr();
                for (int t = 0; t < typeNum; t++) {
                    expr.addTerm(1, locationVars[s][t]);
                }
                oneTypePerStationAtMost[s].setExpr(expr);
            }

            for (int i = 0; i < customerNum; i++) {
                oneVisitPerCustomerAtMost[i] = cplex.addRange(0, 1, "atMOVisitPerCustomer_" + i);
            }
            for (int s = 0; s < stationNum; s++) {
                stationCapConstraint[s] = cplex.addRange(-Double.MAX_VALUE, 0, "capacityConstraint_" + s);
                expr = cplex.linearNumExpr();
                for (int t = 0; t < typeNum; t++) {
                    expr.addTerm(-dataModel.instance.getType()[t], locationVars[s][t]);
                }
                stationCapConstraint[s].setExpr(expr);
            }
            for (int k = 0; k < workerNum; k++) {
                int capacity=dataModel.instance.getWorkers().get(k).getCapacity();
                oneRoutePerWorkerAtMost[k] = cplex.addRange(0, capacity, "atMORoutePerWorker_" + k);
            }

        } catch (IloException e) {
            e.printStackTrace();
        }
        //Define objective
        //Define a container for the variables
        Map<PricingProblem, OrderedBiMap<AssignmentColumn, IloNumVar>> varMap = new LinkedHashMap<>();
        for (PricingProblem pricingProblem : pricingProblems) {
            varMap.put(pricingProblem, new OrderedBiMap<>());
        }
        LocationAssignmentMasterData locationAssignmentMasterData = new LocationAssignmentMasterData(cplex, varMap, locationVars, pricingProblems);


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
            if (config.EXPORT_MODEL) {
                masterData.cplex.exportModel(config.EXPORT_MASTER_DIR + "master_" + this.getIterationCount() + ".lp");
            }
            exportModel("master_" + this.getIterationCount() + ".lp");
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
            double[] dualT = masterData.cplex.getDuals(oneTypePerStationAtMost);
            double[] dualC = masterData.cplex.getDuals(oneVisitPerCustomerAtMost);
            double[] dualS = masterData.cplex.getDuals(stationCapConstraint);
            double[] dualW = masterData.cplex.getDuals(oneRoutePerWorkerAtMost);
            double[][] dualWS = new double[dataModel.instance.getWorkers().size()][dataModel.instance.getStationCandidates().size()];
            double[][] dualWC = new double[dataModel.instance.getWorkers().size()][dataModel.instance.getCustomers().size()];

            for (WorkerStationInequality workerStationInequality : masterData.workerStationInequalities.keySet()) {
                int wIndex = workerStationInequality.worker.getIndex();
                int sIndex = workerStationInequality.stationCandidate.getIndex();
                dualWS[wIndex][sIndex] = masterData.cplex.getDual(masterData.workerStationInequalities.get(workerStationInequality));
            }
            for (WorkerCustomerInequality workerCustomerInequality : masterData.workerCustomerInequalities.keySet()) {
                int wIndex = workerCustomerInequality.worker.getIndex();
                int cIndex = workerCustomerInequality.customer.getIndex();
                dualWC[wIndex][cIndex] = masterData.cplex.getDual(masterData.workerCustomerInequalities.get(workerCustomerInequality));
            }
//                AssignmentColumn[] assignmentColumns = masterData.getVarMapForPricingProblem(pricingProblem).getKeysAsArray(new AssignmentColumn[masterData.getNrColumnsForPricingProblem(pricingProblem)]);
//                IloNumVar[] vars = masterData.getVarMapForPricingProblem(pricingProblem).getValuesAsArray(new IloNumVar[masterData.getNrColumnsForPricingProblem(pricingProblem)]);
//                double[] rc=masterData.cplex.getReducedCosts(vars);
//                for(int i=0;i<rc.length;i++){
//                    if(rc[i]<0){
//                        int a=0;
//                    }
//                }


            dualMap.put("oneTypePerStationAtMost", dualT);
            dualMap.put("oneVisitPerCustomerAtMost", dualC);
            dualMap.put("stationCapConstraint", dualS);
            dualMap.put("oneRoutePerWorkerAtMost", dualW);

//            pricingProblem.initPricingProblem(dualMap, getSolution());
            pricingProblem.initPricingProblem(dualMap,dualWS,dualWC);
        } catch (IloException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void addColumn(AssignmentColumn column) {

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
                sConstraintCoe[column_true.stationCandidate.getIndex()] = column_true.demand;
                //Set linear coefficient for worker One JobSimple At Most
                int wConstraintCoe[] = new int[dataModel.instance.getWorkers().size()];
                wConstraintCoe[column_true.worker.getIndex()] = column_true.demand;


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
    public List<AssignmentColumn> getSolution() {
        List<AssignmentColumn> solution = new ArrayList<>();
        try {
            for (PricingProblem pricingProblem : pricingProblems) {
                AssignmentColumn[] assignmentColumns = masterData.getVarMapForPricingProblem(pricingProblem).getKeysAsArray(new AssignmentColumn[masterData.getNrColumnsForPricingProblem(pricingProblem)]);
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
            for (int s = 0; s < dataModel.instance.getStationCandidates().size(); s++) {
                for (int t = 0; t < dataModel.instance.getType().length; t++) {
                    double value = masterData.cplex.getValue(masterData.locationVars[s][t]);
                    if (value >= config.PRECISION) {
                        AssignmentColumn_virtual assignmentColumn_virtual = new AssignmentColumn_virtual(null, false, "virtualAssignmentColumn", s, t);
                        assignmentColumn_virtual.value = value;
                        solution.add(assignmentColumn_virtual);
//                        System.out.println(assignmentColumn_virtual);
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
        for (AssignmentColumn jc : this.getSolution())
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

    public void branchingDecisionPerformed(BranchingDecision bd) {

        if (bd instanceof FixLocationWithType) {
            try {
                FixLocationWithType fixLocationWithType = (FixLocationWithType) bd;
                int locationIndex = fixLocationWithType.locationIndex;
                int type = fixLocationWithType.type;
                for (int t = 0; t < dataModel.instance.getType().length; t++) {
                    if (t == type) {
                        locationVars[locationIndex][t].setLB(1);
                    } else {
                        locationVars[locationIndex][t].setUB(0);
                    }
                }
            } catch (IloException e) {
                e.printStackTrace();
            }

        } else if (bd instanceof RemoveLocation) {
            try {
                RemoveLocation removeLocation = (RemoveLocation) bd;
                int locationIndex = removeLocation.locationIndex;
                for (int t = 0; t < dataModel.instance.getType().length; t++) {
                    locationVars[locationIndex][t].setUB(0);
                }
            } catch (IloException e) {
                e.printStackTrace();
            }
        }
        //For simplicity, we simply destroy the master problem and rebuild it. Of course, something more sophisticated may be used which retains the master problem.
        this.close(); //Close the old cplex model
        masterData = this.buildModel(); //Create a new model without any columns
        if (bd instanceof FixWorkerStation) {
            FixWorkerStation fixWorkerStation = (FixWorkerStation) bd;
            Worker worker = fixWorkerStation.worker;
            StationCandidate stationCandidate = fixWorkerStation.stationCandidate;
            WorkerStationInequalityGenerator WSIneqGenerator = new WorkerStationInequalityGenerator(dataModel, worker, stationCandidate, true);
            WSIneqGenerator.setMasterData(masterData);
            WSIneqGenerator.generateInqualities();
        } else if (bd instanceof IncompatibleWorkerStation) {
            IncompatibleWorkerStation incompatibleWorkerStation = (IncompatibleWorkerStation) bd;
            Worker worker = incompatibleWorkerStation.worker;
            StationCandidate stationCandidate = incompatibleWorkerStation.stationCandidate;
            WorkerStationInequalityGenerator WSIneqGenerator = new WorkerStationInequalityGenerator(dataModel, worker, stationCandidate, false);
            WSIneqGenerator.setMasterData(masterData);
            WSIneqGenerator.generateInqualities();
        } else if (bd instanceof FixWorkerCustomer) {
            FixWorkerCustomer fixWorkerCustomer = (FixWorkerCustomer) bd;
            Worker worker = fixWorkerCustomer.worker;
            Customer customer = fixWorkerCustomer.customer;
            WorkerCustomerInequalityGenerator WCIneqGenerator = new WorkerCustomerInequalityGenerator(dataModel, worker, customer, true);
            WCIneqGenerator.setMasterData(masterData);
            WCIneqGenerator.generateInqualities();
        }else if(bd instanceof IncompatibleWorkerCustomer){
            IncompatibleWorkerCustomer incompatibleWorkerCustomer = (IncompatibleWorkerCustomer) bd;
            Worker worker = incompatibleWorkerCustomer.worker;
            Customer customer = incompatibleWorkerCustomer.customer;
            WorkerCustomerInequalityGenerator WCIneqGenerator = new WorkerCustomerInequalityGenerator(dataModel, worker, customer, false);
            WCIneqGenerator.setMasterData(masterData);
            WCIneqGenerator.generateInqualities();
        }
    }

    /**
     * Undo branching decisions during backtracking in the Branch-and-Price tree
     *
     * @param bd Branching decision
     */
    @Override
    public void branchingDecisionReversed(BranchingDecision bd) {
        if (bd instanceof FixLocationWithType) {
            try {
                FixLocationWithType fixLocationWithType = (FixLocationWithType) bd;
                int locationIndex = fixLocationWithType.locationIndex;
                int type = fixLocationWithType.type;
                for (int t = 0; t < dataModel.instance.getType().length; t++) {
                    if (t == type) {
                        locationVars[locationIndex][t].setLB(0);
                    } else {
                        locationVars[locationIndex][t].setUB(1);
                    }
                }
            } catch (IloException e) {
                e.printStackTrace();
            }
        } else if (bd instanceof RemoveLocation) {
            try {
                RemoveLocation removeLocation = (RemoveLocation) bd;
                int locationIndex = removeLocation.locationIndex;
                for (int t = 0; t < dataModel.instance.getType().length; t++) {
                    locationVars[locationIndex][t].setUB(1);
                }
            } catch (IloException e) {
                e.printStackTrace();
            }
        }

    }
}
