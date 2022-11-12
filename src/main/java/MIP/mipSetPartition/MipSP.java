package MIP.mipSetPartition;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.cg.pricing.PricingProblem;
import lombok.Data;
import model.Customer;
import model.Instance;
import model.Solution;
import model.Solution1stage;
import model.Solution2Stage;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.util.Configuration;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;
import util.Constants;
import util.GlobalVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 6/21/22 3:02 PM
 */
@Data
public class MipSP {
    private Instance dataModel;
    private ModelBuilderSP modelBuilderSP;
    public MipDataSP mipDataSP;

    private int objectiveValue = -1; //Best objective found after MIP
    private int bestObjValue = -1; //Best objective found after MIP
    private boolean optimal = false; //Solution is optimal
    //    private Solution solution; //Best solution
    private boolean isFeasible = true; //Solution is feasible
    private List<AssignmentColumn> solution ;

//    private List<AssignmentColumn> columns;
    private int stationNum;
    private int workerNum;
    private int customerNum;
    private int typeNum;

    public MipSP(Instance instance, List<AssignmentColumn> columns) {
        this.dataModel = instance;
//        this.columns=columns;
        try {
            modelBuilderSP = new ModelBuilderSP(dataModel,columns);
            mipDataSP = modelBuilderSP.getILP();
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

        if (mipDataSP.cplex.solve() && (mipDataSP.cplex.getStatus() == IloCplex.Status.Feasible || mipDataSP.cplex.getStatus() == IloCplex.Status.Optimal)) {
            this.objectiveValue = (int) Math.round(mipDataSP.cplex.getObjValue());// MathProgrammingUtil.doubleToInt(mipDataS.cplex.getObjValue());//(int)Math.round(mipDataS.cplex.getObjValue());
            System.out.println("*************"+ mipDataSP.cplex.getObjValue());
            this.bestObjValue = (int) Math.round(mipDataSP.cplex.getBestObjValue());
            System.out.println(mipDataSP.cplex.getBestObjValue());
            //this.printSolution();
            this.optimal = mipDataSP.cplex.getStatus() == IloCplex.Status.Optimal;
            this.isFeasible = true;

        } else if (mipDataSP.cplex.getStatus() == IloCplex.Status.Infeasible) {
//			throw new RuntimeException("Mip infeasible");
            this.isFeasible = false;
            this.optimal = true;
        } else if (mipDataSP.cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim) {
            System.out.println("No solution could be found in the given amount of time");
            this.isFeasible = true; //Technically there is no proof whether or not a feasible solution exists
            this.optimal = false;
        } else {
            //NOTE: when cplex does not find a solution before the default time out, it throws a Status Unknown exception
            //NOTE2: Might be required to extend the default runtime.
            throw new RuntimeException("Cplex solve terminated with status: " + mipDataSP.cplex.getStatus());
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
        return mipDataSP.cplex.getNnodes();
    }

    public List<AssignmentColumn> getSolution() {
        List<AssignmentColumn> solution = new ArrayList<>();
        try {
                //Iterate over each column and add it to the solution if it has a non-zero value
                for (int i = 0; i < mipDataSP.vars.length; i++) {
                    mipDataSP.columns.get(i).value = mipDataSP.cplex.getValue(mipDataSP.vars[i]);
                    if (mipDataSP.columns.get(i).value >= Constants.EPSILON) {
                        solution.add(mipDataSP.columns.get(i));
                        System.out.println(mipDataSP.columns.get(i));
                    }
                }

            for (int s = 0; s < dataModel.getStationCandidates().size(); s++) {
                for (int t = 0; t < dataModel.getType().length; t++) {
                    double value = mipDataSP.cplex.getValue(mipDataSP.varsLocation[s][t]);
                    if (value >= Constants.PRECISION) {
                        AssignmentColumn_virtual assignmentColumn_virtual = new AssignmentColumn_virtual(null, false, "virtualAssignmentColumn", s, t);
                        assignmentColumn_virtual.value = value;
                        solution.add(assignmentColumn_virtual);
                        System.out.println(assignmentColumn_virtual);
                    }
                }
            }
            mipDataSP.cplex.end();
        } catch (IloException e) {
            e.printStackTrace();
        }

        return solution;
    }
}
