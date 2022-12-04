package benders.cg.pricing;

import benders.cg.column.AssignmentColumn_true;
import benders.model.LocationAssignment;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;
import model.Customer;
import model.Scenario;
import model.StationCandidate;
import model.Worker;
import org.jgrapht.util.VertexPair;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;
import util.Constants;
import util.GlobalVariable;
import util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/7 17:44
 */
public class ExactPricingProblemSolver extends AbstractPricingProblemSolver<LocationAssignment, AssignmentColumn_true, PricingProblem> {
    private IloCplex cplex; //Cplex instance.
    private IloObjective obj; //Objective function
    private IloIntVar[] x;
    private IloNumVar[] z;
    private IloIntVar[] v;
    private IloIntVar tao;
    //    IloNumVar[][] theta;
    private IloNumVar dmax1;
    private IloIntVar[] u;
    private IloNumVar[] f1;
    private IloIntVar[][] varTheta;
    //    IloNumVar[] epsilon;
    private IloNumVar dmax2;
    private IloNumVar[] f2;
    private IloNumVar a1plus;
    private IloNumVar a1minus;
    private IloNumVar b1plus;
    private IloNumVar b1minus;
    private IloNumVar[] gamma9;
    private IloNumVar[] gamma11;

    private IloLinearNumExpr objExp;
    private Map<VertexPair<Customer>, IloConstraint> branchingPairConstraints = new HashMap<>();
//    private Map<Worker, List<IloConstraint>> workerConstraints = new HashMap<>();

    /**
     * Creates a new solver instance for a particular pricing problem
     *
     * @param dataModel      data model
     * @param pricingProblem pricing problem
     */
    public ExactPricingProblemSolver(LocationAssignment dataModel, PricingProblem pricingProblem) {
        super(dataModel, pricingProblem);
        this.name = "ExactMatchingCalculator";
        this.buildModel();
        for(StationCandidate stationCandidate:dataModel.incompatibleStations){
            try {
                v[stationCandidate.getIndex()].setUB(0);
            } catch (IloException e) {
                throw new RuntimeException(e);
            }
        }
        for(StationCandidate stationCandidate:dataModel.fixedStations){
            try {
                v[stationCandidate.getIndex()].setLB(1);
            } catch (IloException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void buildModel() {
        try {
            cplex = new IloCplex();
            cplex.setParam(IloCplex.IntParam.AdvInd, 0);
            cplex.setParam(IloCplex.IntParam.Threads, 1);
            cplex.setOut(null);


//        int nodeNum = dataModel.instance.getStations().size() + dataModel.instance.getParcels().size() + dataModel.instance.getWorkers().size() * 2;
//        int workerNum = dataModel.instance.getWorkers().size();
            int customerNum = dataModel.instance.getCustomers().size();
//        int scenarioNum = dataModel.instance.getScenarios().size();
            int workerCapacityNum = dataModel.instance.getWorkerCapacityNum();
            int stationNum = dataModel.instance.getStationCandidates().size();

            int workerCapacity = dataModel.instance.getWorkers().get(0).getCapacity();//We assume that the capacity of each worker is homogeneous
            Worker worker = pricingProblem.worker;
            //create variables
            //second stage decision variable
            x = new IloIntVar[customerNum];
            for (int i = 0; i < customerNum; i++) {
                IloIntVar var = cplex.boolVar("x_i" + i);
                x[i] = var;
            }

            v = new IloIntVar[stationNum];
            for (int s = 0; s < stationNum; s++) {
                IloIntVar var = cplex.boolVar("v_s" + s);
                v[s] = var;
            }

            tao = cplex.boolVar("tao");
            z = new IloNumVar[stationNum];
            for (int s = 0; s < stationNum; s++) {
                IloNumVar var = cplex.numVar(0, workerCapacity, "z_s" + s);
                z[s] = var;
            }


            //for travel time prediction, 35 in total
//            theta = new IloNumVar[customerNum][stationNum]; //1. domain [0,1]
//            for (int i = 0; i < customerNum; i++) {
//                for (int s = 0; s < stationNum; s++) {
//                    IloNumVar var = cplex.numVar(0, 1, "theta_i" + i + "_s" + s);
//                    theta[i][s] = var;
//                }
//            }

            dmax1 = cplex.numVar(0, dataModel.instance.getCoordinateMax() * 1.414, "dmax1"); //2. [0,1000sqart2]

            u = new IloIntVar[workerCapacityNum + 1];//3. {0,1}
            for (int j = 0; j <= workerCapacityNum; j++) {
                IloIntVar var = cplex.boolVar("u_j" + j);
                u[j] = var;

            }
            f1 = new IloNumVar[workerCapacityNum + 1];//4. [0,1000]
            for (int j = 0; j <= workerCapacityNum; j++) {
                IloNumVar var = cplex.numVar(0, dataModel.instance.getCoordinateMax(), "f1_j" + j);
                f1[j] = var;
            }
            varTheta = new IloIntVar[customerNum][stationNum];//5. {0,1}
            for (int i = 0; i < customerNum; i++) {
                for (int s = 0; s < stationNum; s++) {

                    IloIntVar var = cplex.boolVar("varTheta_i" + i + "_s" + s);
                    varTheta[i][s] = var;

                }
            }
//             epsilon = new IloNumVar[customerNum];//6. [0,1000]
//            for (int i = 0; i < customerNum; i++) {
//                IloNumVar var = cplex.numVar(0, 1, "epsilon_i" + i);
//                epsilon[i] = var;
//
//            }
            dmax2 = cplex.numVar(0, dataModel.instance.getCoordinateMax() * 1.414, "dmax2");//7. [0,1000sqart2]

            f2 = new IloNumVar[workerCapacityNum + 1];//8. [0,1000]

            for (int j = 0; j <= workerCapacityNum; j++) {

                IloNumVar var = cplex.numVar(0, dataModel.instance.getCoordinateMax(), "f2_j" + j);
                f2[j] = var;

            }
            a1plus = cplex.numVar(0, dataModel.instance.getCoordinateMax(), "a1plus");//9. [0,1000]
//
            a1minus = cplex.numVar(-dataModel.instance.getCoordinateMax(), 0, "a1minus");//10. [-1000,0]
//
            b1plus = cplex.numVar(0, dataModel.instance.getCoordinateMax(), "b1plus");//11. [0,1000]


            b1minus = cplex.numVar(-1 * dataModel.instance.getCoordinateMax(), 0, "b1minus");//12. [-1000,0]

            int temp = (int) (dataModel.instance.getCoordinateMax() * workerCapacityNum);
            gamma9 = new IloNumVar[workerCapacityNum + 1];//25. [0,500*10]

            for (int j = 0; j <= workerCapacityNum; j++) {

                IloNumVar var = cplex.numVar(0, temp, "gamma9_j" + j);
                gamma9[j] = var;

            }

            gamma11 = new IloNumVar[workerCapacityNum + 1];//27. [0,500*10]

            for (int j = 0; j <= workerCapacityNum; j++) {

                IloNumVar var = cplex.numVar(0, temp, "gamma11_j" + j);
                gamma11[j] = var;

            }

            //Create objective: Minimize weighted travel travelTime
            objExp = cplex.linearNumExpr();

            double coe0 = GlobalVariable.daysNum;


            //1. number of location n_k^\xi
            double coe = coe0 * dataModel.instance.getModelCoe()[1];
            for (int i = 0; i < customerNum; i++) {
                objExp.addTerm(coe - coe0 * dataModel.instance.getCustomers().get(i).getUnservedPenalty(), x[i]);
            }

            //3. D_{1k}^\xi
            coe = coe0 * dataModel.instance.getModelCoe()[3];
            objExp.addTerm(coe, dmax1);
            //4. \bar{d}_{1k}^\xi
            coe = coe0 * dataModel.instance.getModelCoe()[4];
            for (int j = 0; j <= workerCapacityNum; j++) {
                objExp.addTerm(coe, f1[j]);
            }

            //6. D_{2k}^\xi
            coe = coe0 * dataModel.instance.getModelCoe()[6];
            objExp.addTerm(coe, dmax2);
            //7. \bar{d}_{2k}^\xi
            coe = coe0 * dataModel.instance.getModelCoe()[7];
            for (int j = 0; j <= workerCapacityNum; j++) {
                objExp.addTerm(coe, f2[j]);
            }

            //20. gamma_9
            coe = coe0 * dataModel.instance.getModelCoe()[20];
            for (int j = 0; j <= workerCapacityNum; j++) {
                objExp.addTerm(coe, gamma9[j]);
            }

            //22. gamma_11
            coe = coe0 * dataModel.instance.getModelCoe()[22];
            for (int j = 0; j <= workerCapacityNum; j++) {
                objExp.addTerm(coe, gamma11[j]);
            }


            obj = cplex.addMinimize();

            //create constraints

            //Second stage
            //1. worker capacity
            Scenario scenario = dataModel.instance.getScenarios().get(0);


            IloLinearIntExpr expr = cplex.linearIntExpr();
            for (int i = 0; i < customerNum; i++) {
                expr.addTerm(scenario.getCustomerDemand()[i], x[i]);
            }
            cplex.addGe(workerCapacity, expr, "worker Capacity");


            //2. worker capacity number
            expr = cplex.linearIntExpr();
            for (int i = 0; i < customerNum; i++) {
                expr.addTerm(1, x[i]);
            }
            cplex.addLe(expr, workerCapacityNum, "worker CapacityNum");

//        //3. each worker can visit one station at most
            expr = cplex.linearIntExpr();
            for (int s = 0; s < stationNum; s++) {
                expr.addTerm(1, v[s]);
            }
            cplex.addGe(1, expr, "worker visit one station");

            expr = cplex.linearIntExpr();
            for (int i = 0; i < customerNum; i++) {
                expr.addTerm(1, x[i]);
            }
            expr.addTerm(-1, tao);
            cplex.addGe(expr, 0, "tao1");
            expr = cplex.linearIntExpr();
            for (int i = 0; i < customerNum; i++) {
                expr.addTerm(1, x[i]);
            }
            expr.addTerm(-dataModel.instance.getWorkerCapacityNum(), tao);
            cplex.addLe(expr, 0, "tao2");

            expr = cplex.linearIntExpr();
            for (int s = 0; s < stationNum; s++) {
                expr.addTerm(1, v[s]);
            }
            expr.addTerm(-1, tao);
            cplex.addGe(expr, 0, "tao3");
            //6. define z

            for (int s = 0; s < stationNum; s++) {
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                for (int i = 0; i < customerNum; i++) {
                    numExpr.addTerm(scenario.getCustomerDemand()[i], x[i]);
                }
                int M = workerCapacity;
                numExpr.addTerm(M, v[s]);
                numExpr.addTerm(-1, z[s]);
                cplex.addLe(numExpr, M, "defineZ1_s" + s);
            }

            for (int s = 0; s < stationNum; s++) {

                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                for (int i = 0; i < customerNum; i++) {
                    numExpr.addTerm(scenario.getCustomerDemand()[i], x[i]);
                }
                numExpr.addTerm(-1, z[s]);
                cplex.addGe(numExpr, 0, "defineZ2_s" + s);
            }

            for (int s = 0; s < stationNum; s++) {
                int M = workerCapacity;
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                numExpr.addTerm(M, v[s]);
                numExpr.addTerm(-1, z[s]);
                cplex.addGe(numExpr, 0, "defineZ3_s" + s);

            }

            //D_1
            double M = 1.414 * dataModel.instance.getCoordinateMax();

            for (int i = 0; i < customerNum; i++) {
                for (int s = 0; s < stationNum; s++) {
                    IloLinearNumExpr numExpr = cplex.linearNumExpr();
                    numExpr.addTerm(1, dmax1);
                    numExpr.addTerm(-M, x[i]);
                    numExpr.addTerm(-M, v[s]);
                    int sIndex = dataModel.instance.getStationCandidates().get(s).getNodeIndex();
                    int iIndex = dataModel.instance.getCustomers().get(i).getNodeIndex();
                    double travelSI = dataModel.instance.getTravelCostMatrix()[sIndex][iIndex];
                    cplex.addGe(numExpr, travelSI - 2 * M, "D1_i" + i + "_s" + s);
                }
            }

            //define u

            expr = cplex.linearIntExpr();
            for (int j = 0; j <= workerCapacityNum; j++) {
                expr.addTerm(j, u[j]);
            }
            IloLinearIntExpr expr1 = cplex.linearIntExpr();
            for (int i = 0; i < customerNum; i++) {
                expr1.addTerm(1, x[i]);
            }
            cplex.addEq(expr, expr1, "u1");

            expr = cplex.linearIntExpr();
            for (int j = 0; j <= workerCapacityNum; j++) {
                expr.addTerm(1, u[j]);
            }
            cplex.addEq(expr, 1, "u2");


            //\bar{d}_1
            M = dataModel.instance.getCoordinateMax() * workerCapacityNum;

            for (int j = 0; j <= workerCapacityNum; j++) {
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                numExpr.addTerm(M, u[j]);
                numExpr.addTerm(-1, f1[j]);
                cplex.addGe(numExpr, 0, "bar{d}1_1");
            }


            for (int j = 1; j <= workerCapacityNum; j++) {
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                for (int i = 0; i < customerNum; i++) {
                    for (int s = 0; s < stationNum; s++) {
                        int sIndex = dataModel.instance.getStationCandidates().get(s).getNodeIndex();
                        int iIndex = dataModel.instance.getCustomers().get(i).getNodeIndex();
                        double travelSI = dataModel.instance.getTravelCostMatrix()[sIndex][iIndex];
                        numExpr.addTerm((travelSI * 1.0) / (j * 1.0), varTheta[i][s]);
                    }
                }
                numExpr.addTerm(-1, f1[j]);
                cplex.addGe(numExpr, 0, "bar{d}1_2");
            }


            for (int j = 1; j <= workerCapacityNum; j++) {
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                for (int i = 0; i < customerNum; i++) {
                    for (int s = 0; s < stationNum; s++) {
                        int sIndex = dataModel.instance.getStationCandidates().get(s).getNodeIndex();
                        int iIndex = dataModel.instance.getCustomers().get(i).getNodeIndex();
                        double travelSI = dataModel.instance.getTravelCostMatrix()[sIndex][iIndex];
                        numExpr.addTerm((travelSI * 1.0) / (j * 1.0), varTheta[i][s]);

                    }
                }
                numExpr.addTerm(M, u[j]);
                numExpr.addTerm(-1, f1[j]);
                cplex.addLe(numExpr, M, "bar{d}1_3");
            }


            for (int i = 0; i < customerNum; i++) {
                for (int s = 0; s < stationNum; s++) {
                    expr = cplex.linearIntExpr();
                    expr.addTerm(1, x[i]);
                    expr.addTerm(1, v[s]);
                    expr.addTerm(-2, varTheta[i][s]);
                    cplex.addGe(expr, 0, "varTheta1_i" + i + "_s" + s);
                }
            }


            for (int i = 0; i < customerNum; i++) {
                for (int s = 0; s < stationNum; s++) {
                    expr = cplex.linearIntExpr();
                    expr.addTerm(1, x[i]);
                    expr.addTerm(1, v[s]);
                    expr.addTerm(-1, varTheta[i][s]);
                    cplex.addLe(expr, 1, "varTheta2_i" + i + "_s" + s);
                }
            }


            //D_2
            M = dataModel.instance.getCoordinateMax() * 1.414;


            for (int i = 0; i < customerNum; i++) {
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                numExpr.addTerm(1, dmax2);
                numExpr.addTerm(-M, x[i]);
                int iIndex = dataModel.instance.getCustomers().get(i).getNodeIndex();
                int dIndex = worker.getIndexD();
                int travelID = dataModel.instance.getTravelCostMatrix()[iIndex][dIndex];

                IloConstraint constraint = cplex.addGe(numExpr, travelID - M, "D2_i" + i);

            }


            //\bar{d}_2
            M = dataModel.instance.getCoordinateMax() * workerCapacityNum;

            for (int j = 0; j <= workerCapacityNum; j++) {
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                numExpr.addTerm(M, u[j]);
                numExpr.addTerm(-1, f2[j]);
                cplex.addGe(numExpr, 0, "bar{d}2_1_j" + j);
            }


            for (int j = 1; j <= workerCapacityNum; j++) {
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                for (int i = 0; i < customerNum; i++) {
                    int iIndex = dataModel.instance.getCustomers().get(i).getNodeIndex();
                    int dIndex = worker.getIndexD();
                    int travelID = dataModel.instance.getTravelCostMatrix()[iIndex][dIndex];
                    numExpr.addTerm((travelID * 1.0) / (j * 1.0), x[i]);
                }
                numExpr.addTerm(-1, f2[j]);
                IloConstraint constraint = cplex.addGe(numExpr, 0, "bar{d}2_2_j" + j);

            }


            for (int j = 1; j <= workerCapacityNum; j++) {
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                for (int i = 0; i < customerNum; i++) {
                    int iIndex = dataModel.instance.getCustomers().get(i).getNodeIndex();
                    int dIndex = worker.getIndexD();
                    int travelID = dataModel.instance.getTravelCostMatrix()[iIndex][dIndex];
                    numExpr.addTerm((travelID * 1.0) / (j * 1.0), x[i]);
                }
                numExpr.addTerm(M, u[j]);
                numExpr.addTerm(-1, f2[j]);
                IloConstraint constraint = cplex.addLe(numExpr, M, "bar{d}2_3_j" + j);

            }

            //a1
            M = dataModel.instance.getCoordinateMax();

            for (int i = 0; i < customerNum; i++) {
                Customer customer = dataModel.instance.getCustomers().get(i);
                int lat = customer.getLat();
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                numExpr.addTerm(lat, x[i]);
                numExpr.addTerm(-1, a1plus);
                cplex.addLe(numExpr, 0, "a1_1_i" + i);
            }


            for (int i = 0; i < customerNum; i++) {
                Customer customer = dataModel.instance.getCustomers().get(i);
                int lat = customer.getLat();
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                numExpr.addTerm(M, x[i]);
                numExpr.addTerm(-1, a1minus);
                cplex.addLe(numExpr, M + lat, "a1_2_i" + i);
            }


            //b1

            for (int i = 0; i < customerNum; i++) {
                Customer customer = dataModel.instance.getCustomers().get(i);
                int lng = customer.getLng();
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                numExpr.addTerm(lng, x[i]);
                numExpr.addTerm(-1, b1plus);
                cplex.addLe(numExpr, 0, "b1_1_i" + i);
            }


            for (int i = 0; i < customerNum; i++) {
                Customer customer = dataModel.instance.getCustomers().get(i);
                int lng = customer.getLng();
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                numExpr.addTerm(M, x[i]);
                numExpr.addTerm(-1, b1minus);
                cplex.addLe(numExpr, M + lng, "b1_2_i" + i);
            }


//        a_1n
            M = dataModel.instance.getCoordinateMax() * workerCapacityNum;

            for (int j = 0; j <= workerCapacityNum; j++) {
                double jr = j;
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                numExpr.addTerm(M, u[j]);
                numExpr.addTerm(-1, gamma9[j]);
                cplex.addGe(numExpr, 0, "a_1n_1_j" + j);
                IloLinearNumExpr numExpr1 = cplex.linearNumExpr();
                numExpr1.addTerm(jr, a1plus);
                numExpr1.addTerm(jr, a1minus);
                numExpr1.addTerm(-1, gamma9[j]);
                cplex.addGe(numExpr1, 0, "a_1n_2_j" + j);
                IloLinearNumExpr expr2 = cplex.linearNumExpr();
                expr2.addTerm(jr, a1minus);
                expr2.addTerm(jr, a1plus);
                expr2.addTerm(M, u[j]);
                expr2.addTerm(-1, gamma9[j]);
                cplex.addLe(expr2, M, "a_1n_3_j" + j);
            }


//        b_1n
            for (int j = 0; j <= workerCapacityNum; j++) {
                double jr = j;
                IloLinearNumExpr numExpr = cplex.linearNumExpr();
                numExpr.addTerm(M, u[j]);
                numExpr.addTerm(-1, gamma11[j]);
                cplex.addGe(numExpr, 0, "b_1n_1_j" + j);
                IloLinearNumExpr numExpr1 = cplex.linearNumExpr();
                numExpr1.addTerm(jr, b1plus);
                numExpr1.addTerm(jr, b1minus);
                numExpr1.addTerm(-1, gamma11[j]);
                cplex.addGe(numExpr1, 0, "b_1n_2_j" + j);
                IloLinearNumExpr expr2 = cplex.linearNumExpr();
                expr2.addTerm(jr, b1minus);
                expr2.addTerm(jr, b1plus);
                expr2.addTerm(M, u[j]);
                expr2.addTerm(-1, gamma11[j]);
                cplex.addLe(expr2, M, "b_1n_3_j" + j);
            }
        } catch (IloException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected List<AssignmentColumn_true> generateNewColumns() throws TimeLimitExceededException {
        List<AssignmentColumn_true> newColumns = new ArrayList<>();
        try {
            /*solve the model by cplex*/
            //Compute how much time we may take to solve the pricing problem
            double timeRemaining = Math.max(1, (timeLimit - System.currentTimeMillis()) / 1000.0);
            cplex.setParam(IloCplex.DoubleParam.TiLim, timeRemaining); //set time limit in seconds
            exportModel("pricing_.lp");

            //Solve the problem and check the solution nodeStatus
            if (!cplex.solve() || cplex.getStatus() != IloCplex.Status.Optimal) {
                if (cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim) { //Aborted due to time limit
                    throw new TimeLimitExceededException();
                } else if (cplex.getStatus() == IloCplex.Status.Infeasible) { //Pricing problem infeasible
                    pricingProblemInfeasible = true;
                    this.objective = Double.MAX_VALUE;
//                    throw new InfeasibleExceededException();
//                    throw new RuntimeException("Pricing problem infeasible");
                    AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblem, true, "initial", -1000000, (short) 0, dataModel.instance.getWorkers().get(0), new HashSet<>(dataModel.instance.getCustomers()), dataModel.instance.getStationCandidates().get(0));
//
                    newColumns.add(assignmentColumn_true);
                    System.out.println("++++++++++Pricing problem infeasible");
                } else {
                    throw new RuntimeException("Pricing problem solve failed! Status: " + cplex.getStatus());
                }
            } else { //Pricing problem solved to optimality.
                this.pricingProblemInfeasible = false;
                this.objective = cplex.getObjValue();
                int demand=0;
                double[] valuesX = cplex.getValues(x); //Get the variable values

                for (int i = 0; i < valuesX.length; i++) {
                    if (MathProgrammingUtil.doubleToBoolean(valuesX[i])) {
                        demand += dataModel.instance.getScenarios().get(0).getCustomerDemand()[i];
                    }
                }
                if (objective - demand*pricingProblem.dualCostsMap.get("oneRoutePerWorkerAtMost")[pricingProblem.worker.getIndex()] <= -Constants.precisionForReducedCost) { //Generate new column if it has negative reduced cost

                    double[] valuesv = cplex.getValues(v);
                    double[] valuesz = cplex.getValues(z);
//                    double[][] valuestheta = new double[theta.length][theta[0].length];
//                    for(int i=0;i<theta.length;i++){
//                        for(int j=0;j<theta[i].length;j++){
//                        valuestheta[i][j]= cplex.getValue(theta[i][j]);}
//                    }
                    double valueDmax1 = cplex.getValue(dmax1);
                    double[] valuesu = cplex.getValues(u);
                    double[] valuesf1 = cplex.getValues(f1);
                    double[][] valuesVartheta = new double[varTheta.length][varTheta[0].length];
                    for (int i = 0; i < varTheta.length; i++) {
                        for (int j = 0; j < varTheta[i].length; j++) {
                            valuesVartheta[i][j] = cplex.getValue(varTheta[i][j]);
                        }
                    }
//                    double[] valuesepsilon = cplex.getValues(epsilon);

                    double valueDmax2 = cplex.getValue(dmax2);
                    double[] valuesf2 = cplex.getValues(f2);
                    double valuea1plus = cplex.getValue(a1plus);
                    double valuea1minus = cplex.getValue(a1minus);
                    double valueb1plus = cplex.getValue(b1plus);
                    double valueb1minus = cplex.getValue(b1minus);

                    double[] valuesgamma9 = cplex.getValues(gamma9);
                    double[] valuesgamma11 = cplex.getValues(gamma11);


                    Set<Customer> customers = new HashSet<>();
                    double cost = objective;
                    for (int i = 0; i < valuesX.length; i++) {
                        if (MathProgrammingUtil.doubleToBoolean(valuesX[i])) {
                            customers.add(dataModel.instance.getCustomers().get(i));
                            cost += pricingProblem.dualCostsMap.get("oneVisitPerCustomerAtMost")[i];
                        }
                    }
                    if (!customers.isEmpty()) {
                        double[] valuesV = cplex.getValues(v);
                        double[] valuesZ = cplex.getValues(z);
                        StationCandidate stationCandidate = new StationCandidate();
                        for (int s = 0; s < valuesV.length; s++) {
                            if (MathProgrammingUtil.doubleToBoolean(valuesV[s])) {
                                stationCandidate = dataModel.instance.getStationCandidates().get(s);
                                cost += pricingProblem.dualCostsMap.get("stationCapConstraint")[s] * valuesZ[s];
                                break;
                            }
                        }
                        double cost1 = Util.getCost(customers, pricingProblem.worker, stationCandidate, dataModel.instance);
                        if (cost - cost1 >= 0.001) {
                            cost = cost1;
                            int a = 0;
                        }
                        //Create an AssignmentColumn for the pricing problem with the specific station, worker, and customers
                        AssignmentColumn_true assignmentColumn = new AssignmentColumn_true(pricingProblem, false, "exactPricing", cost1, Util.getDemand(customers, dataModel.instance.getScenarios().get(0)), pricingProblem.worker, customers, stationCandidate);
                        newColumns.add(assignmentColumn);
//                        System.out.println("column number:"+newColumns.size());
                    }
                    //                    assert Math.abs(cost-cost1)<0.1:"cost!=cost1";
                }
            }

        } catch (IloException e) {
            e.printStackTrace();
        }


        // double obj=solvemodel
        // if(obj-\epsilon_k<Constants.epsilon){
        //newColumns.add(xxx)
        // }


        return newColumns;
    }

    public void exportModel(String fileName) {
        try {
            cplex.exportModel("./output/pricingLP/" + fileName);
        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setObjective() {
        //0. dual variable related terms
        /*min \beta_1*q_1+\beta_2*q_2+\beta_3*q_3+...-\sum_{i\in N} (\pi_i+h_i)x_i -\sum_{s\in S} \sigma_s z_s-\epsilon_k,
         where \beta is the coefficient of each feature for travel time prediction
         */
        try {
            IloLinearNumExpr objExpNew = cplex.linearNumExpr();
            for (int i = 0; i < dataModel.instance.getCustomers().size(); i++) {
                double c = pricingProblem.dualCostsMap.get("oneVisitPerCustomerAtMost")[i];
                objExpNew.addTerm(-c, x[i]);
            }
            for (int s = 0; s < dataModel.instance.getStationCandidates().size(); s++) {
                double c = pricingProblem.dualCostsMap.get("stationCapConstraint")[s];
                objExpNew.addTerm(-c, z[s]);

            }
            objExpNew.add(objExp);
            obj.clearExpr();
            obj.setExpr(objExpNew);

        } catch (IloException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void close() {
        cplex.end();
    }



}
