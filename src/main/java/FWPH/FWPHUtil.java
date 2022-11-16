package FWPH;

import FWPH.model.FWPHInput;
import FWPH.model.FWPHSolution;
import FWPH.model.SDMInput;
import FWPH.model.SDMSolution;
import MIP.mip2ndStage.Mip2ndStage;
import MIP.mipDeterministic.MipD;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import locationAssignmentBAP.LocationAssignmentSolver;
import locationAssignmentBAP.LocationAssignmentSolver2;
import locationAssignmentBAP.model.LocationAssignment;
import model.Customer;
import model.Instance;
import model.Scenario;
import FWPH.model.SolutionValue;
import model.StationCandidate;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;
import util.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 7/9/22 10:40 AM
 */
public class FWPHUtil {
    public static FWPHSolution solveFWPH(FWPHInput fwphInput) throws IloException, TimeLimitExceededException {
        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");// a为am/pm的标记
        Date date = new Date();// 获取当前时间
        List<Instance> instanceList = fwphInput.getInstanceList();
        //initialize lambda, x, z
        FWPHSolution fwphSolutionInitial = FWPHUtil.initial(instanceList);
        double lambda[][][] = fwphSolutionInitial.getLambda();
        SolutionValue[] solutionValues = fwphSolutionInitial.getSolutionValues();
        List<SolutionValue>[] V = fwphSolutionInitial.getV();
        double z_k[][] = fwphSolutionInitial.getZ();
        double z_kminus1[][] = new double[z_k.length][z_k[0].length];
        FWPHSolution fwphSolution = new FWPHSolution();
        double phi_k = 0;
        date = new Date();
        System.out.println("现在时间：" + sdf.format(date)+ "== Start FWPH Iteration!!!");
        for (int k = 0; k < fwphInput.getKmax(); k++) {
            date = new Date();
            System.out.println("现在时间：" + sdf.format(date)+ "== start iter=====================================" + (k + 1));
            lambda = FWPHUtil.updateLambda(lambda, solutionValues, z_k, fwphInput.getRou());
            for (int s = 0; s < z_k.length; s++) {
                for (int t = 0; t < z_k[0].length; t++) {
                    z_kminus1[s][t] = z_k[s][t];
                    z_k[s][t] = 0;
                }
            }
            double x0[][][] = new double[instanceList.size()][z_kminus1.length][z_kminus1[0].length];
            SDMSolution[] sdmSolutions = new SDMSolution[instanceList.size()];
            phi_k = 0;
            for (int xi = 0; xi < instanceList.size(); xi++) {
                for (int s = 0; s < z_kminus1.length; s++) {
                    for (int t = 0; t < z_kminus1[0].length; t++) {
                        x0[xi][s][t] = (1 - fwphInput.getAlpha()) * z_kminus1[s][t] + fwphInput.getAlpha() * solutionValues[xi].getX()[s][t];
                    }
                }
                double[][] lambdaxi = lambda[xi];
                SDMInput sdmInput = new SDMInput();
                sdmInput.setLambdas(lambdaxi);
                sdmInput.setVs(V[xi]);
                sdmInput.setZ(z_kminus1);
                sdmInput.setSolutionValue_kmiuns1(solutionValues[xi]);
                sdmInput.setXs(x0[xi]);
                sdmInput.setRou(fwphInput.getRou());
                sdmInput.setInstance(instanceList.get(xi));
                sdmSolutions[xi] = FWPHUtil.SDM(sdmInput);
                solutionValues[xi] = sdmSolutions[xi].getXY_s();
                phi_k += fwphInput.getProbability()[xi] * sdmSolutions[xi].getPhi_s();

                for (int s = 0; s < z_kminus1.length; s++) {
                    for (int t = 0; t < z_kminus1[0].length; t++) {
                        z_k[s][t] += fwphInput.getProbability()[xi] * sdmSolutions[xi].getXY_s().getX()[s][t];
                    }
                }
            }

            if (isTerminate(z_kminus1, solutionValues, fwphInput.getProbability(), fwphInput.getEpsilon())) {
                break;
            }

        }
        fwphSolution.setV(V);
        fwphSolution.setZ(z_k);
        fwphSolution.setLambda(lambda);
        fwphSolution.setSolutionValues(solutionValues);
        fwphSolution.setPhi(phi_k);
        date = new Date();
        System.out.println("现在时间：" + sdf.format(date)+ "== End FWPH Iteration!!!");
        return fwphSolution;
    }

    public static boolean isTerminate(double[][] z_kminus1, SolutionValue[] solutionValues, double[] probability, double epsilon) {
        double gap = calculateGap(z_kminus1, solutionValues, probability);
        System.out.println("gap===============================" + gap);
        if (gap < epsilon) {
            return true;
        }
        return false;
    }

    public static double calculateGap(double[][] z_kminus1, SolutionValue[] solutionValues, double[] probability) {
        double gap = 0;
        for (int xi = 0; xi < solutionValues.length; xi++) {
            for (int s = 0; s < z_kminus1.length; s++) {
                for (int t = 0; t < z_kminus1[s].length; t++) {
                    gap += probability[xi] * (z_kminus1[s][t] - solutionValues[xi].getX()[s][t]) * (z_kminus1[s][t] - solutionValues[xi].getX()[s][t]);
                }
                System.out.println(Arrays.toString(z_kminus1[s])+" "+Arrays.toString(solutionValues[xi].getX()[s]));
            }
        }
        gap = Math.sqrt(gap);
        return gap;
    }

    public static SDMSolution SDM(SDMInput sdmInput) throws IloException, TimeLimitExceededException {
        //update z, XY, phi, and V

        int tmax = sdmInput.getTmax();
        double[][] lambdasNew;

        double[][] Z = sdmInput.getZ();
        double rou = sdmInput.getRou();
        double[][] lambda = sdmInput.getLambdas();
        List<SolutionValue> solutionValueList = sdmInput.getVs();
        double phi_s = 0;
        SolutionValue solutionValue_t = new SolutionValue();
        for (int t = 0; t < tmax; t++) {
            lambdasNew = getNewLambda(lambda, sdmInput.getXs(), Z, rou);
            sdmInput.getInstance().setLambda(lambdasNew);
            LocationAssignment locationAssignment = new LocationAssignment(sdmInput.getInstance());
//            LocationAssignmentSolver locationAssignmentSolver = new LocationAssignmentSolver(locationAssignment);
//            SolutionValue solutionValue = locationAssignmentSolver.solveInstance();
            LocationAssignmentSolver2 locationAssignmentSolver = new LocationAssignmentSolver2(locationAssignment);
            SolutionValue solutionValue = locationAssignmentSolver.solveInstance();
//            solutionValue = solveInstance(sdmInput.getInstance());
            solutionValueList.add(solutionValue);
            if (t == 1) {
                phi_s = solutionValue.getObj();
            }
            solutionValue_t = solveConvexQuadraticSubproblem(Z, rou, lambda, solutionValueList);
        }
        SDMSolution sdmSolution = new SDMSolution();
        sdmSolution.setPhi_s(phi_s);
        sdmSolution.setXY_s(solutionValue_t);
        sdmSolution.setVs(solutionValueList);
        return sdmSolution;
    }

    public static SolutionValue solveConvexQuadraticSubproblem(double[][] Z, double rou, double[][] lambda, List<SolutionValue> solutionValueList) throws IloException {
        double[] obj3 = new double[solutionValueList.size()];
        for (int i = 0; i < solutionValueList.size(); i++) {
            for (int s = 0; s < lambda.length; s++) {
                for (int t = 0; t < lambda[0].length; t++) {
                    obj3[i] += lambda[s][t] * solutionValueList.get(i).getX()[s][t];
                }
            }
        }

        IloCplex cplex = new IloCplex();
        IloNumVar[] a = new IloNumVar[solutionValueList.size()];
        for (int i = 0; i < solutionValueList.size(); i++) {
            IloNumVar var = cplex.numVar(0, 1, "a_" + i);
            a[i] = var;
        }
        IloNumExpr obj0 = cplex.lqNumExpr();
        IloLinearNumExpr objLinear = cplex.linearNumExpr();
        for (int i = 0; i < solutionValueList.size(); i++) {
            SolutionValue solutionValue = solutionValueList.get(i);
            objLinear.addTerm(a[i], solutionValue.getObjFirst());
            objLinear.addTerm(a[i], solutionValue.getObjSecond());
            objLinear.addTerm(a[i], obj3[i]);
        }
        for (int s = 0; s < lambda.length; s++) {
            for (int t = 0; t < lambda[0].length; t++) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                for (int i = 0; i < solutionValueList.size(); i++) {
                    expr.addTerm(a[i], solutionValueList.get(i).getX()[s][t]);
                }

                IloNumExpr expr1 = cplex.prod(-2 * Z[s][t], expr);
                IloNumExpr lqNumExpr = cplex.prod(expr, expr);

                obj0 = lqNumExpr;
                obj0 = cplex.sum(obj0, expr1);
            }
        }
        obj0 = cplex.sum(obj0, objLinear);
        cplex.addMinimize(obj0);

        //constraints
        IloLinearNumExpr expr = cplex.linearNumExpr();
        for (int i = 0; i < solutionValueList.size(); i++) {
            expr.addTerm(1, a[i]);
        }
        cplex.addEq(expr, 1);

        double[] av = new double[a.length];

        if (cplex.solve()) {
            for (int i = 0; i < a.length; i++) {
                av[i] = cplex.getValue(a[i]);
            }

        } else {

        }
        SolutionValue solutionValue_t = new SolutionValue();
        double[][] x = new double[lambda.length][lambda[0].length];
        double obj1 = 0;
        double obj2 = 0;
        double obj3p = 0;
        for (int i = 0; i < solutionValueList.size(); i++) {
            for (int s = 0; s < lambda.length; s++) {
                for (int t = 0; t < lambda[0].length; t++) {

                    x[s][t] += av[i] * solutionValueList.get(i).getX()[s][t];
                }
            }
            obj1 += av[i] * solutionValueList.get(i).getObjFirst();
            obj2 += av[i] * solutionValueList.get(i).getObjSecond();
            obj3p += av[i] * obj3[i];

        }
        solutionValue_t.setX(x);
        solutionValue_t.setObj(obj3p);
        solutionValue_t.setObjSecond(obj2);
        solutionValue_t.setObjFirst(obj1);
        return solutionValue_t;
    }

    public static double[][] getNewLambda(double[][] lambda, double[][] Xs, double[][] Z, double rou) {
        double[][] lambdasNew = new double[lambda.length][lambda[0].length];
        for (int s = 0; s < lambda.length; s++) {
            for (int t = 0; t < lambda[s].length; t++) {
                lambdasNew[s][t] = lambda[s][t] + rou * (Xs[s][t] - Z[s][t]);
            }
        }

        return lambdasNew;
    }

    public static double[][][] updateLambda(double[][][] lambda, SolutionValue[] solutionValues, double z[][], double rou) {
        System.out.println("update lambda:");
        for (int i = 0; i < lambda.length; i++) {
            for (int j = 0; j < lambda[i].length; j++) {
                for (int k = 0; k < lambda[i][j].length; k++) {
                    lambda[i][j][k] = lambda[i][j][k] + rou * (solutionValues[i].getX()[j][k] - z[j][k]);
                }
                System.out.println(Arrays.toString(lambda[i][j]));
            }
        }
        return lambda;
    }

    public static FWPHSolution initial(List<Instance> instanceList) throws IloException, TimeLimitExceededException {
        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");// a为am/pm的标记
        Date date = new Date();// 获取当前时间
        System.out.println("现在时间：" + sdf.format(date)+ "== Initial Start!");

        FWPHSolution fwphSolution = new FWPHSolution();
        double[][][] lambda = new double[instanceList.size()][instanceList.get(0).getStationCandidates().size()][instanceList.get(0).getType().length];
        SolutionValue[] XY = new SolutionValue[instanceList.size()];
        double phi = 0;//lower bound
        List<SolutionValue>[] V = new List[instanceList.size()];
        for (int i = 0; i < instanceList.size(); i++) {
            V[i] = new ArrayList<>();
        }
        double[][] Z = new double[instanceList.get(0).getStationCandidates().size()][instanceList.get(0).getType().length];
        double[][] x = new double[instanceList.get(0).getStationCandidates().size()][instanceList.get(0).getType().length];

        for (int i = 0; i < instanceList.size(); i++) {
            date = new Date();
            System.out.println("现在时间：" + sdf.format(date)+ "== Start solving Initial solution of instance: "+i);// 输出已经格式化的现在时间（24小时制）
            Instance instance = instanceList.get(i);
            instance.setLambda(lambda[i]);
            LocationAssignment locationAssignment = new LocationAssignment(instance);
            LocationAssignmentSolver2 locationAssignmentSolver = new LocationAssignmentSolver2(locationAssignment);
            date = new Date();
            System.out.println("现在时间：" + sdf.format(date)+ "== Start solving Initial solution of instance: "+i +"! 1st stage + 2nd stage!");
            SolutionValue solutionValue = locationAssignmentSolver.solveInstance();
            date = new Date();
            System.out.println("现在时间：" + sdf.format(date)+ "== End at solving Initial solution of instance: "+i +"! 1st stage + 2nd stage!");

//          SolutionValue solutionValue = solveInstance(instance);

            V[i].add(solutionValue);
            XY[i] = solutionValue;
            if (i == 0) {
                x = solutionValue.getX();
            } else if (i > 0) {
                date = new Date();
                System.out.println("现在时间：" + sdf.format(date)+ "== Start solving Initial solution of instance: "+i +" Only 2nd stage!");
                LocationAssignment locationAssignment2 = new LocationAssignment(instance,x);
                LocationAssignmentSolver2 locationAssignmentSolver2ndS = new LocationAssignmentSolver2(locationAssignment2,x);
               SolutionValue solutionValue1 = locationAssignmentSolver2ndS.solveInstance();
//                               SolutionValue solutionValue1 = solve2ndStage(instance, x);

                date = new Date();
                System.out.println("现在时间：" + sdf.format(date)+ "== End at solving Initial solution of instance: "+i +" Only 2nd stage!");
                V[i].add(solutionValue1);
            }
            phi += instance.getScenarios().get(0).getProbability() * solutionValue.getObj();
            for (int s = 0; s < Z.length; s++) {
                for (int t = 0; t < Z[s].length; t++) {
                    Z[s][t] += instance.getScenarios().get(0).getProbability() * solutionValue.getX()[s][t];
                }
            }
        }
        fwphSolution.setLambda(lambda);
        fwphSolution.setPhi(phi);
        fwphSolution.setV(V);
        fwphSolution.setSolutionValues(XY);
        fwphSolution.setZ(Z);
        date = new Date();
        System.out.println("现在时间：" + sdf.format(date)+ "== Initial End!");
        return fwphSolution;
    }


    public static SolutionValue solve2ndStage(Instance instance, double[][] x) throws IloException {
        Mip2ndStage mip = new Mip2ndStage(instance, 1, x);
        long runTime = System.currentTimeMillis();
        System.out.println("Starting branch and bound for " + instance.getName());
        mip.solve();
        runTime = System.currentTimeMillis() - runTime;
        System.out.println("Objective: " + mip.getObjectiveValue() + "; Runtime: " + runTime);
        double[][] y1 = mip.getVarsAssignIKValues()[0];//allocationIK
        double[][] y2 = mip.getVarsAssignSKValues()[0];//allocationSK
        double[] y3 = mip.getUnServedValues()[0];//unServed
        double obj = mip.getBestObjValue();
        SolutionValue solutionValue = new SolutionValue();
        solutionValue.setX(x);
//        solutionValue.setY1(y1);
//        solutionValue.setY2(y2);
//        solutionValue.setY3(y3);
        ;
        solutionValue.setObjSecond(mip.getSolution().getSolution2Stages().get(0).getObj());
        solutionValue.setObjFirst(mip.getSolution().getSolution1stage().getObj());
        solutionValue.setObj(solutionValue.getObjFirst() + solutionValue.getObjSecond());
        return solutionValue;
    }

    public static SolutionValue solveInstance(Instance instance) throws IloException {
        MipD mip = new MipD(instance, instance.getScenarios().size());
        long runTime = System.currentTimeMillis();
        System.out.println("Starting branch and bound for " + instance.getName());
        mip.solve();
        runTime = System.currentTimeMillis() - runTime;
        System.out.println("Objective: " + mip.getObjectiveValue() + "; Runtime: " + runTime);
        int[][] x = mip.getVarsLocationValues();//Location
        double[][] xd = new double[x.length][x[0].length];
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[0].length; j++) {
                xd[i][j] = x[i][j];
            }
        }
        double[][][] y1 = mip.getVarsAssignIKValues();//allocationIK
        double[][][] y2 = mip.getVarsAssignSKValues();//allocationSK
        double[][] y3 = mip.getUnServedValues();//unServed
        double[][] tao = mip.getTaoValues();
        double obj = mip.getBestObjValue();
        SolutionValue solutionValue = new SolutionValue();
        solutionValue.setX(xd);
//        solutionValue.setY1(y1);
//        solutionValue.setY2(y2);
//        solutionValue.setY3(y3);
        solutionValue.setObj(obj);
        solutionValue.setObjFirst(mip.getSolution().getSolution1stage().getObj());
        solutionValue.setObjSecond( mip.getSolution().getSolution2Stages().get(0).getObj());

        double[] cost = new double[instance.getWorkers().size()];
        for (int k = 0; k < instance.getWorkers().size(); k++) {
            StationCandidate stationCandidate = new StationCandidate();
            Worker worker = instance.getWorkers().get(k);
            Set<Customer> customers = new HashSet<>();
            if (MathProgrammingUtil.doubleToBoolean(tao[k][0])) {
                for (int s = 0; s < instance.getStationCandidates().size(); s++) {
                    if (MathProgrammingUtil.doubleToBoolean(y2[s][k][0])) {
                        stationCandidate = instance.getStationCandidates().get(s);
                    }
                }
                for (int i = 0; i < instance.getCustomers().size(); i++) {
                    if (MathProgrammingUtil.doubleToBoolean(y1[i][k][0])) {
                        customers.add(instance.getCustomers().get(i));
                    }
                }
                cost[k] = Util.getCost(customers, worker, stationCandidate, instance);
                System.out.println("============" + cost[k]);

            }

        }

        return solutionValue;
    }

    public static List<Instance> getInstances(Instance instance) {
        List<Instance> instanceList = new ArrayList<>();
        //get instances for each scenario
        for (Scenario scenario : instance.getScenarios()) {
            Instance instance1 = new Instance();
            instance1.setWorkers(instance.getWorkers());
            instance1.setCustomers(instance.getCustomers());
            instance1.setStationCandidates(instance.getStationCandidates());
            instance1.setTravelCostMatrix(instance.getTravelCostMatrix());
            instance1.setScenarios(Collections.singletonList(scenario));
            instance1.setWorkerCapacityNum(instance.getWorkerCapacityNum());
            instance1.setCoordinateMax(instance.getCoordinateMax());
            instance1.setCoordinateSDinOneRoute(instance.getCoordinateSDinOneRoute());
            instance1.setModelCoe(instance.getModelCoe());
            instance1.setName(instance.getName() + "scenario: " + scenario.getIndex());
            instance1.setType(instance.getType());
            instanceList.add(instance1);
        }
        return instanceList;
    }
}
