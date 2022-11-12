package MIP.mip2ndStage;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import lombok.Data;
import model.Customer;
import model.Instance;
import model.Solution;
import model.Solution1stage;
import model.Solution2Stage;
import model.Station;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;
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
public class Mip2ndStage {
    private Instance dataModel;
    private ModelBuilder2nd modelBuilder2nd;
    private MipData2nd mipData2nd;
    int stationNum;
    int workerNum;
    int customerNum;
    int scenarioNum;
    int workerCapacityNum;
    //first stage variable
    int[][] varsLocationValues;
    //second stage decision variable
    double[][][] varsAssignIKValues;
    double[][][] varsAssignSKValues;
    double[][] unServedValues;
    double[][][] zValues;
    //for travel time prediction, 35 in total
    double[][][][] thetaValues; //1. domain [0,1]
    double[][] dmax1Values; //2. [0,1000sqart2]
    double[][][] uValues;//3. {0,1}
    double[][][] f1Values;//4. [0,1000]
    double[][][][] varThetaValues;//5. {0,1}
    double[][][] epsilonValues;//6. [0,1000]
    double[][] dmax2Values;//7. [0,1000sqart2]
    double[][][] f2Values;//8. [0,1000]
    double[][] a1plusValues;//9. [0,1000]
    double[][] a1minusValues;//10. [-1000,0]
    double[][] b1plusValues;//11. [0,1000]
    double[][] b1minusValues;//12. [-1000,0]
    double[][] a2plusValues;//13. [0,1000]
    double[][] a2minusValues;//14. [-1000,0]
    double[][] b2plusValues;//15. [0,1000]
    double[][] b2minusValues;//16. [-1000,0]
    double[][][] gamma1Values;//17. [0,500*10^-0.5]
    double[][][] gamma2Values;//18. [0,500*10^-0.5]
    double[][][] gamma3Values;//19. [0,500*10^-0.5]
    double[][][] gamma4Values;//20. [0,500*10^-0.5]
    double[][][] gamma5Values;//21. [0,500*10^0.5]
    double[][][] gamma6Values;//22. [0,500*10^0.5]
    double[][][] gamma7Values;//23. [0,500*10^0.5]
    double[][][] gamma8Values;//24. [0,500*10^0.5]
    double[][][] gamma9Values;//25. [0,500*10]
    double[][][] gamma10Values;//26. [0,500*10]
    double[][][] gamma11Values;//27. [0,500*10]
    double[][][] gamma12Values;//28. [0,500*10]
    double[][][] gamma13Values;//29. [0,500*10^1.5]
    double[][][] gamma14Values;//30. [0,500*10^1.5]
    double[][][] gamma15Values;//31. [0,500*10^1.5]
    double[][][] gamma16Values;//32. [0,500*10^1.5]~[0,1000*10^1.5]
    double[][][][] xPrimeValues;//33. {0,1}
    double[][][] deltaValues;//34. [0,200]
    double[][][] deltaPrimeValues;// 35. [0,200]
    private int objectiveValue = -1; //Best objective found after MIP
    private int bestObjValue = -1; //Best objective found after MIP
    private boolean optimal = false; //Solution is optimal
    //    private Solution solution; //Best solution
    private boolean isFeasible = true; //Solution is feasible
    private Solution solution;
    int nodeNum;
    int TotalSceNum;

    int typeNum;
    double[][] x;
    List<Station> stations = new ArrayList<>();

    public Mip2ndStage(Instance instance, int TotalSceNum,double[][] x) {
        this.dataModel = instance;
        workerNum = dataModel.getWorkers().size();
        customerNum = dataModel.getCustomers().size();
        scenarioNum = dataModel.getScenarios().size();
        workerCapacityNum = dataModel.getWorkerCapacityNum();
        typeNum=dataModel.getType().length;
        for (int s = 0; s < x.length; s++) {
            for (int t = 0; t < x[s].length; t++) {
                if (x[s][t] == 1) {
                    Station station = (Station) (instance.getStationCandidates().get(s));
                    station.setCapacity(instance.getType()[t]);
                    stations.add(station);
                }
            }
        }
        this.x=x;
        stationNum = stations.size();
        varsLocationValues=new int [x.length][x[0].length];
        //second stage decision variable
        varsAssignIKValues = new double[customerNum][workerNum][scenarioNum];
        varsAssignSKValues = new double[stationNum][workerNum][scenarioNum];
        unServedValues = new double[customerNum][scenarioNum];
        zValues = new double[stationNum][workerNum][scenarioNum];
        //for travel time prediction, 35 in total
        thetaValues = new double[customerNum][stationNum][workerNum][scenarioNum]; //1. domain [0,1]
        dmax1Values = new double[workerNum][scenarioNum]; //2. [0,1000sqart2]
        uValues = new double[workerNum][workerCapacityNum+1][scenarioNum];//3. {0,1}
        f1Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//4. [0,1000]
        varThetaValues = new double[customerNum][stationNum][workerNum][scenarioNum];//5. {0,1}
        epsilonValues = new double[customerNum][workerNum][scenarioNum];//6. [0,1000]
        dmax2Values = new double[workerNum][scenarioNum];//7. [0,1000sqart2]
        f2Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//8. [0,1000]
        a1plusValues = new double[workerNum][scenarioNum];//9. [0,1000]
        a1minusValues = new double[workerNum][scenarioNum];//10. [-1000,0]
        b1plusValues = new double[workerNum][scenarioNum];//11. [0,1000]
        b1minusValues = new double[workerNum][scenarioNum];//12. [-1000,0]
        a2plusValues = new double[workerNum][scenarioNum];//13. [0,1000]
        a2minusValues = new double[workerNum][scenarioNum];//14. [-1000,0]
        b2plusValues = new double[workerNum][scenarioNum];//15. [0,1000]
        b2minusValues = new double[workerNum][scenarioNum];//16. [-1000,0]
        gamma1Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//17. [0,500*10^-0.5]
        gamma2Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//18. [0,500*10^-0.5]
        gamma3Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//19. [0,500*10^-0.5]
        gamma4Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//20. [0,500*10^-0.5]
        gamma5Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//21. [0,500*10^0.5]
        gamma6Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//22. [0,500*10^0.5]
        gamma7Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//23. [0,500*10^0.5]
        gamma8Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//24. [0,500*10^0.5]
        gamma9Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//25. [0,500*10]
        gamma10Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//26. [0,500*10]
        gamma11Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//27. [0,500*10]
        gamma12Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//28. [0,500*10]
        gamma13Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//29. [0,500*10^1.5]
        gamma14Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//30. [0,500*10^1.5]
        gamma15Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//31. [0,500*10^1.5]
        gamma16Values = new double[workerNum][workerCapacityNum+1][scenarioNum];//32. [0,500*10^1.5]~[0,1000*10^1.5]
        xPrimeValues = new double[customerNum][customerNum][workerNum][scenarioNum];//33. {0,1}
        deltaValues = new double[workerNum][workerCapacityNum+1][scenarioNum];//34. [0,200]
        deltaPrimeValues = new double[workerNum][workerCapacityNum+1][scenarioNum];// 35. [0,200]
        this.TotalSceNum=TotalSceNum;


        try {
            modelBuilder2nd = new ModelBuilder2nd(dataModel,TotalSceNum,stations);
            mipData2nd = modelBuilder2nd.getILP();

        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    public void solve() throws IloException {
        mipData2nd.cplex.exportModel("mip.lp");
        //mipDataS.cplex.writeParam(arg0)
//		mipDataS.cplex.exportModel("mip.sav");
//		mipDataS.cplex.exportModel("mip.mps");
//		mipDataS.cplex.writeMIPStart("start.mst");
//		mipDataS.cplex.writeParam("param.prm");
//		mipDataS.cplex.setParam(BooleanParam.PreInd, false); //Disable presolve.
//		mipDataS.cplex.setParam(IntParam.Threads, 2);

        if (mipData2nd.cplex.solve() && (mipData2nd.cplex.getStatus() == IloCplex.Status.Feasible || mipData2nd.cplex.getStatus() == IloCplex.Status.Optimal)) {
            this.objectiveValue = (int) Math.round(mipData2nd.cplex.getObjValue());// MathProgrammingUtil.doubleToInt(mipDataS.cplex.getObjValue());//(int)Math.round(mipDataS.cplex.getObjValue());

            this.bestObjValue = (int) Math.round(mipData2nd.cplex.getBestObjValue());
            //this.printSolution();
            this.optimal = mipData2nd.cplex.getStatus() == IloCplex.Status.Optimal;
            this.isFeasible = true;

            solution = new Solution();
            List<Solution2Stage> solution2Stages = new ArrayList<>();
            solution.setSolution2Stages(solution2Stages);
            solution.setObj(objectiveValue);
            Solution1stage solution1stage = new Solution1stage();
            solution.setSolution1stage(solution1stage);

            double obj1st=0;

            for (int s = 0; s < dataModel.getStationCandidates().size(); s++) {
                for(int t=0;t<dataModel.getType().length;t++){
                    varsLocationValues[s][t] = MathProgrammingUtil.doubleToInt(x[s][t]);
                    if(varsLocationValues[s][t]==1){
                        StationCandidate stationCandidate=dataModel.getStationCandidates().get(s);
                        obj1st+=(stationCandidate.getFixedCost()*1.0/TotalSceNum);
                        obj1st+=(stationCandidate.getCapacityCost()*dataModel.getType()[t]*1.0/TotalSceNum);
                    }
                }
            }
            solution1stage.setObj(obj1st);
            solution1stage.setStationLocation(varsLocationValues);
            double expectedValue=0;
            //second stage decision variable
            for (int xi = 0; xi < scenarioNum; xi++) {
                Solution2Stage solution2Stage = new Solution2Stage();
                solution2Stages.add(solution2Stage);
                Set<Customer> unservedCustomers = new HashSet<>();
                Map<Worker, Set<Customer>> customerAllocationDecision = new HashMap<>();
                Map<Worker, Double> travelCost = new HashMap<>();
                Map<Worker, StationCandidate> stationAllocationDecision = new HashMap<>();
                solution2Stage.setCustomerAllocationDecision(customerAllocationDecision);
                solution2Stage.setUnservedCustomers(unservedCustomers);
                solution2Stage.setScenarioNo(xi);
                solution2Stage.setTravelCost(travelCost);
                solution2Stage.setStationAllocationDecision(stationAllocationDecision);
                double obj=0;
                for (int k = 0; k < workerNum; k++) {
                    Worker worker = dataModel.getWorkers().get(k);
                    Set<Customer> customers = new HashSet<>();
                    customerAllocationDecision.put(worker, customers);
                    for (int i = 0; i < customerNum; i++) {
                        varsAssignIKValues[i][k][xi] = mipData2nd.cplex.getValue(mipData2nd.varsAssignIK[i][k][xi]);
                        if (MathProgrammingUtil.doubleToBoolean(varsAssignIKValues[i][k][xi])) {
                            customers.add(dataModel.getCustomers().get(i));
                        }
                    }
                }

                for (int k = 0; k < workerNum; k++) {
                    Worker worker = dataModel.getWorkers().get(k);
                    for (int s = 0; s < stationNum; s++) {
                        varsAssignSKValues[s][k][xi] = mipData2nd.cplex.getValue(mipData2nd.varsAssignSK[s][k][xi]);
                        if (MathProgrammingUtil.doubleToBoolean(varsAssignSKValues[s][k][xi])) {
                            stationAllocationDecision.put(worker, dataModel.getStationCandidates().get(s));
                        }
                    }
                }

                for (int i = 0; i < customerNum; i++) {
                    unServedValues[i][xi] = mipData2nd.cplex.getValue(mipData2nd.unServed[i][xi]);
                    if (MathProgrammingUtil.doubleToBoolean(unServedValues[i][xi])) {
                        Customer customer=dataModel.getCustomers().get(i);
                        unservedCustomers.add(customer);
                        obj+=customer.getUnservedPenalty();
                    }
                }


                for (int k = 0; k < workerNum; k++) {
                    for (int s = 0; s < stationNum; s++) {
                        zValues[s][k][xi] = mipData2nd.cplex.getValue(mipData2nd.z[s][k][xi]);
                    }
                }

                //for travel time prediction, 35 in total

//                for (int k = 0; k < workerNum; k++) {
//                    for (int s = 0; s < stationNum; s++) {
//                        for (int i = 0; i < customerNum; i++) {
//                            thetaValues[i][s][k][xi] = mipDataD.cplex.getValue(mipDataD.theta[i][s][k][xi]);
//                        }
//                    }
//                }

                for (int k = 0; k < workerNum; k++) {
                    Worker worker=dataModel.getWorkers().get(k);
                    double cost=0;
                            cost+=dataModel.getModelCoe()[1]*customerAllocationDecision.get(worker).size();
                    dmax1Values[k][xi] = mipData2nd.cplex.getValue(mipData2nd.dmax1[k][xi]);
                    cost+=dataModel.getModelCoe()[3]*dmax1Values[k][xi];
                    for (int j = 0; j < workerCapacityNum+1; j++) {
                        uValues[k][j][xi] = mipData2nd.cplex.getValue(mipData2nd.u[k][j][xi]);
                    }

                    for (int j = 0; j < workerCapacityNum+1; j++) {
                        f1Values[k][j][xi] = mipData2nd.cplex.getValue(mipData2nd.f1[k][j][xi]);
                        cost+=dataModel.getModelCoe()[4]*f1Values[k][j][xi];
                    }

                    for (int s = 0; s < stationNum; s++) {
                        for (int i = 0; i < customerNum; i++) {
                            varThetaValues[i][s][k][xi] = mipData2nd.cplex.getValue(mipData2nd.varTheta[i][s][k][xi]);
                        }
                    }


//                for (int k = 0; k < workerNum; k++) {
//                    for (int i = 0; i < customerNum; i++) {
//                        epsilonValues[i][k][xi] = mipDataD.cplex.getValue(mipDataD.epsilon[i][k][xi]);
//                    }
//                }


                    dmax2Values[k][xi] = mipData2nd.cplex.getValue(mipData2nd.dmax2[k][xi]);
                    cost+=dataModel.getModelCoe()[6]*dmax2Values[k][xi];


                    for (int j = 0; j < workerCapacityNum+1; j++) {
                        f2Values[k][j][xi] = mipData2nd.cplex.getValue(mipData2nd.f2[k][j][xi]);
                        cost+=dataModel.getModelCoe()[7]*f2Values[k][j][xi];

                    }

                    a1plusValues[k][xi] = mipData2nd.cplex.getValue(mipData2nd.a1plus[k][xi]);

                    a1minusValues[k][xi] = mipData2nd.cplex.getValue(mipData2nd.a1minus[k][xi]);

                    b1plusValues[k][xi] = mipData2nd.cplex.getValue(mipData2nd.b1plus[k][xi]);

                    b1minusValues[k][xi] = mipData2nd.cplex.getValue(mipData2nd.b1minus[k][xi]);

                    for (int j = 0; j < workerCapacityNum+1; j++) {
                        gamma9Values[k][j][xi] = mipData2nd.cplex.getValue(mipData2nd.gamma9[k][j][xi]);
                        cost+=dataModel.getModelCoe()[20]*gamma9Values[k][j][xi];

                    }

                    for (int j = 0; j < workerCapacityNum+1; j++) {

                        gamma11Values[k][j][xi] = mipData2nd.cplex.getValue(mipData2nd.gamma11[k][j][xi]);
                        cost+=dataModel.getModelCoe()[22]*gamma11Values[k][j][xi];

                    }
                    travelCost.put(worker,cost);
                    obj+=cost* GlobalVariable.daysNum;
                }
                solution2Stage.setObj(obj*1.0/TotalSceNum);
                expectedValue+=obj*(1.0/dataModel.getScenarios().size());

//                for (int k = 0; k < workerNum; k++) {
//                    a2plusValues[k][xi] = mipDataD.cplex.getValue(mipDataD.a2plus[k][xi]);
//                }

//                for (int k = 0; k < workerNum; k++) {
//                    a2minusValues[k][xi] = mipDataD.cplex.getValue(mipDataD.a2minus[k][xi]);
//                }

//                for (int k = 0; k < workerNum; k++) {
//                    b2plusValues[k][xi] = mipDataD.cplex.getValue(mipDataD.b2plus[k][xi]);
//                }

//                for (int k = 0; k < workerNum; k++) {
//                    b2minusValues[k][xi] = mipDataD.cplex.getValue(mipDataD.b2minus[k][xi]);
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma1Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma1[k][j][xi]);
//                    }
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma2Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma2[k][j][xi]);
//                    }
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma3Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma3[k][j][xi]);
//                    }
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma4Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma4[k][j][xi]);
//                    }
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma5Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma5[k][j][xi]);
//                    }
//                }
//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma6Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma6[k][j][xi]);
//                    }
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma7Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma7[k][j][xi]);
//                    }
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma8Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma8[k][j][xi]);
//                    }
//                }
//
//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma9Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma9[k][j][xi]);
//                    }
//                }
//
//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma10Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma10[k][j][xi]);
//                    }
//                }
//
//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma11Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma11[k][j][xi]);
//                    }
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma12Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma12[k][j][xi]);
//                    }
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma13Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma13[k][j][xi]);
//                    }
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma14Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma14[k][j][xi]);
//                    }
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma15Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma15[k][j][xi]);
//                    }
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        gamma16Values[k][j][xi] = mipDataD.cplex.getValue(mipDataD.gamma16[k][j][xi]);
//                    }
//                }

//                for (int k = 0; k < workerNum; k++) {
//                    for (int i = 0; i < customerNum; i++) {
//                        for (int ip = 0; ip < customerNum; ip++) {
//                            xPrimeValues[i][ip][k][xi]=mipDataD.cplex.getValue(mipDataD.xPrime[i][ip][k][xi]);
//                        }
//                    }
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        deltaValues[k][j][xi] = mipDataD.cplex.getValue(mipDataD.delta[k][j][xi]);
//                    }
//                }

//                for (int j = 0; j < workerCapacityNum; j++) {
//                    for (int k = 0; k < workerNum; k++) {
//                        deltaPrimeValues[k][j][xi] = mipDataD.cplex.getValue(mipDataD.deltaPrime[k][j][xi]);
//                    }
//                }
            }
            solution.setExpectedValue(expectedValue);

            System.out.println(solution);


            //Verify solution
//			SolutionValidator.validate(btsp, solution);
//			if(solution.getObjective()!=this.objectiveValue)
//				throw new RuntimeException("Objective constructed solution deviates from MIP objective. MIP obj: "+this.objectiveValue+" constr sol obj: "+solution.getObjective());
//			System.out.println("Solution correct");
//			System.out.println("Solution:\n{}"+solution);
        } else if (mipData2nd.cplex.getStatus() == IloCplex.Status.Infeasible) {
//			throw new RuntimeException("Mip infeasible");
            this.isFeasible = false;
            this.optimal = true;
        } else if (mipData2nd.cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim) {
            System.out.println("No solution could be found in the given amount of time");
            this.isFeasible = true; //Technically there is no proof whether or not a feasible solution exists
            this.optimal = false;
        } else {
            //NOTE: when cplex does not find a solution before the default time out, it throws a Status Unknown exception
            //NOTE2: Might be required to extend the default runtime.
            throw new RuntimeException("Cplex solve terminated with status: " + mipData2nd.cplex.getStatus());
        }
        mipData2nd.cplex.end();
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
        return mipData2nd.cplex.getNnodes();
    }
}
