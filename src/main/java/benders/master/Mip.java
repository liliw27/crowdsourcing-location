package benders.master;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import lombok.Data;
import model.Instance;
import model.Station;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;
import util.GlobalVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 6/21/22 3:02 PM
 */
@Data
public class Mip {
    private Instance dataModel;
    private ModelBuilder modelBuilder;
    public MipData mipData;

    private int objectiveValue = -1; //Best objective found after MIP
    private int bestObjValue = -1; //Best objective found after MIP
    private boolean optimal = false; //Solution is optimal
    //    private Solution solution; //Best solution
    private boolean isFeasible = true; //Solution is feasible
    private List<Station> solution;

    //    private List<AssignmentColumn> columns;
    private int stationNum;
    private int workerNum;
    private int customerNum;
    private int typeNum;
    private double expectedObj = 0;
    private double secondStageObj;
    private double firstStageObj;
    private double CVaR;

    public Mip(Instance instance) {
        this.dataModel = instance;

        try {
            modelBuilder = new ModelBuilder(dataModel);
            mipData = modelBuilder.getILP();
        } catch (IloException e) {
            e.printStackTrace();
        }
    }


    public void solve() throws IloException {
//        mipDataSP.cplex.exportModel("mip.lp");
        //mipDataS.cplex.writeParam(arg0)
//		mipDataS.cplex.exportModel("mip.sav");
//		mipDataS.cplex.exportModel("mip.mps");
//		mipDataS.cplex.writeMIPStart("start.mst");
//		mipDataS.cplex.writeParam("param.prm");
//		mipDataS.cplex.setParam(BooleanParam.PreInd, false); //Disable presolve.
//		mipDataS.cplex.setParam(IntParam.Threads, 2);

        if (mipData.cplex.solve() && (mipData.cplex.getStatus() == IloCplex.Status.Feasible || mipData.cplex.getStatus() == IloCplex.Status.Optimal)) {
            this.objectiveValue = (int) Math.round(mipData.cplex.getObjValue());// MathProgrammingUtil.doubleToInt(mipDataS.cplex.getObjValue());//(int)Math.round(mipDataS.cplex.getObjValue());
            System.out.println("*************" + mipData.cplex.getObjValue());
            this.bestObjValue = (int) Math.round(mipData.cplex.getBestObjValue());
            System.out.println(mipData.cplex.getBestObjValue());
            solution = getSolution();
            //this.printSolution();
            this.optimal = mipData.cplex.getStatus() == IloCplex.Status.Optimal;
            this.isFeasible = true;
            if (dataModel.isMultipleCut()) {
                double[] valuesQ = mipData.cplex.getValues(mipData.varsQ);
                for (int xi = 0; xi < dataModel.getScenarios().size(); xi++) {
                    this.expectedObj += dataModel.getScenarios().get(xi).getProbability() * valuesQ[xi];
                }
                if (dataModel.isCVaR()) {
                    double[] valuest = mipData.cplex.getValues(mipData.varst);
                    for (int xi = 0; xi < dataModel.getScenarios().size(); xi++) {
                        this.CVaR += dataModel.getScenarios().get(xi).getProbability() * valuest[xi] / (1 - GlobalVariable.alpha) ;
                        System.out.println("xi_"+xi+" valuet: "+valuest[xi]+" valueq: "+valuesQ[xi]);

                    }
                    if(this.CVaR>10000){
                        this.CVaR= mipData.cplex.getValue(mipData.varz);

                    }else {
                        this.CVaR+= mipData.cplex.getValue(mipData.varz);
                    }
                    System.out.println(" valuez: "+mipData.cplex.getValue(mipData.varz));
                }

            } else {
                this.expectedObj = mipData.cplex.getValue(mipData.varQ);
                if (dataModel.isCVaR()) {
                    double vartValue=mipData.cplex.getValue(mipData.vart);
                    double varzValue=mipData.cplex.getValue(mipData.varz);
                    this.CVaR = vartValue / (1 - GlobalVariable.alpha) + varzValue;
                }
            }

            this.secondStageObj = GlobalVariable.lambda * this.CVaR + (1 - GlobalVariable.lambda) * this.expectedObj;
            this.firstStageObj = this.objectiveValue - secondStageObj;
        } else if (mipData.cplex.getStatus() == IloCplex.Status.Infeasible) {
//			throw new RuntimeException("Mip infeasible");
            this.isFeasible = false;
            this.optimal = true;
        } else if (mipData.cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim) {
            System.out.println("No solution could be found in the given amount of time");
            this.isFeasible = true; //Technically there is no proof whether or not a feasible solution exists
            this.optimal = false;
        } else {
            //NOTE: when cplex does not find a solution before the default time out, it throws a Status Unknown exception
            //NOTE2: Might be required to extend the default runtime.
            throw new RuntimeException("Cplex solve terminated with status: " + mipData.cplex.getStatus());
        }

    }

    /**
     * Get bound on objective value
     */
    public double getLowerBound() {

        if (this.isOptimal())
            return this.getObjectiveValue();
        else
            return this.getBestObjValue();


    }

    /**
     * Indicates whether solution is optimal
     */
    public boolean isOptimal() {
        return optimal;
    }

    /**
     * Returns size of search tree (nr of nodes)
     */
    public int getNrOfNodes() {
        return mipData.cplex.getNnodes();
    }

    public List<Station> getSolution() {
        List<Station> solution = new ArrayList<>();
        try {

            for (int s = 0; s < dataModel.getStationCandidates().size(); s++) {
                double value = mipData.cplex.getValue(mipData.varsLocation[s]);
                if (MathProgrammingUtil.doubleToBoolean(value)) {
                    Station station = (Station) dataModel.getStationCandidates().get(s);
                    station.setCapacity(mipData.cplex.getValue(mipData.varsCapacity[s]));
                    System.out.println(station);
                    solution.add(station);
                }
            }

//            mipData.cplex.end();
        } catch (IloException e) {
            e.printStackTrace();
        }

        return solution;
    }
}
