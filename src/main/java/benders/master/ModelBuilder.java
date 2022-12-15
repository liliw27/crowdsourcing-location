package benders.master;


import benders.optimalityCut.BendersCutCallback;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import model.Instance;
import model.StationCandidate;
import util.Constants;
import util.GlobalVariable;

/**
 * @author Wang Li
 * @description
 * @date 6/21/22 3:03 PM
 */
public class ModelBuilder {


    private Instance dataModel;
    private MipData mipData;


    public ModelBuilder(Instance dataModel) throws IloException {
        this.dataModel = dataModel;
        this.buildModel();
    }

    /**
     * Solve the root node of the Branch and Bound tree.
     */
    public MipData getLP() throws IloException {
        mipData.cplex.setParam(IloCplex.IntParam.NodeLim, 0);
        return mipData;
    }

    /**
     * Solve the entire Branch and Bound tree
     */
    public MipData getILP() throws IloException {
        mipData.cplex.setParam(IloCplex.IntParam.NodeLim, 210000000); //Continue search
        //mipDataS.cplex.setParam(IloCplex.BooleanParam.PreInd, false);
        mipData.cplex.setParam(IloCplex.Param.TimeLimit, 7200); //set time limit in seconds
        mipData.cplex.setParam(IloCplex.IntParam.Threads, 1);
        mipData.cplex.setParam(IloCplex.IntParam.NodeFileInd, 3);
        mipData.cplex.setParam(IloCplex.IntParam.WorkMem, 4096);
        mipData.cplex.setParam(IloCplex.Param.Simplex.Tolerances.Optimality, 0.00001);
        mipData.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0.01);
//        mipData.cplex.setParam(IloCplex.Param.MIP.Strategy.NodeSelect,0);

//        mipData.cplex.setOut(null); //Disable Cplex output
        return mipData;
    }

    private void buildModel() throws IloException {
        IloCplex cplex = new IloCplex();

//        int nodeNum = dataModel.getStations().size() + dataModel.getParcels().size() + dataModel.getWorkers().size() * 2;
        int stationNum = dataModel.getStationCandidates().size();


        //create variables

        //first stage variable
        IloIntVar[] varsLocation = new IloIntVar[stationNum];
        for (int i = 0; i < stationNum; i++) {
            IloIntVar var = cplex.boolVar("y_s" + i);
            varsLocation[i] = var;
        }
        IloNumVar[] varsCapacity = new IloNumVar[stationNum];
        for (int i = 0; i < stationNum; i++) {
            IloNumVar var = cplex.numVar(0, dataModel.getStationCandidates().get(i).getCapaUpper(), "w_s" + i);
            varsCapacity[i] = var;
        }
        IloNumVar[] varsQ = new IloNumVar[dataModel.getScenarios().size()];
        for (int xi = 0; xi < dataModel.getScenarios().size(); xi++) {
            IloNumVar var = cplex.numVar(0, Integer.MAX_VALUE, "varQ_" + xi);
            varsQ[xi] = var;
        }

        IloNumVar[] varst = new IloNumVar[dataModel.getScenarios().size()];
        for (int xi = 0; xi < dataModel.getScenarios().size(); xi++) {
            IloNumVar var = cplex.numVar(0, Integer.MAX_VALUE, "vart_" + xi);
            varst[xi] = var;
        }
        IloNumVar varQ = cplex.numVar(0, Integer.MAX_VALUE, "varQ");
        IloNumVar vart = cplex.numVar(0, Integer.MAX_VALUE, "vart");
//        IloNumVar varQ= cplex.numVar(-100000,Integer.MAX_VALUE,"varQ");
//        IloNumVar vart= cplex.numVar(-100000,Integer.MAX_VALUE,"vart");
        IloNumVar varz = cplex.numVar(Integer.MIN_VALUE, Integer.MAX_VALUE, "varz");


        //Create objective: Minimize weighted travel travelTime
        IloLinearNumExpr obj = cplex.linearNumExpr();
        for (int s = 0; s < stationNum; s++) {
            StationCandidate stationCandidate = dataModel.getStationCandidates().get(s);
            double fixedCost = stationCandidate.getFixedCost();
            double capCost = stationCandidate.getCapacityCost();
            obj.addTerm(fixedCost, varsLocation[s]);
            obj.addTerm(capCost, varsCapacity[s]);
        }
        if (dataModel.isMultipleCut()) {
            if (!dataModel.isCVaR()) {
                for (int xi = 0; xi < dataModel.getScenarios().size(); xi++) {
                    obj.addTerm(dataModel.getScenarios().get(xi).getProbability(), varsQ[xi]);
                }
            } else {
                obj.addTerm(GlobalVariable.lambda, varz);
                for (int xi = 0; xi < dataModel.getScenarios().size(); xi++) {
                    obj.addTerm((1 - GlobalVariable.lambda) * dataModel.getScenarios().get(xi).getProbability(), varsQ[xi]);
                    obj.addTerm(GlobalVariable.lambda / (1 - GlobalVariable.alpha) * dataModel.getScenarios().get(xi).getProbability(), varst[xi]);
                }
            }

        } else {
            if (!dataModel.isCVaR()) {
                obj.addTerm(1, varQ);
            } else {
                obj.addTerm(1 - GlobalVariable.lambda, varQ);
                obj.addTerm(GlobalVariable.lambda, varz);
                obj.addTerm(GlobalVariable.lambda / (1 - GlobalVariable.alpha), vart);
            }

        }


        cplex.addMinimize(obj);

        //create constraints
        //First stage station capacity upper and lower bound
        for (int s = 0; s < stationNum; s++) {
            IloLinearNumExpr expr = cplex.linearNumExpr();
            int lowerCap = dataModel.getStationCandidates().get(s).getCapaLower();
            int upperCap = dataModel.getStationCandidates().get(s).getCapaUpper();
            expr.addTerm(-lowerCap, varsLocation[s]);
            expr.addTerm(1, varsCapacity[s]);
            cplex.addGe(expr, 0, "station capacity selection lower_" + s);
            expr = cplex.linearNumExpr();
            expr.addTerm(-upperCap, varsLocation[s]);
            expr.addTerm(1, varsCapacity[s]);
            cplex.addLe(expr, 0, "station capacity selection upper_" + s);
        }
        IloLinearIntExpr expr = cplex.linearIntExpr();
        for (int s = 0; s < stationNum; s++) {
            expr.addTerm(1, varsLocation[s]);
        }
        cplex.addGe(expr, 1, "at least one location");
        if (dataModel.isMultipleCut() && dataModel.isCVaR()) {

            for (int xi = 0; xi < dataModel.getScenarios().size(); xi++) {
                IloLinearNumExpr expr0 = cplex.linearNumExpr();
                expr0.addTerm(1, varsQ[xi]);
                expr0.addTerm(-1, varz);
                expr0.addTerm(-1, varst[xi]);
                cplex.addLe(expr0, 0, "cvar_" + xi);
            }
        }
//        IloLinearNumExpr expr0 = cplex.linearNumExpr();
//        expr0.addTerm(1,varQ);
//        expr0.addTerm(-1,varz);
//        expr0.addTerm(-1,vart);
//        cplex.addLe(expr0, 0, "CVaR" );


        mipData = new MipData(cplex, varsLocation, varsCapacity, varsQ, varQ,varst, vart, varz);
        cplex.exportModel("mip.lp");
        cplex.use(new BendersCutCallback(dataModel, mipData));
    }
}
