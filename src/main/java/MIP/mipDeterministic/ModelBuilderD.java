package MIP.mipDeterministic;


import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import model.Customer;
import model.Instance;
import model.Scenario;
import model.StationCandidate;
import model.Worker;
import util.Constants;
import util.GlobalVariable;

/**
 * @author Wang Li
 * @description
 * @date 6/21/22 3:03 PM
 */
public class ModelBuilderD {


    private Instance dataModel;
    private MipDataD mipDataD;
    private int TotalSceNum;

    public ModelBuilderD(Instance dataModel,int TotalSceNum) throws IloException {
        this.dataModel = dataModel;
        this.TotalSceNum=TotalSceNum;
        this.buildModel();
    }
    public ModelBuilderD(Instance dataModel) throws IloException {
        new ModelBuilderD(dataModel,1);
    }

    /**
     * Solve the root node of the Branch and Bound tree.
     */
    public MipDataD getLP() throws IloException {
        mipDataD.cplex.setParam(IloCplex.IntParam.NodeLim, 0);
        return mipDataD;
    }

    /**
     * Solve the entire Branch and Bound tree
     */
    public MipDataD getILP() throws IloException {
        mipDataD.cplex.setParam(IloCplex.IntParam.NodeLim, 210000000); //Continue search
        //mipDataS.cplex.setParam(IloCplex.BooleanParam.PreInd, false);
        mipDataD.cplex.setParam(IloCplex.DoubleParam.TiLim, 7200); //set time limit in seconds
        mipDataD.cplex.setParam(IloCplex.IntParam.Threads, Constants.MAXTHREADS);
        mipDataD.cplex.setParam(IloCplex.IntParam.NodeFileInd, 3);
        mipDataD.cplex.setParam(IloCplex.IntParam.WorkMem, 4096);
        mipDataD.cplex.setParam(IloCplex.Param.Simplex.Tolerances.Optimality,0.1);
//        mipDataD.cplex.setOut(null); //Disable Cplex output
        return mipDataD;
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
//        IloNumVar[] varsCapacity = new IloNumVar[stationNum];
//        for (int i = 0; i < stationNum; i++) {
//            IloNumVar var = cplex.numVar(0, 1, "w_" + i);
//            varsCapacity[i] = var;
//        }
        //second stage decision variable
        IloIntVar[][][] varsAssignIK = new IloIntVar[customerNum][workerNum][scenarioNum];
        for (int i = 0; i < customerNum; i++) {
            for (int k = 0; k < workerNum; k++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloIntVar var = cplex.boolVar("x_i" + i + "_k" + k + "_xi" + xi);
                    varsAssignIK[i][k][xi] = var;
                }
            }
        }
        IloIntVar[][] tao = new IloIntVar[workerNum][scenarioNum];

        for (int k = 0; k < workerNum; k++) {
            for (int xi = 0; xi < scenarioNum; xi++) {
                IloIntVar var = cplex.boolVar("tao_k" + k + "_xi" + xi);
                tao[k][xi] = var;
            }
        }

        IloIntVar[][][] varsAssignSK = new IloIntVar[stationNum][workerNum][scenarioNum];
        for (int s = 0; s < stationNum; s++) {
            for (int k = 0; k < workerNum; k++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloIntVar var = cplex.boolVar("v_s" + s + "_k" + k + "_xi" + xi);
                    varsAssignSK[s][k][xi] = var;
                }
            }
        }
        IloIntVar[][] unServed = new IloIntVar[customerNum][scenarioNum];
        for (int i = 0; i < customerNum; i++) {
            for (int xi = 0; xi < scenarioNum; xi++) {
                IloIntVar var = cplex.boolVar("unserved_i" + i + "_xi" + xi);
                unServed[i][xi] = var;
            }
        }
        IloNumVar[][][] z = new IloNumVar[stationNum][workerNum][scenarioNum];
        for (int s = 0; s < stationNum; s++) {
            for (int k = 0; k < workerNum; k++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, dataModel.getWorkers().get(k).getCapacity(), "z_s" + s + "_k" + k + "_xi" + xi);
                    z[s][k][xi] = var;
                }
            }
        }

        //for travel time prediction, 35 in total
        IloNumVar[][][][] theta = new IloNumVar[customerNum][stationNum][workerNum][scenarioNum]; //1. domain [0,1]
        for (int i = 0; i < customerNum; i++) {
            for (int s = 0; s < stationNum; s++) {
                for (int k = 0; k < workerNum; k++) {
                    for (int xi = 0; xi < scenarioNum; xi++) {
                        IloNumVar var = cplex.numVar(0, 1, "theta_i" + i + "_s" + s + "_k" + k + "_xi" + xi);
                        theta[i][s][k][xi] = var;
                    }
                }
            }
        }
        IloNumVar[][] dmax1 = new IloNumVar[workerNum][scenarioNum]; //2. [0,1000sqart2]
        for (int k = 0; k < workerNum; k++) {
            for (int xi = 0; xi < scenarioNum; xi++) {
                IloNumVar var = cplex.numVar(0, dataModel.getCoordinateMax() * 1.414, "dmax1_k" + k + "_xi" + xi);
                dmax1[k][xi] = var;
            }
        }
        IloIntVar[][][] u = new IloIntVar[workerNum][workerCapacityNum + 1][scenarioNum];//3. {0,1}
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloIntVar var = cplex.boolVar("u_k" + k + "_j" + j + "_xi" + xi);
                    u[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] f1 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//4. [0,1000]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, dataModel.getCoordinateMax(), "f1_k" + k + "_j" + j + "_xi" + xi);
                    f1[k][j][xi] = var;
                }
            }
        }
        IloIntVar[][][][] varTheta = new IloIntVar[customerNum][stationNum][workerNum][scenarioNum];//5. {0,1}
        for (int i = 0; i < customerNum; i++) {
            for (int s = 0; s < stationNum; s++) {
                for (int k = 0; k < workerNum; k++) {
                    for (int xi = 0; xi < scenarioNum; xi++) {
                        IloIntVar var = cplex.boolVar("varTheta_i" + i + "_s" + s + "_k" + k + "_xi" + xi);
                        varTheta[i][s][k][xi] = var;
                    }
                }
            }
        }
        IloNumVar[][][] epsilon = new IloNumVar[customerNum][workerNum][scenarioNum];//6. [0,1000]
        for (int i = 0; i < customerNum; i++) {
            for (int k = 0; k < workerNum; k++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, 1, "epsilon_i" + i + "_k" + k + "_xi" + xi);
                    epsilon[i][k][xi] = var;
                }
            }
        }
        IloNumVar[][] dmax2 = new IloNumVar[workerNum][scenarioNum];//7. [0,1000sqart2]
        for (int k = 0; k < workerNum; k++) {
            for (int xi = 0; xi < scenarioNum; xi++) {
                IloNumVar var = cplex.numVar(0, dataModel.getCoordinateMax() * 1.414, "dmax2_k" + k + "_xi" + xi);
                dmax2[k][xi] = var;
            }
        }
        IloNumVar[][][] f2 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//8. [0,1000]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, dataModel.getCoordinateMax(), "f2_k" + k + "_j" + j + "_xi" + xi);
                    f2[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][] a1plus = new IloNumVar[workerNum][scenarioNum];//9. [0,1000]
        for (int k = 0; k < workerNum; k++) {

            for (int xi = 0; xi < scenarioNum; xi++) {
                IloNumVar var = cplex.numVar(0, dataModel.getCoordinateMax(), "a1plus_k" + k + "_xi" + xi);
                a1plus[k][xi] = var;
            }

        }
        IloNumVar[][] a1minus = new IloNumVar[workerNum][scenarioNum];//10. [-1000,0]
        for (int k = 0; k < workerNum; k++) {

            for (int xi = 0; xi < scenarioNum; xi++) {
                IloNumVar var = cplex.numVar(-dataModel.getCoordinateMax(), 0, "a1minus_k" + k + "_xi" + xi);
                a1minus[k][xi] = var;
            }

        }
        IloNumVar[][] b1plus = new IloNumVar[workerNum][scenarioNum];//11. [0,1000]
        for (int k = 0; k < workerNum; k++) {

            for (int xi = 0; xi < scenarioNum; xi++) {
                IloNumVar var = cplex.numVar(0, dataModel.getCoordinateMax(), "b1plus_k" + k + "_xi" + xi);
                b1plus[k][xi] = var;
            }

        }
        IloNumVar[][] b1minus = new IloNumVar[workerNum][scenarioNum];//12. [-1000,0]
        for (int k = 0; k < workerNum; k++) {

            for (int xi = 0; xi < scenarioNum; xi++) {
                IloNumVar var = cplex.numVar(-1*dataModel.getCoordinateMax(), 0, "b1minus_k" + k + "_xi" + xi);
                b1minus[k][xi] = var;
            }

        }
        IloNumVar[][] a2plus = new IloNumVar[workerNum][scenarioNum];//13. [0,1000]
        for (int k = 0; k < workerNum; k++) {
            Worker worker = dataModel.getWorkers().get(k);
            int lat = worker.getLatD();
            for (int xi = 0; xi < scenarioNum; xi++) {
                IloNumVar var = cplex.numVar(lat, dataModel.getCoordinateMax(), "a2plus_k" + k + "_xi" + xi);
                a2plus[k][xi] = var;
            }

        }
        IloNumVar[][] a2minus = new IloNumVar[workerNum][scenarioNum];//14. [-1000,0]
        for (int k = 0; k < workerNum; k++) {
            Worker worker = dataModel.getWorkers().get(k);
            int lat = worker.getLatD();
            for (int xi = 0; xi < scenarioNum; xi++) {
                IloNumVar var = cplex.numVar(-lat, 0, "a2minus_k" + k + "_xi" + xi);
                a2minus[k][xi] = var;
            }

        }
        IloNumVar[][] b2plus = new IloNumVar[workerNum][scenarioNum];//15. [0,1000]
        for (int k = 0; k < workerNum; k++) {

            for (int xi = 0; xi < scenarioNum; xi++) {
                IloNumVar var = cplex.numVar(0, dataModel.getCoordinateMax(), "b2plus_k" + k + "_xi" + xi);
                b2plus[k][xi] = var;
            }

        }
        IloNumVar[][] b2minus = new IloNumVar[workerNum][scenarioNum];//16. [-1000,0]
        for (int k = 0; k < workerNum; k++) {

            for (int xi = 0; xi < scenarioNum; xi++) {
                IloNumVar var = cplex.numVar(-dataModel.getCoordinateMax(), 0, "b2minus_k" + k + "_xi" + xi);
                b2minus[k][xi] = var;
            }

        }
        IloNumVar[][][] gamma1 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//17. [0,500*10^-0.5]
        int temp = (int) ( dataModel.getCoordinateMax());
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma1_k" + k + "_j" + j + "_xi" + xi);
                    gamma1[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] gamma2 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//18. [0,500*10^-0.5]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma2_k" + k + "_j" + j + "_xi" + xi);
                    gamma2[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] gamma3 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//19. [0,500*10^-0.5]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma3_k" + k + "_j" + j + "_xi" + xi);
                    gamma3[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] gamma4 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//20. [0,500*10^-0.5]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma4_k" + k + "_j" + j + "_xi" + xi);
                    gamma4[k][j][xi] = var;
                }
            }
        }
        temp = (int) ( dataModel.getCoordinateMax() * Math.pow(workerCapacityNum, 0.5));
        IloNumVar[][][] gamma5 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//21. [0,500*10^0.5]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma5_k" + k + "_j" + j + "_xi" + xi);
                    gamma5[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] gamma6 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//22. [0,500*10^0.5]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma6_k" + k + "_j" + j + "_xi" + xi);
                    gamma6[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] gamma7 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//23. [0,500*10^0.5]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma7_k" + k + "_j" + j + "_xi" + xi);
                    gamma7[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] gamma8 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//24. [0,500*10^0.5]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma8_k" + k + "_j" + j + "_xi" + xi);
                    gamma8[k][j][xi] = var;
                }
            }
        }
        temp = (int) (dataModel.getCoordinateMax() * workerCapacityNum);
        IloNumVar[][][] gamma9 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//25. [0,500*10]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma9_k" + k + "_j" + j + "_xi" + xi);
                    gamma9[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] gamma10 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//26. [0,500*10]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma10_k" + k + "_j" + j + "_xi" + xi);
                    gamma10[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] gamma11 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//27. [0,500*10]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma11_k" + k + "_j" + j + "_xi" + xi);
                    gamma11[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] gamma12 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//28. [0,500*10]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma12_k" + k + "_j" + j + "_xi" + xi);
                    gamma12[k][j][xi] = var;
                }
            }
        }
        temp = (int) ( dataModel.getCoordinateMax() * Math.pow(workerCapacityNum, 1.5));
        IloNumVar[][][] gamma13 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//29. [0,500*10^1.5]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma13_k" + k + "_j" + j + "_xi" + xi);
                    gamma13[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] gamma14 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//30. [0,500*10^1.5]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma14_k" + k + "_j" + j + "_xi" + xi);
                    gamma14[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] gamma15 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//31. [0,500*10^1.5]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma15_k" + k + "_j" + j + "_xi" + xi);
                    gamma15[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] gamma16 = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//32. [0,500*10^1.5]~[0,1000*10^1.5]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, temp, "gamma16_k" + k + "_j" + j + "_xi" + xi);
                    gamma16[k][j][xi] = var;
                }
            }
        }
        IloIntVar[][][][] xPrime = new IloIntVar[customerNum][customerNum][workerNum][scenarioNum];//33. {0,1}
        for (int i = 0; i < customerNum; i++) {
            for (int ip = 0; ip < customerNum; ip++) {
                for (int k = 0; k < workerNum; k++) {
                    for (int xi = 0; xi < scenarioNum; xi++) {
                        IloIntVar var = cplex.boolVar("xPrime_i" + i + "_i'" + ip + "_k" + k + "_xi" + xi);
                        xPrime[i][ip][k][xi] = var;
                    }
                }
            }
        }
        IloNumVar[][][] delta = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];//34. [0,200]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, dataModel.getCoordinateSDinOneRoute(), "delta_k" + k + "_j" + j + "_xi" + xi);
                    delta[k][j][xi] = var;
                }
            }
        }
        IloNumVar[][][] deltaPrime = new IloNumVar[workerNum][workerCapacityNum + 1][scenarioNum];// 35. [0,200]
        for (int k = 0; k < workerNum; k++) {
            for (int j = 0; j <= workerCapacityNum; j++) {
                for (int xi = 0; xi < scenarioNum; xi++) {
                    IloNumVar var = cplex.numVar(0, dataModel.getCoordinateSDinOneRoute(), "deltaPrime_k" + k + "_j" + j + "_xi" + xi);
                    deltaPrime[k][j][xi] = var;
                }
            }
        }

        //Create objective: Minimize weighted travel travelTime
        IloLinearNumExpr obj = cplex.linearNumExpr();
        for (int s = 0; s < stationNum; s++) {
            StationCandidate stationCandidate=dataModel.getStationCandidates().get(s);
            for(int t=0;t<typeNum;t++){
                double coe=stationCandidate.getFixedCost()+stationCandidate.getCapacityCost()*dataModel.getType()[t];
                obj.addTerm((coe*1.0)/TotalSceNum+dataModel.getLambda()[s][t], varsLocation[s][t]);
            }
        }
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                double coe0 = GlobalVariable.daysNum * (dataModel.getScenarios().get(xi).getIsWorkerAvailable()[k] * 1.0) / (dataModel.getScenarios().size()*TotalSceNum * 1.0);

                //1. number of location n_k^\xi
                double coe = coe0 * dataModel.getModelCoe()[1];
                for (int i = 0; i < customerNum; i++) {
                    obj.addTerm(coe, varsAssignIK[i][k][xi]);
                }
                //2. d_{1k}^\xi
//                coe = coe0 * dataModel.getModelCoe()[2];
//                for (int i = 0; i < customerNum; i++) {
//                    for (int s = 0; s < stationNum; s++) {
//                        int sIndex = dataModel.getStationCandidates().get(s).getNodeIndex();
//                        int iIndex = dataModel.getCustomers().get(i).getNodeIndex();
//                        double travelSI = dataModel.getTravelCostMatrix()[sIndex][iIndex];
//                        obj.addTerm(coe * travelSI, theta[i][s][k][xi]);
//                    }
//                }
                //3. D_{1k}^\xi
                coe = coe0 * dataModel.getModelCoe()[3];
                obj.addTerm(coe, dmax1[k][xi]);
                //4. \bar{d}_{1k}^\xi
                coe = coe0 * dataModel.getModelCoe()[4];
                for (int j = 0; j <= workerCapacityNum; j++) {
                    obj.addTerm(coe, f1[k][j][xi]);
                }
                //5. d_{2k}^\xi
//                coe = coe0 * dataModel.getModelCoe()[5];
//                for (int i = 0; i < customerNum; i++) {
//                    int iIndex = dataModel.getCustomers().get(i).getNodeIndex();
//                    int dIndex = dataModel.getWorkers().get(k).getIndexD();
//                    double travelID = dataModel.getTravelCostMatrix()[iIndex][dIndex];
//                    obj.addTerm(coe * travelID, epsilon[i][k][xi]);
//                }
                //6. D_{2k}^\xi
                coe = coe0 * dataModel.getModelCoe()[6];
                obj.addTerm(coe, dmax2[k][xi]);
                //7. \bar{d}_{2k}^\xi
                coe = coe0 * dataModel.getModelCoe()[7];
                for (int j = 0; j <= workerCapacityNum; j++) {
                   obj.addTerm(coe, f2[k][j][xi]);
                }
                //8. a_1
//                coe = coe0 * dataModel.getModelCoe()[8];
//                obj.addTerm(coe, a1plus[k][xi]);
//                obj.addTerm(coe, a1minus[k][xi]);
//                //9. b_1
//                coe = coe0 * dataModel.getModelCoe()[9];
//                obj.addTerm(coe, b1plus[k][xi]);
//                obj.addTerm(coe, b1minus[k][xi]);
//                //10. a_2
//                coe = coe0 * dataModel.getModelCoe()[10];
//                obj.addTerm(coe, a2plus[k][xi]);
//                obj.addTerm(coe, a2minus[k][xi]);
//                //11. b_2
//                coe = coe0 * dataModel.getModelCoe()[11];
//                obj.addTerm(coe, b2plus[k][xi]);
//                obj.addTerm(coe, b2minus[k][xi]);
//                //12. gamma_1
//                coe = coe0 * dataModel.getModelCoe()[12];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma1[k][j][xi]);
//                }
//                //13. gamma_2
//                coe = coe0 * dataModel.getModelCoe()[13];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma2[k][j][xi]);
//                }
//                //14. gamma_3
//                coe = coe0 * dataModel.getModelCoe()[14];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma3[k][j][xi]);
//                }
//                //15. gamma_4
//                coe = coe0 * dataModel.getModelCoe()[15];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma4[k][j][xi]);
//                }
//                //16. gamma_5
//                coe = coe0 * dataModel.getModelCoe()[16];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma5[k][j][xi]);
//                }
//                //17. gamma_6
//                coe = coe0 * dataModel.getModelCoe()[17];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma6[k][j][xi]);
//                }
//                //18. gamma_7
//                coe = coe0 * dataModel.getModelCoe()[18];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma7[k][j][xi]);
//                }
//                //19. gamma_8
//                coe = coe0 * dataModel.getModelCoe()[19];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma8[k][j][xi]);
//                }
                //20. gamma_9
                coe = coe0 * dataModel.getModelCoe()[20];
                for (int j = 0; j <= workerCapacityNum; j++) {
                    obj.addTerm(coe, gamma9[k][j][xi]);
                }
                //21. gamma_10
//                coe = coe0 * dataModel.getModelCoe()[21];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma10[k][j][xi]);
//                }
                //22. gamma_11
                coe = coe0 * dataModel.getModelCoe()[22];
                for (int j = 0; j <= workerCapacityNum; j++) {
                    obj.addTerm(coe, gamma11[k][j][xi]);
                }
                //23. gamma_12
//                coe = coe0 * dataModel.getModelCoe()[23];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma12[k][j][xi]);
//                }
//                //24. gamma_13
//                coe = coe0 * dataModel.getModelCoe()[24];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma13[k][j][xi]);
//                }
//                //25. gamma_14
//                coe = coe0 * dataModel.getModelCoe()[25];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma14[k][j][xi]);
//                }
//                //26. gamma_15
//                coe = coe0 * dataModel.getModelCoe()[26];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma15[k][j][xi]);
//                }
//                //27. gamma_16
//                coe = coe0 * dataModel.getModelCoe()[27];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, gamma16[k][j][xi]);
//                }
//                //28.s_a
//                coe = coe0 * dataModel.getModelCoe()[28];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, delta[k][j][xi]);
//                }
//                //29.s_b
//                coe = coe0 * dataModel.getModelCoe()[29];
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    obj.addTerm(coe, deltaPrime[k][j][xi]);
//                }

            }
            // Penalty for unserved customers
            double coe0 = GlobalVariable.daysNum * 1.0 / (dataModel.getScenarios().size()*TotalSceNum * 1.0);
            for (int i = 0; i < customerNum; i++) {
                double coe = coe0 * dataModel.getCustomers().get(i).getUnservedPenalty();
                obj.addTerm(coe, unServed[i][xi]);
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
        //1. worker capacity
        for (int xi = 0; xi < scenarioNum; xi++) {
            Scenario scenario = dataModel.getScenarios().get(xi);
            for (int k = 0; k < workerNum; k++) {
                Worker worker = dataModel.getWorkers().get(k);

                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int i = 0; i < customerNum; i++) {
                    expr.addTerm(scenario.getCustomerDemand()[i], varsAssignIK[i][k][xi]);
                }
                cplex.addGe(worker.getCapacity() * scenario.getIsWorkerAvailable()[k], expr, "worker Capacity_k" + k + "_xi" + xi);
            }
        }


        //2. worker capacity number
        for (int xi = 0; xi < scenarioNum; xi++) {
            Scenario scenario = dataModel.getScenarios().get(xi);
            for (int k = 0; k < workerNum; k++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int i = 0; i < customerNum; i++) {
                    expr.addTerm(1, varsAssignIK[i][k][xi]);
                }
                expr.addTerm(-workerCapacityNum * scenario.getIsWorkerAvailable()[k], tao[k][xi]);
                cplex.addLe(expr, 0, "worker CapacityNum_k" + k + "_xi" + xi);
            }
        }
        for (int xi = 0; xi < scenarioNum; xi++) {
            Scenario scenario = dataModel.getScenarios().get(xi);
            for (int k = 0; k < workerNum; k++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int i = 0; i < customerNum; i++) {
                    expr.addTerm(1, varsAssignIK[i][k][xi]);
                }
                expr.addTerm(-1, tao[k][xi]);
                cplex.addGe(expr, 0, "worker CapacityNum2_k" + k + "_xi" + xi);
            }
        }

        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int s = 0; s < stationNum; s++) {
                    expr.addTerm(1, varsAssignSK[s][k][xi]);
                }
                expr.addTerm(-1, tao[k][xi]);
                cplex.addGe(expr, 0, "worker CapacityNum3_k" + k + "_xi" + xi);
            }
        }

//        //3. each worker can visit one station at most
        for (int xi = 0; xi < scenarioNum; xi++) {
            Scenario scenario = dataModel.getScenarios().get(xi);
            for (int k = 0; k < workerNum; k++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int s = 0; s < stationNum; s++) {
                    expr.addTerm(1, varsAssignSK[s][k][xi]);
                }
                cplex.addGe(scenario.getIsWorkerAvailable()[k], expr, "worker visit one station_k" + k + "_xi" + xi);
            }
        }
//        //4. each customer is either visited by a worker or unServed
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int i = 0; i < customerNum; i++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int k = 0; k < workerNum; k++) {
                    expr.addTerm(1, varsAssignIK[i][k][xi]);
                }
                expr.addTerm(1, unServed[i][xi]);
                cplex.addEq(1, expr, "customer visited or unserved_i" + i + "_xi" + xi);
            }
        }
        //5. station capacity
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int s = 0; s < stationNum; s++) {
                StationCandidate stationCandidate = dataModel.getStationCandidates().get(s);
                IloLinearNumExpr expr = cplex.linearNumExpr();
                for (int k = 0; k < workerNum; k++) {
                    expr.addTerm(1, z[s][k][xi]);
                }
                for(int t=0;t<typeNum;t++){
                    expr.addTerm(-1*dataModel.getType()[t], varsLocation[s][t]);

                }
                cplex.addLe(expr, 0, "station capacity_s" + s + "_xi" + xi);
            }
        }
        //6. define z
        for (int xi = 0; xi < scenarioNum; xi++) {
            Scenario scenario = dataModel.getScenarios().get(xi);
            for (int s = 0; s < stationNum; s++) {
                for (int k = 0; k < workerNum; k++) {
                    Worker worker = dataModel.getWorkers().get(k);
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    for (int i = 0; i < customerNum; i++) {
                        expr.addTerm(scenario.getCustomerDemand()[i], varsAssignIK[i][k][xi]);
                    }
                    int M = worker.getCapacity();
                    expr.addTerm(M, varsAssignSK[s][k][xi]);
                    expr.addTerm(-1, z[s][k][xi]);
                    cplex.addLe(expr, M, "defineZ1_s" + s + "_k" + k + "_xi" + xi);
                }
            }
        }
        for (int xi = 0; xi < scenarioNum; xi++) {
            Scenario scenario = dataModel.getScenarios().get(xi);
            for (int s = 0; s < stationNum; s++) {
                for (int k = 0; k < workerNum; k++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    for (int i = 0; i < customerNum; i++) {
                        expr.addTerm(scenario.getCustomerDemand()[i], varsAssignIK[i][k][xi]);
                    }
                    expr.addTerm(-1, z[s][k][xi]);
                    cplex.addGe(expr, 0, "defineZ2_s" + s + "_k" + k + "_xi" + xi);
                }
            }
        }
        //define d_1
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                IloLinearNumExpr expr = cplex.linearNumExpr();
//                for (int i = 0; i < customerNum; i++) {
//                    for (int s = 0; s < stationNum; s++) {
//                        expr.addTerm(1, theta[i][s][k][xi]);
//                    }
//                }
//                expr.addTerm(-1, tao[k][xi]);
//                cplex.addEq(0, expr, "defined1_1_k" + k + "_xi" + xi);
//            }
//        }
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int i = 0; i < customerNum; i++) {
//                    for (int s = 0; s < stationNum; s++) {
//                        IloLinearNumExpr expr = cplex.linearNumExpr();
//                        expr.addTerm(1, theta[i][s][k][xi]);
//                        expr.addTerm(-1, varsAssignIK[i][k][xi]);
//                        cplex.addLe(expr, 0, "defined1_2_k" + k + "_xi" + xi + "_i" + i + "_s" + s);
//                    }
//                }
//            }
//        }
//
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int i = 0; i < customerNum; i++) {
//                    for (int s = 0; s < stationNum; s++) {
//                        IloLinearNumExpr expr = cplex.linearNumExpr();
//                        expr.addTerm(1, theta[i][s][k][xi]);
//                        expr.addTerm(-1, varsAssignSK[s][k][xi]);
//                        cplex.addLe(expr, 0, "defined1_3_k" + k + "_xi" + xi + "_i" + i + "_s" + s);
//                    }
//                }
//            }
//        }

        //D_1
        double M = 1.414 * dataModel.getCoordinateMax();
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int i = 0; i < customerNum; i++) {
                    for (int s = 0; s < stationNum; s++) {
                        IloLinearNumExpr expr = cplex.linearNumExpr();
                        expr.addTerm(1, dmax1[k][xi]);
                        expr.addTerm(-M, varsAssignIK[i][k][xi]);
                        expr.addTerm(-M, varsAssignSK[s][k][xi]);
                        int sIndex = dataModel.getStationCandidates().get(s).getNodeIndex();
                        int iIndex = dataModel.getCustomers().get(i).getNodeIndex();
                        double travelSI = dataModel.getTravelCostMatrix()[sIndex][iIndex];
                        cplex.addGe(expr, travelSI - 2 * M, "D1_k" + k + "_xi" + xi + "_i" + i + "_s" + s);
                    }
                }
            }
        }
        //define u
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int j = 0; j <= workerCapacityNum; j++) {
                    expr.addTerm(j, u[k][j][xi]);
                }
                IloLinearIntExpr expr1 = cplex.linearIntExpr();
                for (int i = 0; i < customerNum; i++) {
                    expr1.addTerm(1, varsAssignIK[i][k][xi]);
                }
                cplex.addEq(expr, expr1, "u1_k" + k + "_xi" + xi);
            }
        }
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                IloLinearIntExpr expr = cplex.linearIntExpr();
                for (int j = 0; j <= workerCapacityNum; j++) {
                    expr.addTerm(1, u[k][j][xi]);
                }
                cplex.addEq(expr, 1, "u2_k" + k + "_xi" + xi);
            }
        }

        //\bar{d}_1
        M = dataModel.getCoordinateMax() * workerCapacityNum;
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int j = 0; j <= workerCapacityNum; j++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(M, u[k][j][xi]);
                    expr.addTerm(-1, f1[k][j][xi]);
                    cplex.addGe(expr, 0, "bar{d}1_1_k" + k + "_j" + j + "_xi" + xi);
                }
            }
        }
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int j = 1; j <= workerCapacityNum; j++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    for (int i = 0; i < customerNum; i++) {
                        for (int s = 0; s < stationNum; s++) {
                            int sIndex = dataModel.getStationCandidates().get(s).getNodeIndex();
                            int iIndex = dataModel.getCustomers().get(i).getNodeIndex();
                            double travelSI = dataModel.getTravelCostMatrix()[sIndex][iIndex];
                            expr.addTerm((travelSI * 1.0) / (j * 1.0), varTheta[i][s][k][xi]);

                        }
                    }
                    expr.addTerm(-1, f1[k][j][xi]);
                    cplex.addGe(expr, 0, "bar{d}1_2_k" + k + "_j" + j + "_xi" + xi);
                }
            }
        }
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int j = 1; j <= workerCapacityNum; j++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    for (int i = 0; i < customerNum; i++) {
                        for (int s = 0; s < stationNum; s++) {
                            int sIndex = dataModel.getStationCandidates().get(s).getNodeIndex();
                            int iIndex = dataModel.getCustomers().get(i).getNodeIndex();
                            double travelSI = dataModel.getTravelCostMatrix()[sIndex][iIndex];
                            expr.addTerm((travelSI * 1.0) / (j * 1.0), varTheta[i][s][k][xi]);

                        }
                    }
                    expr.addTerm(M, u[k][j][xi]);
                    expr.addTerm(-1, f1[k][j][xi]);
                    cplex.addLe(expr, M, "bar{d}1_3_k" + k + "_j" + j + "_xi" + xi);
                }
            }
        }
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int i = 0; i < customerNum; i++) {
                    for (int s = 0; s < stationNum; s++) {
                        IloLinearIntExpr expr = cplex.linearIntExpr();
                        expr.addTerm(1, varsAssignIK[i][k][xi]);
                        expr.addTerm(1, varsAssignSK[s][k][xi]);
                        expr.addTerm(-2, varTheta[i][s][k][xi]);
                        cplex.addGe(expr, 0, "varTheta1_i" + i + "_s" + s + "_k" + k + "_xi" + xi);
                    }
                }
            }
        }
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int i = 0; i < customerNum; i++) {
                    for (int s = 0; s < stationNum; s++) {
                        IloLinearIntExpr expr = cplex.linearIntExpr();
                        expr.addTerm(1, varsAssignIK[i][k][xi]);
                        expr.addTerm(1, varsAssignSK[s][k][xi]);
                        expr.addTerm(-1, varTheta[i][s][k][xi]);
                        cplex.addLe(expr, 1, "varTheta2_i" + i + "_s" + s + "_k" + k + "_xi" + xi);
                    }
                }
            }
        }

        //d_2
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                IloLinearNumExpr expr = cplex.linearNumExpr();
//                for (int i = 0; i < customerNum; i++) {
//                    expr.addTerm(1, epsilon[i][k][xi]);
//                }
//                expr.addTerm(-1, tao[k][xi]);
//                cplex.addEq(0, expr, "d2_1_k" + k + "_xi" + xi);
//            }
//        }
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int i = 0; i < customerNum; i++) {
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(1, epsilon[i][k][xi]);
//                    expr.addTerm(-1, varsAssignIK[i][k][xi]);
//                    cplex.addLe(expr, 0, "d2_1_i" + i + "_k" + k + "_xi" + xi);
//
//                }
//            }
//        }

        //D_2
        M = dataModel.getCoordinateMax() * 1.414;
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int i = 0; i < customerNum; i++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(1, dmax2[k][xi]);
                    expr.addTerm(-M, varsAssignIK[i][k][xi]);
                    int iIndex = dataModel.getCustomers().get(i).getNodeIndex();
                    int dIndex = dataModel.getWorkers().get(k).getIndexD();
                    int travelID = dataModel.getTravelCostMatrix()[iIndex][dIndex];
                    cplex.addGe(expr, travelID - M, "D2_i" + i + "_k" + k + "_xi" + xi);
                }
            }
        }
        //\bar{d}_2
        M = dataModel.getCoordinateMax() * workerCapacityNum;
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int j = 0; j <= workerCapacityNum; j++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(M, u[k][j][xi]);
                    expr.addTerm(-1, f2[k][j][xi]);
                    cplex.addGe(expr, 0, "bar{d}2_1_k" + k + "_j" + j + "_xi" + xi);
                }
            }
        }
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int j = 1; j <= workerCapacityNum; j++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    for (int i = 0; i < customerNum; i++) {
                        int iIndex = dataModel.getCustomers().get(i).getNodeIndex();
                        int dIndex = dataModel.getWorkers().get(k).getIndexD();
                        int travelID = dataModel.getTravelCostMatrix()[iIndex][dIndex];
                        expr.addTerm((travelID * 1.0) / (j * 1.0), varsAssignIK[i][k][xi]);


                    }
                    expr.addTerm(-1, f2[k][j][xi]);
                    cplex.addGe(expr, 0, "bar{d}2_2_k" + k + "_j" + j + "_xi" + xi);
                }
            }
        }
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int j = 1; j <= workerCapacityNum; j++) {
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    for (int i = 0; i < customerNum; i++) {
                        int iIndex = dataModel.getCustomers().get(i).getNodeIndex();
                        int dIndex = dataModel.getWorkers().get(k).getIndexD();
                        int travelID = dataModel.getTravelCostMatrix()[iIndex][dIndex];
                        expr.addTerm((travelID * 1.0) / (j * 1.0), varsAssignIK[i][k][xi]);


                    }
                    expr.addTerm(M, u[k][j][xi]);
                    expr.addTerm(-1, f2[k][j][xi]);
                    cplex.addLe(expr, M, "bar{d}2_3_k" + k + "_j" + j + "_xi" + xi);
                }
            }
        }

        //a1
       M = dataModel.getCoordinateMax();
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int i = 0; i < customerNum; i++) {
                    Customer customer = dataModel.getCustomers().get(i);
                    int lat = customer.getLat();
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(lat, varsAssignIK[i][k][xi]);
                    expr.addTerm(-1, a1plus[k][xi]);
                    cplex.addLe(expr, 0, "a1_1_i" + i + "_k" + k + "_xi" + xi);
                }
            }
        }

        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int i = 0; i < customerNum; i++) {
                    Customer customer = dataModel.getCustomers().get(i);
                    int lat = customer.getLat();
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(M, varsAssignIK[i][k][xi]);
                    expr.addTerm(-1, a1minus[k][xi]);
                    cplex.addLe(expr, M + lat, "a1_2_i" + i + "_k" + k + "_xi" + xi);
                }
            }
        }

        //b1
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int i = 0; i < customerNum; i++) {
                    Customer customer = dataModel.getCustomers().get(i);
                    int lng = customer.getLng();
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(lng, varsAssignIK[i][k][xi]);
                    expr.addTerm(-1, b1plus[k][xi]);
                    cplex.addLe(expr, 0, "b1_1_i" + i + "_k" + k + "_xi" + xi);
                }
            }
        }

        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int i = 0; i < customerNum; i++) {
                    Customer customer = dataModel.getCustomers().get(i);
                    int lng = customer.getLng();
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(M, varsAssignIK[i][k][xi]);
                    expr.addTerm(-1, b1minus[k][xi]);
                    cplex.addLe(expr, M + lng, "b1_2_i" + i + "_k" + k + "_xi" + xi);
                }
            }
        }

        //a2
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                IloLinearNumExpr expr = cplex.linearNumExpr();
//                expr.addTerm(1, a1plus[k][xi]);
//                expr.addTerm(-1, a2plus[k][xi]);
//                cplex.addLe(expr, 0, "a2_1_k" + k + "_xi" + xi);
//            }
//        }
//
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int s = 0; s < stationNum; s++) {
//                    StationCandidate stationCandidate = dataModel.getStationCandidates().get(s);
//                    int lat = stationCandidate.getLat();
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(lat, varsAssignSK[s][k][xi]);
//                    expr.addTerm(-1, a2plus[k][xi]);
//                    cplex.addLe(expr, 0, "a2_2_s" + s + "_k" + k + "_xi" + xi);
//                }
//            }
//        }
//
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                IloLinearNumExpr expr = cplex.linearNumExpr();
//                expr.addTerm(1, a1minus[k][xi]);
//                expr.addTerm(-1, a2minus[k][xi]);
//                cplex.addLe(expr, 0, "a2_3_k" + k + "_xi" + xi);
//            }
//        }
//
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int s = 0; s < stationNum; s++) {
//                    StationCandidate stationCandidate = dataModel.getStationCandidates().get(s);
//                    int lat = stationCandidate.getLat();
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, varsAssignSK[s][k][xi]);
//                    expr.addTerm(-1, a2minus[k][xi]);
//                    cplex.addLe(expr, M + lat, "a2_4_s" + s + "_k" + k + "_xi" + xi);
//                }
//            }
//        }
//
//        //b2
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                IloLinearNumExpr expr = cplex.linearNumExpr();
//                expr.addTerm(1, b1plus[k][xi]);
//                expr.addTerm(-1, b2plus[k][xi]);
//                cplex.addLe(expr, 0, "b2_1_k" + k + "_xi" + xi);
//            }
//        }
//
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int s = 0; s < stationNum; s++) {
//                    StationCandidate stationCandidate = dataModel.getStationCandidates().get(s);
//                    int lng = stationCandidate.getLng();
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(lng, varsAssignSK[s][k][xi]);
//                    expr.addTerm(-1, b2plus[k][xi]);
//                    cplex.addLe(expr, 0, "b2_2_s" + s + "_k" + k + "_xi" + xi);
//                }
//            }
//        }
//
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                IloLinearNumExpr expr = cplex.linearNumExpr();
//                expr.addTerm(1, b1minus[k][xi]);
//                expr.addTerm(-1, b2minus[k][xi]);
//                cplex.addLe(expr, 0, "b2_3_k" + k + "_xi" + xi);
//            }
//        }
//
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int s = 0; s < stationNum; s++) {
//                    StationCandidate stationCandidate = dataModel.getStationCandidates().get(s);
//                    int lng = stationCandidate.getLng();
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, varsAssignSK[s][k][xi]);
//                    expr.addTerm(-1, b2minus[k][xi]);
//                    cplex.addLe(expr, M + lng, "b2_4_s" + s + "_k" + k + "_xi" + xi);
//                }
//            }
//        }
//        //a_1n^-0.5
//        M = dataModel.getCoordinateMax();
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 1; j <= workerCapacityNum; j++) {
//                    double jr = Math.pow(j, -0.5);
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma1[k][j][xi]);
//                    cplex.addGe(expr, 0, "a_1n^-0.5_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, a1plus[k][xi]);
//                    expr1.addTerm(jr, a1minus[k][xi]);
//                    expr1.addTerm(-1, gamma1[k][j][xi]);
//                    cplex.addGe(expr1, 0, "a_1n^-0.5_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, a1minus[k][xi]);
//                    expr2.addTerm(jr, a1plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma1[k][j][xi]);
//                    cplex.addLe(expr2, M, "a_1n^-0.5_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
//        //a_2n^-0.5
//
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 1; j <= workerCapacityNum; j++) {
//                    double jr = Math.pow(j, -0.5);
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma2[k][j][xi]);
//                    cplex.addGe(expr, 0, "a_2n^-0.5_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, a2plus[k][xi]);
//                    expr1.addTerm(jr, a2minus[k][xi]);
//                    expr1.addTerm(-1, gamma2[k][j][xi]);
//                    cplex.addGe(expr1, 0, "a_2n^-0.5_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, a2minus[k][xi]);
//                    expr2.addTerm(jr, a2plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma2[k][j][xi]);
//                    cplex.addLe(expr2, M, "a_2n^-0.5_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
//
//        //b_1n^-0.5
//
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 1; j <= workerCapacityNum; j++) {
//                    double jr = Math.pow(j, -0.5);
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma3[k][j][xi]);
//                    cplex.addGe(expr, 0, "b_1n^-0.5_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, b1plus[k][xi]);
//                    expr1.addTerm(jr, b1minus[k][xi]);
//                    expr1.addTerm(-1, gamma3[k][j][xi]);
//                    cplex.addGe(expr1, 0, "b_1n^-0.5_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, b1minus[k][xi]);
//                    expr2.addTerm(jr, b1plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma3[k][j][xi]);
//                    cplex.addLe(expr2, M, "b_1n^-0.5_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
//
//        //b_2n^-0.5
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 1; j <= workerCapacityNum; j++) {
//                    double jr = Math.pow(j, -0.5);
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma4[k][j][xi]);
//                    cplex.addGe(expr, 0, "b_2n^-0.5_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, b2plus[k][xi]);
//                    expr1.addTerm(jr, b2minus[k][xi]);
//                    expr1.addTerm(-1, gamma4[k][j][xi]);
//                    cplex.addGe(expr1, 0, "b_2n^-0.5_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, b2minus[k][xi]);
//                    expr2.addTerm(jr, b2plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma4[k][j][xi]);
//                    cplex.addLe(expr2, M, "b_2n^-0.5_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }

        //a_1n^0.5
//        M = dataModel.getCoordinateMax() * 2.34;
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    double jr = Math.pow(j, 0.5);
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma5[k][j][xi]);
//                    cplex.addGe(expr, 0, "a_1n^0.5_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, a1plus[k][xi]);
//                    expr1.addTerm(jr, a1minus[k][xi]);
//                    expr1.addTerm(-1, gamma5[k][j][xi]);
//                    cplex.addGe(expr1, 0, "a_1n^0.5_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, a1minus[k][xi]);
//                    expr2.addTerm(jr, a1plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma5[k][j][xi]);
//                    cplex.addLe(expr2, M, "a_1n^0.5_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
        //a_2n^0.5

//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    double jr = Math.pow(j, 0.5);
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma6[k][j][xi]);
//                    cplex.addGe(expr, 0, "a_2n^0.5_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, a2plus[k][xi]);
//                    expr1.addTerm(jr, a2minus[k][xi]);
//                    expr1.addTerm(-1, gamma6[k][j][xi]);
//                    cplex.addGe(expr1, 0, "a_2n^0.5_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, a2minus[k][xi]);
//                    expr2.addTerm(jr, a2plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma6[k][j][xi]);
//                    cplex.addLe(expr2, M, "a_2n^0.5_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }

//        b_1n^0.5

//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    double jr = 2;
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma7[k][j][xi]);
//                    cplex.addGe(expr, 0, "b_1n^0.5_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, b1plus[k][xi]);
//                    expr1.addTerm(jr, b1minus[k][xi]);
//                    expr1.addTerm(-1, gamma7[k][j][xi]);
//                    cplex.addGe(expr1, 0, "b_1n^0.5_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, b1minus[k][xi]);
//                    expr2.addTerm(jr, b1plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma7[k][j][xi]);
//                    cplex.addLe(expr2, M, "b_1n^0.5_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }

        //b_2n^0.5
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    double jr = Math.pow(j, 0.5);
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma8[k][j][xi]);
//                    cplex.addGe(expr, 0, "b_2n^0.5_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, b2plus[k][xi]);
//                    expr1.addTerm(jr, b2minus[k][xi]);
//                    expr1.addTerm(-1, gamma8[k][j][xi]);
//                    cplex.addGe(expr1, 0, "b_2n^0.5_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, b2minus[k][xi]);
//                    expr2.addTerm(jr, b2plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma8[k][j][xi]);
//                    cplex.addLe(expr2, M, "b_2n^0.5_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }

//        a_1n
        M = dataModel.getCoordinateMax() * workerCapacityNum;
        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int j = 0; j <= workerCapacityNum; j++) {
                    double jr = j;
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(M, u[k][j][xi]);
                    expr.addTerm(-1, gamma9[k][j][xi]);
                    cplex.addGe(expr, 0, "a_1n_1_k" + k + "_j" + j + "_xi" + xi);
                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
                    expr1.addTerm(jr, a1plus[k][xi]);
                    expr1.addTerm(jr, a1minus[k][xi]);
                    expr1.addTerm(-1, gamma9[k][j][xi]);
                    cplex.addGe(expr1, 0, "a_1n_2_k" + k + "_j" + j + "_xi" + xi);
                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
                    expr2.addTerm(jr, a1minus[k][xi]);
                    expr2.addTerm(jr, a1plus[k][xi]);
                    expr2.addTerm(M, u[k][j][xi]);
                    expr2.addTerm(-1, gamma9[k][j][xi]);
                    cplex.addLe(expr2, M, "a_1n_3_k" + k + "_j" + j + "_xi" + xi);
                }
            }
        }
//        a_2n

//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    double jr = j;
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma10[k][j][xi]);
//                    cplex.addGe(expr, 0, "a_2n_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, a2plus[k][xi]);
//                    expr1.addTerm(jr, a2minus[k][xi]);
//                    expr1.addTerm(-1, gamma10[k][j][xi]);
//                    cplex.addGe(expr1, 0, "a_2n_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, a2minus[k][xi]);
//                    expr2.addTerm(jr, a2plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma10[k][j][xi]);
//                    cplex.addLe(expr2, M, "a_2n_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }

//        b_1n

        for (int xi = 0; xi < scenarioNum; xi++) {
            for (int k = 0; k < workerNum; k++) {
                for (int j = 0; j <= workerCapacityNum; j++) {
                    double jr = j;
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    expr.addTerm(M, u[k][j][xi]);
                    expr.addTerm(-1, gamma11[k][j][xi]);
                    cplex.addGe(expr, 0, "b_1n_1_k" + k + "_j" + j + "_xi" + xi);
                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
                    expr1.addTerm(jr, b1plus[k][xi]);
                    expr1.addTerm(jr, b1minus[k][xi]);
                    expr1.addTerm(-1, gamma11[k][j][xi]);
                    cplex.addGe(expr1, 0, "b_1n_2_k" + k + "_j" + j + "_xi" + xi);
                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
                    expr2.addTerm(jr, b1minus[k][xi]);
                    expr2.addTerm(jr, b1plus[k][xi]);
                    expr2.addTerm(M, u[k][j][xi]);
                    expr2.addTerm(-1, gamma11[k][j][xi]);
                    cplex.addLe(expr2, M, "b_1n_3_k" + k + "_j" + j + "_xi" + xi);
                }
            }
        }

        //b_2n
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    double jr = j;
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma12[k][j][xi]);
//                    cplex.addGe(expr, 0, "b_2n_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, b2plus[k][xi]);
//                    expr1.addTerm(jr, b2minus[k][xi]);
//                    expr1.addTerm(-1, gamma12[k][j][xi]);
//                    cplex.addGe(expr1, 0, "b_2n_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, b2minus[k][xi]);
//                    expr2.addTerm(jr, b2plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma12[k][j][xi]);
//                    cplex.addLe(expr2, M, "b_2n_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
//
//        //a_1n^1.5
//        M = dataModel.getCoordinateMax() * Math.pow(workerCapacityNum, 1.5);
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    double jr = Math.pow(j, 1.5);
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma13[k][j][xi]);
//                    cplex.addGe(expr, 0, "a_1n^1.5_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, a1plus[k][xi]);
//                    expr1.addTerm(jr, a1minus[k][xi]);
//                    expr1.addTerm(-1, gamma13[k][j][xi]);
//                    cplex.addGe(expr1, 0, "a_1n^1.5_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, a1minus[k][xi]);
//                    expr2.addTerm(jr, a1plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma13[k][j][xi]);
//                    cplex.addLe(expr2, M, "a_1n^1.5_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
//        //a_2n^1.5
//
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    double jr = Math.pow(j, 1.5);
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma14[k][j][xi]);
//                    cplex.addGe(expr, 0, "a_2n^1.5_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, a2plus[k][xi]);
//                    expr1.addTerm(jr, a2minus[k][xi]);
//                    expr1.addTerm(-1, gamma14[k][j][xi]);
//                    cplex.addGe(expr1, 0, "a_2n^1.5_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, a2minus[k][xi]);
//                    expr2.addTerm(jr, a2plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma14[k][j][xi]);
//                    cplex.addLe(expr2, M, "a_2n^1.5_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
//
//        //b_1n^1.5
//
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    double jr = Math.pow(j, 1.5);
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma15[k][j][xi]);
//                    cplex.addGe(expr, 0, "b_1n^1.5_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, b1plus[k][xi]);
//                    expr1.addTerm(jr, b1minus[k][xi]);
//                    expr1.addTerm(-1, gamma15[k][j][xi]);
//                    cplex.addGe(expr1, 0, "b_1n^1.5_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, b1minus[k][xi]);
//                    expr2.addTerm(jr, b1plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma15[k][j][xi]);
//                    cplex.addLe(expr2, M, "b_1n^1.5_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
//
//        //b_2n^1.5
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 0; j <= workerCapacityNum; j++) {
//                    double jr = Math.pow(j, 1.5);
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, gamma16[k][j][xi]);
//                    cplex.addGe(expr, 0, "b_2n^1.5_1_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr1 = cplex.linearNumExpr();
//                    expr1.addTerm(jr, b2plus[k][xi]);
//                    expr1.addTerm(jr, b2minus[k][xi]);
//                    expr1.addTerm(-1, gamma16[k][j][xi]);
//                    cplex.addGe(expr1, 0, "b_2n^1.5_2_k" + k + "_j" + j + "_xi" + xi);
//                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
//                    expr2.addTerm(jr, b2minus[k][xi]);
//                    expr2.addTerm(jr, b2plus[k][xi]);
//                    expr2.addTerm(M, u[k][j][xi]);
//                    expr2.addTerm(-1, gamma16[k][j][xi]);
//                    cplex.addLe(expr2, M, "b_2n^1.5_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
//
//
//        //s_a
//
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int i = 0; i < customerNum; i++) {
//                    for (int ip = 0; ip < customerNum; ip++) {
//                        IloLinearIntExpr expr = cplex.linearIntExpr();
//                        expr.addTerm(1, varsAssignIK[i][k][xi]);
//                        expr.addTerm(1, varsAssignIK[ip][k][xi]);
//                        expr.addTerm(-2, xPrime[i][ip][k][xi]);
//                        cplex.addGe(expr, 0, "xPrime1_i" + i + "_ip" + ip + "_k" + k + "_xi" + xi);
//                    }
//                }
//            }
//        }
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int i = 0; i < customerNum; i++) {
//                    for (int ip = 0; ip < customerNum; ip++) {
//                        IloLinearIntExpr expr = cplex.linearIntExpr();
//                        expr.addTerm(1, varsAssignIK[i][k][xi]);
//                        expr.addTerm(1, varsAssignIK[ip][k][xi]);
//                        expr.addTerm(-1, xPrime[i][ip][k][xi]);
//                        cplex.addLe(expr, 1, "xPrime2_i" + i + "_ip" + ip + "_k" + k + "_xi" + xi);
//                    }
//                }
//            }
//        }
//
//        M = dataModel.getCoordinateSDinOneRoute();
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 2; j <= workerCapacityNum; j++) {
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, delta[k][j][xi]);
//                    cplex.addGe(expr, 0, "sa_1_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 2; j <= workerCapacityNum; j++) {
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    for (int i = 0; i < customerNum; i++) {
//                        for (int ip = 0; ip < customerNum; ip++) {
//                            int lati=dataModel.getCustomers().get(i).getLat();
//                            int latip=dataModel.getCustomers().get(ip).getLat();
//                            expr.addTerm(Math.abs(lati-latip) / (j*(j-1) * 1.0), xPrime[i][ip][k][xi]);
//                        }
//                    }
//                    expr.addTerm(-1, delta[k][j][xi]);
//                    cplex.addGe(expr, 0, "sa_2_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 2; j <= workerCapacityNum; j++) {
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    for (int i = 0; i < customerNum; i++) {
//                        for (int ip = 0; ip < customerNum; ip++) {
//                            int lati=dataModel.getCustomers().get(i).getLat();
//                            int latip=dataModel.getCustomers().get(ip).getLat();
//                            expr.addTerm(Math.abs(lati-latip) / (j*(j-1) * 1.0), xPrime[i][ip][k][xi]);
//
//                        }
//                    }
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, delta[k][j][xi]);
//                    cplex.addLe(expr, M, "sa_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
//        //s_b
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 2; j <= workerCapacityNum; j++) {
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, deltaPrime[k][j][xi]);
//                    cplex.addGe(expr, 0, "sa_1_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 2; j <= workerCapacityNum; j++) {
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    for (int i = 0; i < customerNum; i++) {
//                        for (int ip = 0; ip < customerNum; ip++) {
//                            int lngi=dataModel.getCustomers().get(i).getLng();
//                            int lngip=dataModel.getCustomers().get(ip).getLng();
//                            expr.addTerm(Math.abs(lngi-lngip) / (j*(j-1) * 1.0), xPrime[i][ip][k][xi]);
//
//                        }
//                    }
//                    expr.addTerm(-1, deltaPrime[k][j][xi]);
//                    cplex.addGe(expr, 0, "sa_2_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }
//        for (int xi = 0; xi < scenarioNum; xi++) {
//            for (int k = 0; k < workerNum; k++) {
//                for (int j = 2; j <= workerCapacityNum; j++) {
//                    IloLinearNumExpr expr = cplex.linearNumExpr();
//                    for (int i = 0; i < customerNum; i++) {
//                        for (int ip = 0; ip < customerNum; ip++) {
//                            int lngi=dataModel.getCustomers().get(i).getLng();
//                            int lngip=dataModel.getCustomers().get(ip).getLng();
//                            expr.addTerm(Math.abs(lngi-lngip) / (j*(j-1) * 1.0), xPrime[i][ip][k][xi]);
//
//                        }
//                    }
//                    expr.addTerm(M, u[k][j][xi]);
//                    expr.addTerm(-1, deltaPrime[k][j][xi]);
//                    cplex.addLe(expr, M, "sa_3_k" + k + "_j" + j + "_xi" + xi);
//                }
//            }
//        }


        mipDataD = new MipDataD(cplex, varsLocation, varsAssignIK, tao, varsAssignSK, unServed, z, theta, dmax1, u, f1, varTheta, epsilon, dmax2, f2, a1plus, a1minus, b1plus, b1minus, a2plus, a2minus, b2plus, b2minus, gamma1, gamma2, gamma3, gamma4, gamma5, gamma6, gamma7, gamma8, gamma9,
                gamma10, gamma11, gamma12, gamma13, gamma14, gamma15, gamma16, xPrime, delta, deltaPrime);
    }
}
