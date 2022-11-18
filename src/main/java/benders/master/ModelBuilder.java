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
        mipData.cplex.setParam(IloCplex.DoubleParam.TimeLimit, 7200); //set time limit in seconds
        mipData.cplex.setParam(IloCplex.IntParam.Threads, Constants.MAXTHREADS);
        mipData.cplex.setParam(IloCplex.IntParam.NodeFileInd, 3);
        mipData.cplex.setParam(IloCplex.IntParam.WorkMem, 4096);
        mipData.cplex.setParam(IloCplex.Param.Simplex.Tolerances.Optimality, 0.1);


        mipData.cplex.setOut(null); //Disable Cplex output
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
        IloIntVar[] varsCapacity = new IloIntVar[stationNum];
        for (int i = 0; i < stationNum; i++) {
            IloIntVar var = cplex.intVar(0, dataModel.getStationCandidates().get(i).getCapaUpper(), "w_s" + i);
            varsCapacity[i] = var;
        }
        IloIntVar[] varsQ= new IloIntVar[dataModel.getScenarios().size()];
        for (int xi=0; xi<dataModel.getScenarios().size();xi++){
            IloIntVar var = cplex.intVar(-100000,Integer.MAX_VALUE,"varQ_"+xi);
            varsQ[xi]=var;
        }
        IloIntVar varQ= cplex.intVar(-100000,Integer.MAX_VALUE,"varQ");;


        //Create objective: Minimize weighted travel travelTime
        IloLinearNumExpr obj = cplex.linearNumExpr();
        for (int s = 0; s < stationNum; s++) {
            StationCandidate stationCandidate = dataModel.getStationCandidates().get(s);
            double fixedCost = stationCandidate.getFixedCost();
            double capCost = stationCandidate.getCapacityCost();
            obj.addTerm(fixedCost, varsLocation[s]);
            obj.addTerm(capCost, varsCapacity[s]);
        }
        if(dataModel.isMultipleCut()){
            for (int xi=0; xi<dataModel.getScenarios().size();xi++){
                obj.addTerm(1.0*dataModel.getScenarios().get(xi).getProbability(),varsQ[xi]);
            }
        }else{
            obj.addTerm(1.0,varQ);
        }



        cplex.addMinimize(obj);

        //create constraints
        //First stage station capacity upper and lower bound
        for (int s = 0; s < stationNum; s++) {
            IloLinearNumExpr expr = cplex.linearNumExpr();
            int lowerCap= dataModel.getStationCandidates().get(s).getCapaLower();
            int upperCap= dataModel.getStationCandidates().get(s).getCapaUpper();
            expr.addTerm( -lowerCap, varsLocation[s]);
            expr.addTerm(1, varsCapacity[s]);
            cplex.addGe(expr,0, "station capacity selection lower_" + s);
            expr = cplex.linearNumExpr();
            expr.addTerm( -upperCap, varsLocation[s]);
            expr.addTerm(1, varsCapacity[s]);
            cplex.addLe(expr, 0, "station capacity selection upper_" + s);
        }
        IloLinearIntExpr expr = cplex.linearIntExpr();
        for(int s = 0; s < stationNum; s++) {
            expr.addTerm(1,varsLocation[s]);
        }
        cplex.addGe(expr, 1, "at least one location" );


        mipData = new MipData(cplex, varsLocation, varsCapacity, varsQ,varQ);
        cplex.exportModel("mip.lp");
        cplex.use(new BendersCutCallback(dataModel, mipData));
    }
}
