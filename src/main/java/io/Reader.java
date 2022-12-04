package io;

import model.Station;
import model.Worker;
import model.Customer;
import model.Instance;
import model.Scenario;
import model.StationCandidate;
import scala.collection.immutable.Stream;
import util.Constants;
import util.GlobalVariable;
import util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * @author Wang Li
 * @description
 * @date 6/19/22 6:38 PM
 */
public class Reader {
    public static Instance readInstance(File file, int scenarioNum, int randomSeed, int numS, int numC, int numW, double lambdaW) throws FileNotFoundException {
        //lambdaW: the ratio of total demand of customers on the total capacity of workers
        Random random = new Random(randomSeed);
        List<Worker> workers = new ArrayList<>();
        List<Customer> customers = new ArrayList<>();
        List<StationCandidate> stationCandidates = new ArrayList<>();
        List<Scenario> scenarios = new ArrayList<>();
        int[][] travelCostMatrix;
        Instance instance = new Instance();


        Scanner scanner = new Scanner(file);
        String string = scanner.nextLine();
        String[] split = string.split(":");
        int stationNum = Integer.parseInt(split[1]);
        string = scanner.nextLine();
        split = string.split(":");
        int workerNum = Integer.parseInt(split[1]);
        string = scanner.nextLine();
        split = string.split(":");
        int customerNum = Integer.parseInt(split[1]);
        int index = 0;

        scanner.nextLine();

        for (int i = 0; i < stationNum; i++) {
            StationCandidate station = new Station();
            string = scanner.nextLine();
            split = string.split(" ");
            station.setLat(Integer.parseInt(split[1]));
            station.setLng(Integer.parseInt(split[2]));
            station.setCapaLower(Integer.parseInt(split[3]));
            station.setCapaUpper(Integer.parseInt(split[4]));
            stationCandidates.add(station);

        }
        for (int i = 0; i < stationCandidates.size(); i++) {
            StationCandidate stationCandidate = stationCandidates.get(i);
            stationCandidate.setNodeIndex(index);
            stationCandidate.setIndex(i);
            index++;
        }
        scanner.nextLine();

        for (int i = 0; i < workerNum; i++) {
            Worker worker = new Worker();
            string = scanner.nextLine();
            split = string.split(" ");

            int latO = Integer.parseInt(split[1]);
            int lngO = Integer.parseInt(split[2]);
            int latD = Integer.parseInt(split[3]);
            int lngD = Integer.parseInt(split[4]);
            int capacity = Integer.parseInt(split[5]);
            worker.setLatO(latO);
            worker.setLngO(lngO);
            worker.setLatD(latD);
            worker.setLngD(lngD);
            worker.setCapacity(capacity);


            int travelTOD = Util.calTravelTime(latO, lngO, latD, lngD);
            worker.setTravelTOD(travelTOD);
            worker.setMaxDetour(travelTOD);

            workers.add(worker);
        }
//        Collections.shuffle(workers, random);
//        int numW = Math.min((int) (numC * ratio), workers.size());
        for (int i = 0; i < workers.size(); i++) {
            Worker worker = workers.get(i);
            worker.setIndexO(index);
            index++;
            worker.setIndexD(index);
            index++;
            worker.setIndex(i);
        }

        scanner.nextLine();

        for (int i = 0; i < customerNum; i++) {
            Customer customer = new Customer();
            string = scanner.nextLine();
            split = string.split(" ");
            customer.setLat(Integer.parseInt(split[1]));
            customer.setLng(Integer.parseInt(split[2]));
            customer.setDemandExpected(Integer.parseInt(split[3]));
            customers.add(customer);
            customer.setNodeIndex(index);
            index++;
            customer.setIndex(i);
        }
//        Collections.shuffle(customers,random);

        int totalNodes = workers.size() * 2 + customers.size() + stationCandidates.size();
        travelCostMatrix = new int[totalNodes][totalNodes];
        if (GlobalVariable.isReadMatrix) {
            scanner.nextLine();

            for (int i = 0; i < totalNodes; i++) {
                string = scanner.nextLine();
                split = string.split(" ");
                for (int j = 0; j < totalNodes; j++) {
                    travelCostMatrix[i][j] = Integer.parseInt(split[j]);
                }
            }
            for (Worker worker : workers) {
                worker.setTravelTOD(travelCostMatrix[worker.getIndexO()][worker.getIndexD()]);
                worker.setMaxDetour(Math.max(30, travelCostMatrix[worker.getIndexO()][worker.getIndexD()]));
            }
        } else {
            travelCostMatrix = calTravelTimeMatrix(workers, customers, stationCandidates);
        }

        stationCandidates = stationCandidates.subList(0, numS);

        workers = workers.subList(0, numW);

        customers = customers.subList(0, numC);


        for (int i = 0; i < workers.size(); i++) {
            double ratio = customers.size() * 1.0 / (lambdaW * workers.size());
            double d = customers.get(i % customers.size()).getDemandExpected();
            workers.get(i).setCapacity((int) (ratio * d));
        }


        scenarios = generateScenarios(scenarioNum, customers, workers, numC, numW, random, lambdaW);
        instance.setScenarios(scenarios);
        instance.setWorkers(workers);
        instance.setCustomers(customers);
        instance.setStationCandidates(stationCandidates);
        instance.setTravelCostMatrix(travelCostMatrix);
        instance.setName(file.getName());
        double[] modelCoe = new double[30];
        modelCoe[4] = 1.06357853;
        modelCoe[7] = 1.11416112;
        modelCoe[3] = 0.20291482;
        modelCoe[6] = 0.22328071;
        modelCoe[1] = 2.98403633;
//        modelCoe[8]=0.05;
//        modelCoe[9]=0.05;
        modelCoe[16] = 0.0005;
        modelCoe[17] = 0.0005;
        modelCoe[18] = 0.0005;
        modelCoe[19] = 0.0005;
        modelCoe[20] = 0.03384613;//0.0002;
        modelCoe[21] = 0.0002;
        modelCoe[22] = 0.02948948;//0.0002;
        modelCoe[23] = 0.0002;
        instance.setModelCoe(modelCoe);

        int typeNum = 2;
        int[] type = new int[2];
        type[0] = 100;
        type[1] = 200;
        instance.setType(type);
        double[][] lambda = new double[stationNum][typeNum];
        instance.setLambda(lambda);
        Constants.totalPenalty = getTotalPenalty(customers);
        return instance;
    }

    private static int getTotalPenalty(List<Customer> customers) {
        int totalpen = 0;
        for (Customer customer : customers) {
            totalpen += customer.getUnservedPenalty();
        }
        return totalpen;
    }

    private static int[][] calTravelTimeMatrix(List<Worker> workers, List<Customer> parcels, List<StationCandidate> stations) {
        int totalNodes = workers.size() * 2 + parcels.size() + stations.size();
        int[][] travelTimeMatrix = new int[totalNodes][totalNodes];
        for (Worker worker : workers) {
            int o = worker.getIndexO();

            int d = worker.getIndexD();
            travelTimeMatrix[o][d] = Util.calTravelTime(worker.getLatO(), worker.getLngO(), worker.getLatD(), worker.getLngD());
//            travelTimeMatrix[o][d]=0;
            travelTimeMatrix[d][o] = travelTimeMatrix[o][d];
            for (StationCandidate station : stations) {
                int i = worker.getIndexO();
                int j = station.getNodeIndex();
                int k = worker.getIndexD();
                travelTimeMatrix[i][j] = Util.calTravelTime(worker.getLatO(), worker.getLngO(), station.getLat(), station.getLng());
                travelTimeMatrix[j][k] = Util.calTravelTime(station.getLat(), station.getLng(), worker.getLatD(), worker.getLngD());
            }
            for (Customer parcel : parcels) {
                int i = parcel.getNodeIndex();
                int j = worker.getIndexD();
                travelTimeMatrix[i][j] = Util.calTravelTime(parcel.getLat(), parcel.getLng(), worker.getLatD(), worker.getLngD());
            }
        }
        for (StationCandidate station : stations) {
            for (Customer parcel : parcels) {
                int i = station.getNodeIndex();
                int j = parcel.getNodeIndex();
                travelTimeMatrix[i][j] = Util.calTravelTime(station.getLat(), station.getLng(), parcel.getLat(), parcel.getLng());
            }
        }
        for (Customer parcel : parcels) {
            for (Customer parcel1 : parcels) {
                int i = parcel.getNodeIndex();
                int j = parcel1.getNodeIndex();
                if (i != j) {
                    travelTimeMatrix[i][j] = Util.calTravelTime(parcel.getLat(), parcel.getLng(), parcel1.getLat(), parcel1.getLng());
                }
            }

        }
        return travelTimeMatrix;
    }

    private static List<Scenario> generateScenarios(int scenarioNum, List<Customer> customers, List<Worker> workers, int numC, int numW, Random random, double lambdaW) {
        List<Scenario> scenarios = new ArrayList<>();
        double prob = 1.0 / scenarioNum;
        for (int i = 0; i < scenarioNum; i++) {
            List<Worker> availableWorkers = new ArrayList<>();
            Scenario scenario = new Scenario();
            int[] isWorkerAvailable = new int[numW];
            int[] customerDemand = new int[numC];
            int[] workerCapacity = new int[numW];
            Arrays.fill(isWorkerAvailable, 1);
            availableWorkers.addAll(workers);
//            for(int j=0;j<numW;j++){
//                double ran=getRandom(random);
//                if(ran<lambdaW){
//                    isWorkerAvailable[j]=1;
//                    availableWorkers.add(workers.get(j));
//                }else {
//                    isWorkerAvailable[j]=0;
//                }
//            }
            int maxValue = Integer.MIN_VALUE;
            int totalD = 0;
            for (int j = 0; j < numC; j++) {
                double ran = getRandom(random);
                double std = customers.get(j).getDemandExpected() / 10;
                int d = (int) (std * ran + customers.get(j).getDemandExpected());
                if (d == 0) {
                    d = customers.get(j).getDemandExpected();
                }
                if (d > maxValue) {
                    maxValue = d;
                }
                customerDemand[j] = d;
                totalD += d;
            }
            for (int j = 0; j < numW; j++) {
                double ran = getRandom(random);
                double std = workers.get(j).getCapacity() / 10;
                workerCapacity[j] = (int) (std * ran + workers.get(j).getCapacity());
                if (workerCapacity[j] < maxValue) {
                    workerCapacity[j] = workers.get(j).getCapacity();
                }
            }


            scenario.setIndex(i);
            scenario.setIsWorkerAvailable(isWorkerAvailable);
            scenario.setCustomerDemand(customerDemand);
            scenario.setAvailableWorkers(availableWorkers);
            scenario.setWorkerCapacity(workerCapacity);
            scenario.setProbability(prob);
            scenario.setDemandTotal(totalD);
            scenarios.add(scenario);

        }
        return scenarios;
    }

    private static double getRandom(Random random) {
        return random.nextGaussian();
    }
}
