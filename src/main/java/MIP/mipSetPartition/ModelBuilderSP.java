package MIP.mipSetPartition;


import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import model.Customer;
import model.Instance;
import model.Scenario;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;
import util.Constants;
import util.GlobalVariable;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 6/21/22 3:03 PM
 */
public class ModelBuilderSP {


    private Instance dataModel;
    private MipDataSP mipDataSP;
    private List<AssignmentColumn> columns;
    private double[][] fixedLocationSolution;

    public ModelBuilderSP(Instance dataModel, List<AssignmentColumn> columns) throws IloException {
        this.dataModel = dataModel;
        this.columns=columns;
        this.buildModel();
    }
    public ModelBuilderSP(Instance dataModel, List<AssignmentColumn> columns,double[][] fixedLocationSolution) throws IloException {
        this.dataModel = dataModel;
        this.columns=columns;
        this.fixedLocationSolution=fixedLocationSolution;
        this.buildModel(fixedLocationSolution);
    }


    /**
     * Solve the root node of the Branch and Bound tree.
     */
    public MipDataSP getLP() throws IloException {
        mipDataSP.cplex.setParam(IloCplex.IntParam.NodeLim, 0);
        return mipDataSP;
    }

    /**
     * Solve the entire Branch and Bound tree
     */
    public MipDataSP getILP() throws IloException {
        mipDataSP.cplex.setParam(IloCplex.IntParam.NodeLim, 210000000); //Continue search
        //mipDataS.cplex.setParam(IloCplex.BooleanParam.PreInd, false);
        mipDataSP.cplex.setParam(IloCplex.DoubleParam.TimeLimit, 480); //set time limit in seconds
        mipDataSP.cplex.setParam(IloCplex.IntParam.Threads, Constants.MAXTHREADS);
        mipDataSP.cplex.setParam(IloCplex.IntParam.NodeFileInd, 3);
        mipDataSP.cplex.setParam(IloCplex.IntParam.WorkMem, 4096);
        mipDataSP.cplex.setParam(IloCplex.Param.Simplex.Tolerances.Optimality,0.1);


        mipDataSP.cplex.setOut(null); //Disable Cplex output
        return mipDataSP;
    }

    private void buildModel() throws IloException {
        IloCplex cplex = new IloCplex();

//        int nodeNum = dataModel.getStations().size() + dataModel.getParcels().size() + dataModel.getWorkers().size() * 2;
        int stationNum = dataModel.getStationCandidates().size();
        int workerNum = dataModel.getWorkers().size();
        int customerNum = dataModel.getCustomers().size();
        int scenarioNum = dataModel.getScenarios().size();
        int workerCapacityNum = dataModel.getWorkerCapacityNum();
        int typeNum=dataModel.getType().length;

        //create variables

        //first stage variable
        IloIntVar[][] varsLocation = new IloIntVar[stationNum][typeNum];
        for (int i = 0; i < stationNum; i++) {
            for(int j=0;j<typeNum;j++){
                IloIntVar var = cplex.boolVar("y_s" + i+"_t"+j);
                varsLocation[i][j] = var;
            }

        }
        IloIntVar[] vars=new IloIntVar[columns.size()];
        for(int i=0;i<columns.size();i++){
            IloIntVar var=cplex.boolVar("xi_"+i);
            vars[i]=var;
        }


        //Create objective: Minimize weighted travel travelTime
        IloLinearNumExpr obj = cplex.linearNumExpr();
        for (int s = 0; s < stationNum; s++) {
            StationCandidate stationCandidate=dataModel.getStationCandidates().get(s);
            for(int t=0;t<typeNum;t++){
                double coe=stationCandidate.getFixedCost()+stationCandidate.getCapacityCost()*dataModel.getType()[t];
                obj.addTerm((coe*1.0)+dataModel.getLambda()[s][t], varsLocation[s][t]);
            }
        }

        for(int i=0;i<columns.size();i++){
            if(columns.get(i) instanceof AssignmentColumn_true){
                AssignmentColumn_true column_true=(AssignmentColumn_true) columns.get(i);
                obj.addTerm(column_true.cost,vars[i]);
            }
        }

        cplex.addMinimize(obj);

        //create constraints
        //First stage station capacity upper and lower bound
        for (int s = 0; s < stationNum; s++) {
            for(int t=0;t<typeNum;t++){
                IloLinearIntExpr expr = cplex.linearIntExpr();
                expr.addTerm(1, varsLocation[s][t]);
                cplex.addLe(expr, 1, "station capacity selection_" + s);
            }
        }
        //Second stage
        //1. each customer visited at most once
        for(int i=0;i<customerNum;i++){
            IloLinearIntExpr expr = cplex.linearIntExpr();
            Customer customer=dataModel.getCustomers().get(i);
            for(int j=0;j<columns.size();j++){
                if(columns.get(j) instanceof AssignmentColumn_true){
                    AssignmentColumn_true column_true=(AssignmentColumn_true) columns.get(j);
                    if(column_true.customers.contains(customer)){
                        expr.addTerm(1,vars[j]);
                    }
                }
            }
            cplex.addLe(expr, 1, "customer visited at most once_" + i);
        }
        //2.station capacity
        for(int s=0;s<stationNum;s++){
            IloLinearIntExpr expr = cplex.linearIntExpr();
            StationCandidate stationCandidate=dataModel.getStationCandidates().get(s);
            for(int j=0;j<columns.size();j++){
                if(columns.get(j) instanceof AssignmentColumn_true){
                    AssignmentColumn_true column_true=(AssignmentColumn_true) columns.get(j);
                    if(column_true.stationCandidate.equals(stationCandidate)){
                        expr.addTerm(column_true.demand,vars[j]);
                    }
                }
            }
            for(int t=0;t<typeNum;t++){
                expr.addTerm(-dataModel.getType()[t],varsLocation[s][t]);
            }
            cplex.addLe(expr, 0, "station capacity_s" + s);
        }
        //3. each worker implements one trip

        for(int k=0;k<workerNum;k++){
            Worker worker=dataModel.getWorkers().get(k);
            IloLinearIntExpr expr = cplex.linearIntExpr();
            for(int j=0;j<columns.size();j++){
                if(columns.get(j) instanceof AssignmentColumn_true){
                    AssignmentColumn_true column_true=(AssignmentColumn_true) columns.get(j);
                    if(column_true.worker.equals(worker)){
                        expr.addTerm(1,vars[j]);
                    }
                }
            }

            cplex.addLe(expr, 1, "at most one trip per worker_k" + k);
        }

        mipDataSP = new MipDataSP(cplex, varsLocation, vars,columns);
    }
    private void buildModel(double[][]fixedLocationSolution) throws IloException {
        IloCplex cplex = new IloCplex();

//        int nodeNum = dataModel.getStations().size() + dataModel.getParcels().size() + dataModel.getWorkers().size() * 2;
        int stationNum = dataModel.getStationCandidates().size();
        int workerNum = dataModel.getWorkers().size();
        int customerNum = dataModel.getCustomers().size();
        int scenarioNum = dataModel.getScenarios().size();
        int workerCapacityNum = dataModel.getWorkerCapacityNum();
        int typeNum=dataModel.getType().length;

        //create variables

        //first stage variable
        IloIntVar[][] varsLocation = new IloIntVar[stationNum][typeNum];
        for (int i = 0; i < stationNum; i++) {
            for(int j=0;j<typeNum;j++){
                IloIntVar var = cplex.boolVar("y_s" + i+"_t"+j);
                if(MathProgrammingUtil.doubleToBoolean(fixedLocationSolution[i][j])){
                    var.setLB(1);
                }else {
                    var.setUB(0);
                }
                varsLocation[i][j] = var;

            }
        }
        IloIntVar[] vars=new IloIntVar[columns.size()];
        for(int i=0;i<columns.size();i++){
            IloIntVar var=cplex.boolVar("xi_"+i);
            vars[i]=var;
        }


        //Create objective: Minimize weighted travel travelTime
        IloLinearNumExpr obj = cplex.linearNumExpr();
        for (int s = 0; s < stationNum; s++) {
            StationCandidate stationCandidate=dataModel.getStationCandidates().get(s);
            for(int t=0;t<typeNum;t++){
                double coe=stationCandidate.getFixedCost()+stationCandidate.getCapacityCost()*dataModel.getType()[t];
                obj.addTerm((coe*1.0)+dataModel.getLambda()[s][t], varsLocation[s][t]);
            }
        }

        for(int i=0;i<columns.size();i++){
            if(columns.get(i) instanceof AssignmentColumn_true){
                AssignmentColumn_true column_true=(AssignmentColumn_true) columns.get(i);
                obj.addTerm(column_true.cost,vars[i]);
            }
        }

        cplex.addMinimize(obj);

        //create constraints
        //First stage station capacity upper and lower bound
        for (int s = 0; s < stationNum; s++) {
            for(int t=0;t<typeNum;t++){
                IloLinearIntExpr expr = cplex.linearIntExpr();
                expr.addTerm(1, varsLocation[s][t]);
                cplex.addLe(expr, 1, "station capacity selection_" + s);
            }
        }
        //Second stage
        //1. each customer visited at most once
        for(int i=0;i<customerNum;i++){
            IloLinearIntExpr expr = cplex.linearIntExpr();
            Customer customer=dataModel.getCustomers().get(i);
            for(int j=0;j<columns.size();j++){
                if(columns.get(j) instanceof AssignmentColumn_true){
                    AssignmentColumn_true column_true=(AssignmentColumn_true) columns.get(j);
                    if(column_true.customers.contains(customer)){
                        expr.addTerm(1,vars[j]);
                    }
                }
            }
            cplex.addLe(expr, 1, "customer visited at most once_" + i);
        }
        //2.station capacity
        for(int s=0;s<stationNum;s++){
            IloLinearIntExpr expr = cplex.linearIntExpr();
            StationCandidate stationCandidate=dataModel.getStationCandidates().get(s);
            for(int j=0;j<columns.size();j++){
                if(columns.get(j) instanceof AssignmentColumn_true){
                    AssignmentColumn_true column_true=(AssignmentColumn_true) columns.get(j);
                    if(column_true.stationCandidate.equals(stationCandidate)){
                        expr.addTerm(column_true.demand,vars[j]);
                    }
                }
            }
            for(int t=0;t<typeNum;t++){
                expr.addTerm(-dataModel.getType()[t],varsLocation[s][t]);
            }
            cplex.addLe(expr, 0, "station capacity_s" + s);
        }
        //3. each worker implements one trip

        for(int k=0;k<workerNum;k++){
            Worker worker=dataModel.getWorkers().get(k);
            IloLinearIntExpr expr = cplex.linearIntExpr();
            for(int j=0;j<columns.size();j++){
                if(columns.get(j) instanceof AssignmentColumn_true){
                    AssignmentColumn_true column_true=(AssignmentColumn_true) columns.get(j);
                    if(column_true.worker.equals(worker)){
                        expr.addTerm(1,vars[j]);
                    }
                }
            }

            cplex.addLe(expr, 1, "at most one trip per worker_k" + k);
        }

        mipDataSP = new MipDataSP(cplex, varsLocation, vars,columns);
    }
}
