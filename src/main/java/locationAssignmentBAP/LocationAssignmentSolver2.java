package locationAssignmentBAP;

import FWPH.FWPHUtil;
import FWPH.model.SolutionValue;
import MIP.mipSetPartition.MipSP;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import io.Reader;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import locationAssignmentBAP.cg.column.AssignmentColumn_virtual;
import locationAssignmentBAP.cg.pricing.ColumnGenerator;
import locationAssignmentBAP.cg.pricing.ColumnGeneratorManager;
import locationAssignmentBAP.model.LocationAssignment;
import model.Instance;
import model.StationCandidate;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import util.TopK;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 2022/11/8 20:22
 */
public class LocationAssignmentSolver2 {
    double lowerBound = 0;
    double upperBound = 0;
    Map<String, double[]> dualSolution;
    private final LocationAssignment locationAssignment;

    public LocationAssignmentSolver2(LocationAssignment locationAssignment) {
        this.locationAssignment = locationAssignment;
    }

    public SolutionValue solveInstance() throws IloException, TimeLimitExceededException {
        SolutionValue solutionValue ;

        //cg --> get lower bound, solution, and dual solution
        LocationAssignmentCGSolver cgSolver = new LocationAssignmentCGSolver(locationAssignment);
        cgSolver.solveCG();
        lowerBound = cgSolver.getObjectiveValue();
        dualSolution = cgSolver.getDualCostsMap();
        upperBound=cgSolver.upperBound;
        LocationAssignmentSolver solver = new LocationAssignmentSolver(locationAssignment);
        solutionValue = solver.solveInstance();
        lowerBound = Math.max(lowerBound,solver.lowerBound);
//        dualSolution = solver.dualSolution;
        upperBound =  Math.min(upperBound,solver.upperBound);
        if (solver.isOptimal) {
            return solutionValue;
        } else {
            List<AssignmentColumn> solutionCGtrue = new ArrayList<>();
            ColumnGeneratorManager columnGenerator1 = new ColumnGeneratorManager(dualSolution, locationAssignment.instance, Double.MAX_VALUE);
            List<AssignmentColumn> columns = columnGenerator1.generateNewColumns();

            System.out.println("total columns number: " + columns.size());
            TopK topK = new TopK();
            PriorityQueue<AssignmentColumn> queue = topK.bottomK(10000, columns);
            for (AssignmentColumn column : queue) {
                solutionCGtrue.add(column);
            }
            //mip --> utilize solution to get the upper bound
            List<AssignmentColumn> solution=solveMip(solutionCGtrue,true);
            //filter the columns with reduced cost, associated with the obtained dual solution, less than the gap of upper and lower bounds
            double gap = upperBound - lowerBound;
            System.out.println("gap: " + gap + "upper: " + upperBound + "lower: " + lowerBound);

            double g =Math.abs ((upperBound - lowerBound) / lowerBound);
            System.out.println( "relative gap:" + g);
            if(g>0.01){

                List<AssignmentColumn> columnsFiltered = new ArrayList<>();
                for(AssignmentColumn assignmentColumn:columns){
                    if(assignmentColumn.reducedCost<gap){
                        columnsFiltered.add(assignmentColumn);
                    }
                }
                //mip again --> obtain the optimal solution
                System.out.println("filtered columns number: " + columnsFiltered.size());
                solution = solveMip(columnsFiltered,false);
            }


            double objFirst = 0;
            double objSecond = 0;
            double[][] x = new double[locationAssignment.instance.getStationCandidates().size()][locationAssignment.instance.getType().length];
            for (AssignmentColumn column : solution) {
                if (column instanceof AssignmentColumn_true) {
                    objSecond += ((AssignmentColumn_true) column).cost;

                } else {
                    AssignmentColumn_virtual column_virtual = (AssignmentColumn_virtual) column;
                    StationCandidate stationCandidate = locationAssignment.instance.getStationCandidates().get(column_virtual.stationIndex);
                    objFirst += stationCandidate.getFixedCost() + stationCandidate.getCapacityCost() * locationAssignment.instance.getType()[column_virtual.type];
                    x[column_virtual.stationIndex][column_virtual.type] = 1;
                }
            }
            solutionValue.setObjFirst(objFirst);
            solutionValue.setObjSecond(objSecond);
            solutionValue.setObj(objFirst + objSecond);
            solutionValue.setX(x);
        }
        return solutionValue;
    }

    private List<AssignmentColumn> solveMip(List<AssignmentColumn> columns,boolean isterminateAdv) throws IloException {
        long runTime = System.currentTimeMillis();
        System.out.println("Starting branch and bound for " + locationAssignment.instance.getName());
        MipSP mipSP = new MipSP(locationAssignment.instance, columns);
        if(isterminateAdv){
            mipSP.mipDataSP.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap,0.05);
        }else {
            mipSP.mipDataSP.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap,0.01);
        }
        mipSP.solve();
        runTime = System.currentTimeMillis() - runTime;

        if (mipSP.isFeasible()) {
            System.out.println("Objective: " + mipSP.getObjectiveValue());
            System.out.println("Runtime: " + runTime);
            System.out.println("Is optimal: " + mipSP.isOptimal());
            System.out.println("Bound: " + mipSP.getLowerBound());
            System.out.println("Nodes: " + mipSP.getNrOfNodes());
        } else {
            throw new RuntimeException("MIP infeasible!");
        }
        upperBound = Math.min(mipSP.getObjectiveValue(), upperBound);
//        lowerBound=Math.max(mipSP.getLowerBound(),lowerBound);
        return mipSP.getSolution();
    }

    public static void main(String[] args) throws FileNotFoundException, TimeLimitExceededException, IloException {
        File file = new File("dataset/instance/instance");
        Instance instance = Reader.readInstance(file, 5, 0, 5, 40, 15, 1);

//        Instance instance = Reader.readInstance(file, 5, 0, 5, 20, 2, 1);
        //get instances for each scenario
        List<Instance> instanceList = FWPHUtil.getInstances(instance);
        for (int i = 0; i < 1; i++) {
            Instance instance1 = instanceList.get(i);

            LocationAssignment locationAssignment1 = new LocationAssignment(instance1);
            LocationAssignmentSolver2 locationAssignmentSolver = new LocationAssignmentSolver2(locationAssignment1);
            SolutionValue solutionValue = locationAssignmentSolver.solveInstance();


        }
    }
}
