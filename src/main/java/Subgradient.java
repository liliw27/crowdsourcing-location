import MIP.mipDeterministic.MipD;
import ilog.concert.IloException;
import io.Reader;
import model.Customer;
import model.Instance;
import model.Scenario;
import model.StationCandidate;
import model.Worker;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 7/5/22 11:51 PM
 */
public class Subgradient {

//reference: EJOR Wangshuaian Mengqiang
    public static void main(String[] args) throws FileNotFoundException, IloException {
        File file = new File("dataset/instance/instance");
        Instance instance = Reader.readInstance(file, 5, 0, 5,20, 2, 1);
        List<Instance> instanceSingleScenarios = new ArrayList<>();
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
            instanceSingleScenarios.add(instance1);
        }
        int h = 0;
        double[][][] lambda = new double[(instanceSingleScenarios.size() - 1)][instance.getStationCandidates().size()][instance.getType().length];
        double[][][] sumHs = new double[(instanceSingleScenarios.size() - 1)][instance.getStationCandidates().size()][instance.getType().length];
        double lrValue = 0;
        double tao = 0.01;
        double rou = 0.99;
        double[][] locationValue = new double[instance.getStationCandidates().size()][instance.getType().length];
        double[][] locationValueNew;
        while (h < 100) {

//            double t=1.0/h;
            double t = 100 * Math.pow(rou, h);
            locationValueNew = new double[instance.getStationCandidates().size()][instance.getType().length];
            sumHs = new double[(instanceSingleScenarios.size() - 1)][instance.getStationCandidates().size()][instance.getType().length];
            double gap = 0;
            double objective = 0;
            for (int i0 = 0; i0 < instanceSingleScenarios.size(); i0++) {
                Instance instance1 = instanceSingleScenarios.get(i0);
                double[][] lam = new double[instance.getStationCandidates().size()][instance.getType().length];
                if (i0 == 0) {
                    lam = lambda[i0];
                } else if (i0 == instanceSingleScenarios.size() - 1) {
                    for (int j = 0; j < lam.length; j++) {
                        for (int ty = 0; ty < lam[j].length; ty++) {
                            lam[j][ty] = -1 * lambda[i0 - 1][j][ty];
                        }
                    }
                } else {
                    for (int j = 0; j < lam.length; j++) {
                        for (int ty = 0; ty < lam[j].length; ty++) {
                            lam[j][ty] = -1 * lambda[i0 - 1][j][ty] + lambda[i0][j][ty];
                        }

                    }
                }
                instance1.setLambda(lam);
                MipD mip = new MipD(instance1, instance.getScenarios().size());

                long runTime = System.currentTimeMillis();
                System.out.println("Starting branch and bound for " + instance.getName());
                mip.solve();
                runTime = System.currentTimeMillis() - runTime;
                System.out.println("iteration: " + h + "; Scenario: " + instance1.getScenarios().get(0).getIndex() + "; Objective: " + mip.getObjectiveValue() + "; Runtime: " + runTime);
//                int sceNo=instance1.getScenarios().get(0).getIndex();
//                int indexInitial=sceNo*instance.getStationCandidates().size()*2;

                int stationNum = instance.getStationCandidates().size();
                int typeNum = instance.getType().length;

                    for (int i = 0; i < stationNum; i++) {
                        for (int ty = 0; ty < typeNum; ty++) {
                            if(i0 < instanceSingleScenarios.size() - 1){
                                sumHs[i0][i][ty] += mip.getVarsLocationValues()[i][ty];
                            }
                            if (i0 > 0) {
                                sumHs[i0-1][i][ty] -= mip.getVarsLocationValues()[i][ty];
                            }
                        }

                    }



                objective += mip.getSolution().getObj();


                for (int k = 0; k < mip.getVarsLocationValues().length; k++) {
                    for(int ty=0;ty< instance.getType().length;ty++){
                        locationValueNew[k][ty]+=mip.getVarsLocationValues()[k][ty];
                        gap += (mip.getVarsLocationValues()[k][ty] - locationValue[k][ty]) * (mip.getVarsLocationValues()[k][ty] - locationValue[k][ty]);
                    }
                }

            }


            h++;

            if (h == 1) {
                for (int k = 0; k < locationValueNew.length; k++) {
                    for(int ty=0;ty< instance.getType().length;ty++){
                        locationValue[k][ty] = (locationValueNew[k][ty] * 1.0) / instance.getScenarios().size();

                    }
                }

                for (int i = 0; i < lambda.length; i++) {
                    for (int j = 0; j < lambda[i].length; j++) {
                        for(int ty=0;ty< instance.getType().length;ty++){
                            lambda[i][j][ty] += t * sumHs[i][j][ty];

                        }

                    }

                }
//                h++;
                continue;
            }
            gap /= instance.getScenarios().size();
            gap = Math.sqrt(gap);
            System.out.println("========gap:" + gap);
            if (gap <= tao) {
                break;
            } else {
                for (int k = 0; k < locationValueNew.length; k++) {
                    for(int ty=0;ty< instance.getType().length;ty++){
                        locationValue[k][ty] = (locationValueNew[k][ty] * 1.0) / instance.getScenarios().size();

                    }
                }

                for (int i = 0; i < lambda.length; i++) {
                    for (int j = 0; j < lambda[i].length; j++) {
                        for(int ty=0;ty< instance.getType().length;ty++){
                            lambda[i][j][ty] += t * sumHs[i][j][ty];

                        }
                        System.out.println("(iter,s): ("+i+","+j+") "+Arrays.toString(lambda[i][j]));

                    }

                }

            }
//            h++;
        }

    }
}




