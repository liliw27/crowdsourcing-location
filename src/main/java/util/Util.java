package util;

import benders.cg.LocationAssignmentCGSolver;
import benders.cg.column.AssignmentColumn_true;
import benders.model.LocationAssignment;
import benders.model.Solution;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import model.Customer;
import model.Instance;
import model.Scenario;
import model.Station;
import model.StationCandidate;
import model.Worker;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Wang Li
 * @description
 * @date 6/20/22 4:04 PM
 */
public class Util {
    static Booster booster;

    static {
        try {
            booster = XGBoost.loadModel("model.bin");
        } catch (XGBoostError e) {
            throw new RuntimeException(e);
        }
    }

    public static double calTravelTime0(Worker worker, Station station, Customer parcel, int[][] travelTimeMatrix) {
        double travelTime = 0;
        travelTime += travelTimeMatrix[worker.getIndexO()][station.getNodeIndex()];
        travelTime += travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
        travelTime += travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
        return travelTime;

    }

    public static int calTravelTime(int lat1, int lng1, int lat2, int lng2) {

        double distance = Math.sqrt((lat1 - lat2) * (lat1 - lat2) + (lng1 - lng2) * (lng1 - lng2));
        int travelTime = (int) (distance * 20 / (Constants.speed * 1000) * 60);
//        int travelTime = (int) distance;
        if (travelTime == 0) {
            travelTime = 1;
        }

        return travelTime;
    }

    public static double getCostE(Set<Customer> customers, Worker worker, StationCandidate stationCandidate, Instance instance,Scenario scenario) {
        double cost = 0;
        if (customers.size() == 0) {
            return 1000;
        }
        //cost+=GlobalVariable.daysNum*dataModel.instance.getModelCoe()[x]*
        //1. number of location n_k^\xi
        cost += GlobalVariable.daysNum * instance.getModelCoe()[1] * customers.size();
        //3. D_{1k}^\xi the longest distance between the station and the customer locations
        double D1 = 0;
        double sumD = 0;
        for (Customer customer : customers) {
            int travelTime = calTravelTime(customer.getLat(), customer.getLng(), stationCandidate.getLat(), stationCandidate.getLng());
            sumD += travelTime;
            if (D1 < travelTime) {
                D1 = travelTime;
            }
        }
        cost += GlobalVariable.daysNum * instance.getModelCoe()[3] * D1;
        //4. \bar{d}_{1k}^\xi the average distance between the station and customer locations
        double bard1 = sumD / customers.size();
        cost += GlobalVariable.daysNum * instance.getModelCoe()[4] * bard1;
        //6. D_{2k}^\xi is the longest distance between the destination of crowd-courier k and the customer locations
        double D2 = 0;
        double sumD2 = 0;
        for (Customer customer : customers) {
            int travelTime = calTravelTime(customer.getLat(), customer.getLng(), worker.getLatD(), worker.getLngD());
            sumD2 += travelTime;
            if (D2 < travelTime) {
                D2 = travelTime;
            }
        }
        cost += GlobalVariable.daysNum * instance.getModelCoe()[6] * D2;
        //7. \bar{d}_{2k}^\xi the average distance between crowd-courier k and customer locations
        double bard2 = sumD2 / customers.size();
        cost += GlobalVariable.daysNum * instance.getModelCoe()[7] * bard2;
        //20. gamma_9 a_1n(a_1 The maximum latitudinal difference between a pair of customer locations)
        int minlat = Integer.MAX_VALUE;
        int maxlat = Integer.MIN_VALUE;
        int minlng = Integer.MAX_VALUE;
        int maxlng = Integer.MIN_VALUE;
        for (Customer customer : customers) {
            if (minlat > customer.getLat()) {
                minlat = customer.getLat();
            }
            if (maxlat < customer.getLat()) {
                maxlat = customer.getLat();
            }
            if (minlng > customer.getLng()) {
                minlng = customer.getLng();
            }
            if (maxlng < customer.getLng()) {
                maxlng = customer.getLng();
            }
        }
        cost += GlobalVariable.daysNum * instance.getModelCoe()[20] * (maxlat - minlat) * customers.size();

        //22. gamma_11 b_1n(b_1 The maximum longitudinal difference between a pair of customer locations)

        cost += GlobalVariable.daysNum * instance.getModelCoe()[22] * (maxlng - minlng) * customers.size();
        cost = cost * instance.getCompensation() / 60;
        cost = cost - (instance.getCompensation() * worker.getTravelTOD() * 1.0) / 60;

        for (Customer customer : customers) {
            cost -= GlobalVariable.daysNum *scenario.getCustomerDemand()[customer.getIndex()]* customer.getUnservedPenalty();

        }
        return cost;
    }


    public static double getCost(Set<Customer> customers, Worker worker, StationCandidate stationCandidate, Instance instance) {
        double cost = 0;
        if (customers.size() == 0) {
            return 1000;
        }
        //cost+=GlobalVariable.daysNum*dataModel.instance.getModelCoe()[x]*
        //1. number of location n_k^\xi
        cost += GlobalVariable.daysNum * instance.getModelCoe()[1] * customers.size();
        //3. D_{1k}^\xi the longest distance between the station and the customer locations
        double D1 = 0;
        double sumD = 0;
        for (Customer customer : customers) {
            cost -= GlobalVariable.daysNum * customer.getUnservedPenalty();
            int travelTime = calTravelTime(customer.getLat(), customer.getLng(), stationCandidate.getLat(), stationCandidate.getLng());
            sumD += travelTime;
            if (D1 < travelTime) {
                D1 = travelTime;
            }
        }
        cost += GlobalVariable.daysNum * instance.getModelCoe()[3] * D1;
        //4. \bar{d}_{1k}^\xi the average distance between the station and customer locations
        double bard1 = sumD / customers.size();
        cost += GlobalVariable.daysNum * instance.getModelCoe()[4] * bard1;
        //6. D_{2k}^\xi is the longest distance between the destination of crowd-courier k and the customer locations
        double D2 = 0;
        double sumD2 = 0;
        for (Customer customer : customers) {
            int travelTime = calTravelTime(customer.getLat(), customer.getLng(), worker.getLatD(), worker.getLngD());
            sumD2 += travelTime;
            if (D2 < travelTime) {
                D2 = travelTime;
            }
        }
        cost += GlobalVariable.daysNum * instance.getModelCoe()[6] * D2;
        //7. \bar{d}_{2k}^\xi the average distance between crowd-courier k and customer locations
        double bard2 = sumD2 / customers.size();
        cost += GlobalVariable.daysNum * instance.getModelCoe()[7] * bard2;
        //20. gamma_9 a_1n(a_1 The maximum latitudinal difference between a pair of customer locations)
        int minlat = Integer.MAX_VALUE;
        int maxlat = Integer.MIN_VALUE;
        int minlng = Integer.MAX_VALUE;
        int maxlng = Integer.MIN_VALUE;
        for (Customer customer : customers) {
            if (minlat > customer.getLat()) {
                minlat = customer.getLat();
            }
            if (maxlat < customer.getLat()) {
                maxlat = customer.getLat();
            }
            if (minlng > customer.getLng()) {
                minlng = customer.getLng();
            }
            if (maxlng < customer.getLng()) {
                maxlng = customer.getLng();
            }
        }
        cost += GlobalVariable.daysNum * instance.getModelCoe()[20] * (maxlat - minlat) * customers.size();

        //22. gamma_11 b_1n(b_1 The maximum longitudinal difference between a pair of customer locations)

        cost += GlobalVariable.daysNum * instance.getModelCoe()[22] * (maxlng - minlng) * customers.size();
        return cost;
    }

    public static double getCostXGB(Station station, Worker worker, List<Customer> parcelList, int[][] travelTimeMatrix) throws XGBoostError {

        if (parcelList.size() == 1) {
            double travelTime = Util.calTravelTime0(worker, station, parcelList.get(0), travelTimeMatrix);
            return travelTime - travelTimeMatrix[worker.getIndexO()][worker.getIndexD()];
        }
        int avgTravelTimeS = 0;
        int avgTravelTimeW = 0;
        int maxTravelTimeS = 0;
        int maxTravelTimeW = 0;
        int minTravelTimeS = Integer.MAX_VALUE;
        int minTravelTimeW = Integer.MAX_VALUE;
        int areaP;
        int areaTotal;
        int maxLatDiffP;
        int maxLngDiffP;
        int maxLatDiffTotal;
        int maxLngDiffTotal;
        int avgLatDiffP;
        int avgLngDiffP;
        int avgLatDiffTotal;
        int avgLngDiffTotal;


        int maxLatP = 0;
        int minLatP = Integer.MAX_VALUE;
        int maxLngP = 0;
        int minLngP = Integer.MAX_VALUE;
        int maxLatTotal;
        int minLatTotal;
        int maxLngTotal;
        int minLngTotal;
        int latDiffPsum = 0;
        int lngDiffPsum = 0;
        int countP = 0;
        int latDiffTotalsum = 0;
        int lngDiffTotalsum = 0;
        int countT = 0;

        for (Customer parcel : parcelList) {
            avgTravelTimeS += travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
            avgTravelTimeW += travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
            if (maxTravelTimeS < travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()]) {
                maxTravelTimeS = travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
            }
            if (minTravelTimeS > travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()]) {
                minTravelTimeS = travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
            }
            if (maxTravelTimeW < travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()]) {
                maxTravelTimeW = travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
            }
            if (minTravelTimeW > travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()]) {
                minTravelTimeW = travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
            }
            if (maxLatP < parcel.getLat()) {
                maxLatP = parcel.getLat();
            }
            if (minLatP > parcel.getLat()) {
                minLatP = parcel.getLat();
            }
            if (maxLngP < parcel.getLng()) {
                maxLngP = parcel.getLng();
            }
            if (minLngP > parcel.getLng()) {
                minLngP = parcel.getLng();
            }
            for (Customer parcel1 : parcelList) {
                if (parcel.getIndex() >= parcel1.getIndex()) {
                    continue;
                }
                latDiffPsum += Math.abs(parcel.getLat() - parcel1.getLat());
                lngDiffPsum += Math.abs(parcel.getLng() - parcel1.getLng());
                countP++;
                latDiffTotalsum += Math.abs(parcel.getLat() - parcel1.getLat());
                lngDiffTotalsum += Math.abs(parcel.getLng() - parcel1.getLng());
                countT++;

            }
            latDiffTotalsum += Math.abs(station.getLat() - parcel.getLat());
            lngDiffTotalsum += Math.abs(station.getLng() - parcel.getLng());
            countT++;
            latDiffTotalsum += Math.abs(parcel.getLat() - worker.getLatD());
            lngDiffTotalsum += Math.abs(parcel.getLng() - worker.getLngD());
            countT++;
        }
        latDiffTotalsum += Math.abs(station.getLat() - worker.getLatD());
        lngDiffTotalsum += Math.abs(station.getLng() - worker.getLngD());
        countT++;

        avgTravelTimeW = avgTravelTimeW / parcelList.size();
        avgTravelTimeS = avgTravelTimeS / parcelList.size();

        maxLatTotal = Math.max(maxLatP, station.getLat());
        maxLatTotal = Math.max(maxLatTotal, worker.getLatD());
        maxLngTotal = Math.max(maxLngP, station.getLng());
        maxLngTotal = Math.max(maxLngTotal, worker.getLngD());
        minLatTotal = Math.min(minLatP, station.getLat());
        minLatTotal = Math.min(minLatTotal, worker.getLatD());
        minLngTotal = Math.min(minLngP, station.getLng());
        minLngTotal = Math.min(minLngTotal, worker.getLngD());

        maxLatDiffP = maxLatP - minLatP;
        maxLngDiffP = maxLngP - minLngP;
        maxLatDiffTotal = maxLatTotal - minLatTotal;
        maxLngDiffTotal = maxLngTotal - minLngTotal;
        areaP = maxLatDiffP * maxLngDiffP;
        areaTotal = maxLatDiffTotal * maxLngDiffTotal;
        avgLatDiffP = latDiffPsum / countP;
        avgLngDiffP = lngDiffPsum / countP;
        avgLatDiffTotal = latDiffTotalsum / countT;
        avgLngDiffTotal = lngDiffTotalsum / countT;


        float[] predictData = new float[]{21, avgTravelTimeS, avgTravelTimeW, maxTravelTimeS, maxTravelTimeW, minTravelTimeS, minTravelTimeW, areaP,
                areaTotal, maxLatDiffP, maxLngDiffP, maxLatDiffTotal, maxLngDiffTotal, avgLatDiffP, avgLngDiffP, avgLatDiffTotal, avgLngDiffTotal};


        DMatrix dtest0 = new DMatrix(predictData, 1, 17, 0.0F);


        float[][] predicts = booster.predict(dtest0, false, 1);

        return predicts[0][0] - travelTimeMatrix[worker.getIndexO()][worker.getIndexD()];

    }

    public static Set<Pair<Station, Worker>> getAvailableSWPair(List<StationCandidate> stations, List<Worker> workers, int[][] travelTMatrix) {
        Set<Pair<Station, Worker>> pairSWSet = new HashSet<>();
        for (StationCandidate station : stations) {
            for (Worker worker : workers) {
                int travelT = travelTMatrix[worker.getIndexO()][station.getIndex()] + travelTMatrix[station.getIndex()][worker.getIndexD()];

                if (travelT > worker.getTravelTOD() + worker.getMaxDetour()) {
                    continue;
                }

                pairSWSet.add(Pair.of((Station) station, worker));
            }
        }
        return pairSWSet;
    }

    public static Map<Pair<Station, Worker>, Set<Customer>> getUnavailablePairSWPMap(Set<Pair<Station, Worker>> pairSWSet, List<Customer> parcels, int[][] travelTMatrix) {
        Map<Pair<Station, Worker>, Set<Customer>> unavailableSWPMap = new HashMap<>();
        for (Pair<Station, Worker> pair : pairSWSet) {
            Worker worker = pair.getRight();
            Station station = pair.getLeft();
            Set<Customer> unavailableParcelSet = new HashSet<>();
            for (Customer parcel : parcels) {
                int indexO = worker.getIndexO();
                int indexS = station.getNodeIndex();
                int indexP = parcel.getNodeIndex();
                int indexD = worker.getIndexD();
                int travelTOS = travelTMatrix[indexO][indexS];
                int travelTSP = travelTMatrix[indexS][indexP];
                int travelTPD = travelTMatrix[indexP][indexD];
                int travelT = travelTOS + travelTSP + travelTPD;

                if (travelT > worker.getTravelTOD() + worker.getMaxDetour()) {
                    unavailableParcelSet.add(parcel);
                }
            }
            unavailableSWPMap.put(pair, unavailableParcelSet);
//            System.out.println("station:"+station.getIndex()+" worker:"+worker.getIndex()+" unavailable parcel size:"+unavailableParcelSet.size());
        }
        return unavailableSWPMap;
    }

    public static Set<AssignmentColumn_true> generateJob(List<Customer> parcels, Set<Pair<Station, Worker>> pairSWSet, Map<Pair<Station, Worker>, Set<Customer>> unavailableSWPMap, Instance instance, int it) throws IOException, XGBoostError {
        Set<AssignmentColumn_true> columnSet = new LinkedHashSet<>();
        int removedCnt = 0;
        int count = 0;
        if (instance.getWorkerCapacityNum() > 1 && pairSWSet.size() > 10) {
            List<List<Integer>> combines = new ArrayList<>();
            for (int i = 2; i <= instance.getWorkerCapacityNum(); i++) {
                Util.combine(parcels.size(), i, combines);
            }
            long runTime = System.currentTimeMillis();
            BufferedWriter bf = new BufferedWriter(new FileWriter("dataset/predict.svm.txt"));
            Collections.shuffle(combines);
            label1:
            for (Pair<Station, Worker> pair : pairSWSet) {
//                StringBuilder stringBuilder = new StringBuilder(10400000);
                lable:
                for (List<Integer> combine : combines) {
                    Set<Customer> customers = new HashSet<>(combine.size());
                    for (int index : combine) {
                        Customer parcel = parcels.get(index - 1);
                        if (unavailableSWPMap.get(pair).contains(parcel)) {
                            continue lable;
                        }
                        customers.add(parcels.get(index - 1));
                    }

                    //PricingProblem associatedPricingProblem, boolean isArtificial, String creator, double cost, int demand, Worker worker, Set<Customer> customers, StationCandidate stationCandidate
                    AssignmentColumn_true assignmentColumn = new AssignmentColumn_true(null, false, "enumerate", pair.getRight(), customers, (StationCandidate) pair.getLeft(), count);

                    columnSet.add(assignmentColumn);
                    count++;
//                /*summarize the features*/
//                String features = SampleGeneration.getFeature(instanceSample);
                    String features = 21.0 + "\t" + Util.getFeature(pair.getLeft(), pair.getRight(), customers, instance.getTravelCostMatrix());
                    bf.write(features);
                    bf.flush();
                }

            }

            bf.close();
            System.out.println("runtime of feature calculation and record:" + (System.currentTimeMillis() - runTime));
            runTime = System.currentTimeMillis();
            System.out.println("count:" + count);
            float[][] predicts;

            if (count > 0) {
                Booster booster = XGBoost.loadModel("model_2real.bin");//"model_2real.bin"
                DMatrix dtest = new DMatrix("dataset/predict.svm.txt");
//                DMatrix dtest = new DMatrix("dataset/predict.svm.txt#dtest" + it + ".cache");
// predict
                predicts = booster.predict(dtest);
                System.out.println("run time of xgb prediction" + (System.currentTimeMillis() - runTime));
                System.out.println("predict length:" + predicts.length);

                Set<AssignmentColumn_true> removeColum = new HashSet<>();
                for (AssignmentColumn_true assignmentColumn : columnSet) {


                    float travelTime = predicts[assignmentColumn.index][0];
                    travelTime += instance.getTravelCostMatrix()[assignmentColumn.worker.getIndexO()][assignmentColumn.stationCandidate.getNodeIndex()];
                    if (travelTime > assignmentColumn.worker.getMaxDetour() + assignmentColumn.worker.getTravelTOD()) {
                        removeColum.add(assignmentColumn);
                        continue;
                    }
//                    assignmentColumn.cost = ((travelTime - instance.getTravelCostMatrix()[assignmentColumn.worker.getIndexO()][assignmentColumn.worker.getIndexD()]) * Constants.speed * 1.0) / 60;
                    assignmentColumn.cost = instance.getCompensation() * (travelTime - assignmentColumn.worker.getTravelTOD()) / 60;
//                    assignmentColumn.cost = instance.getCompensation() * (travelTime ) / 60;

                }
                columnSet.removeAll(removeColum);
                removedCnt = removeColum.size();

            }
        }

        for (Pair<Station, Worker> pair : pairSWSet) {
            for (Customer parcel : parcels) {
                if (unavailableSWPMap.get(pair).contains(parcel)) {
                    removedCnt++;
                    continue;
                }
                double travelTime = Util.calTravelTime0(pair.getRight(), pair.getLeft(), parcel, instance.getTravelCostMatrix());
                if (travelTime > pair.getRight().getMaxDetour()+pair.getRight().getTravelTOD()) {
                    continue;
                }
                Set<Customer> customers = new HashSet<>();
                customers.add(parcel);
                AssignmentColumn_true assignmentColumn = new AssignmentColumn_true(null, false, "enumerate", pair.getRight(), customers, pair.getLeft(), count);
                assignmentColumn.isDemandsSatisfy = new boolean[instance.getScenarios().size()];
                assignmentColumn.demands = new short[instance.getScenarios().size()];
                assignmentColumn.cost = instance.getCompensation() * (travelTime - pair.getRight().getTravelTOD()) / 60;
//                assignmentColumn.cost = instance.getCompensation() * (travelTime ) / 60;
//                assignmentColumn.cost = ((travelTime - pair.getRight().getTravelTOD()) * Constants.speed * 1.0) / 60;

                columnSet.add(assignmentColumn);
                count++;
            }
        }
        System.out.println("removed count: " + removedCnt);

//        Map<Worker,Set<AssignmentColumn_true>> columnMap = new HashMap<>();
//        for(Worker worker:instance.getWorkers()){
//            columnMap.put(worker,new HashSet<>());
//        }
//        for(AssignmentColumn_true column_true:columnSet){
//            columnMap.get(column_true.worker).add(column_true);
//        }

//        return columnMap;
        return columnSet;
    }

    public static String getFeature(Station station, Worker worker, Set<Customer> parcelList, int[][] travelTimeMatrix) {
        int avgTravelTimeS = 0;
        int avgTravelTimeW = 0;
        int maxTravelTimeS = 0;
        int maxTravelTimeW = 0;
        int minTravelTimeS = Integer.MAX_VALUE;
        int minTravelTimeW = Integer.MAX_VALUE;
        int areaP;
        int areaTotal;
        int maxLatDiffP;
        int maxLngDiffP;
        int maxLatDiffTotal;
        int maxLngDiffTotal;
        int avgLatDiffP;
        int avgLngDiffP;
        int avgLatDiffTotal;
        int avgLngDiffTotal;


        int maxLatP = 0;
        int minLatP = Integer.MAX_VALUE;
        int maxLngP = 0;
        int minLngP = Integer.MAX_VALUE;
        int maxLatTotal;
        int minLatTotal;
        int maxLngTotal;
        int minLngTotal;
        int latDiffPsum = 0;
        int lngDiffPsum = 0;
        int countP = 0;
        int latDiffTotalsum = 0;
        int lngDiffTotalsum = 0;
        int countT = 0;

        for (Customer parcel : parcelList) {
            avgTravelTimeS += travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
            avgTravelTimeW += travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
            if (maxTravelTimeS < travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()]) {
                maxTravelTimeS = travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
            }
            if (minTravelTimeS > travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()]) {
                minTravelTimeS = travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
            }
            if (maxTravelTimeW < travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()]) {
                maxTravelTimeW = travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
            }
            if (minTravelTimeW > travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()]) {
                minTravelTimeW = travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
            }
            if (maxLatP < parcel.getLat()) {
                maxLatP = parcel.getLat();
            }
            if (minLatP > parcel.getLat()) {
                minLatP = parcel.getLat();
            }
            if (maxLngP < parcel.getLng()) {
                maxLngP = parcel.getLng();
            }
            if (minLngP > parcel.getLng()) {
                minLngP = parcel.getLng();
            }
            for (Customer parcel1 : parcelList) {
                if (parcel.getIndex() >= parcel1.getIndex()) {
                    continue;
                }
                latDiffPsum += Math.abs(parcel.getLat() - parcel1.getLat());
                lngDiffPsum += Math.abs(parcel.getLng() - parcel1.getLng());
                countP++;
                latDiffTotalsum += Math.abs(parcel.getLat() - parcel1.getLat());
                lngDiffTotalsum += Math.abs(parcel.getLng() - parcel1.getLng());
                countT++;

            }
            latDiffTotalsum += Math.abs(station.getLat() - parcel.getLat());
            lngDiffTotalsum += Math.abs(station.getLng() - parcel.getLng());
            countT++;
            latDiffTotalsum += Math.abs(parcel.getLat() - worker.getLatD());
            lngDiffTotalsum += Math.abs(parcel.getLng() - worker.getLngD());
            countT++;
        }
        latDiffTotalsum += Math.abs(station.getLat() - worker.getLatD());
        lngDiffTotalsum += Math.abs(station.getLng() - worker.getLngD());
        countT++;

        avgTravelTimeW = avgTravelTimeW / parcelList.size();
        avgTravelTimeS = avgTravelTimeS / parcelList.size();

        maxLatTotal = Math.max(maxLatP, station.getLat());
        maxLatTotal = Math.max(maxLatTotal, worker.getLatD());
        maxLngTotal = Math.max(maxLngP, station.getLng());
        maxLngTotal = Math.max(maxLngTotal, worker.getLngD());
        minLatTotal = Math.min(minLatP, station.getLat());
        minLatTotal = Math.min(minLatTotal, worker.getLatD());
        minLngTotal = Math.min(minLngP, station.getLng());
        minLngTotal = Math.min(minLngTotal, worker.getLngD());

        maxLatDiffP = maxLatP - minLatP;
        maxLngDiffP = maxLngP - minLngP;
        maxLatDiffTotal = maxLatTotal - minLatTotal;
        maxLngDiffTotal = maxLngTotal - minLngTotal;
        areaP = maxLatDiffP * maxLngDiffP;
        areaTotal = maxLatDiffTotal * maxLngDiffTotal;
        avgLatDiffP = latDiffPsum / countP;
        avgLngDiffP = lngDiffPsum / countP;
        avgLatDiffTotal = latDiffTotalsum / countT;
        avgLngDiffTotal = lngDiffTotalsum / countT;

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("1:" + avgTravelTimeS + "\t");
        stringBuilder.append("2:" + avgTravelTimeW + "\t");
        stringBuilder.append("3:" + maxTravelTimeS + "\t");
        stringBuilder.append("4:" + maxTravelTimeW + "\t");
        stringBuilder.append("5:" + minTravelTimeS + "\t");
        stringBuilder.append("6:" + minTravelTimeW + "\t");
        stringBuilder.append("7:" + areaP + "\t");
        stringBuilder.append("8:" + areaTotal + "\t");
        stringBuilder.append("9:" + maxLatDiffP + "\t");
        stringBuilder.append("10:" + maxLngDiffP + "\t");
        stringBuilder.append("11:" + maxLatDiffTotal + "\t");
        stringBuilder.append("12:" + maxLngDiffTotal + "\t");
        stringBuilder.append("13:" + avgLatDiffP + "\t");
        stringBuilder.append("14:" + avgLngDiffP + "\t");
        stringBuilder.append("15:" + avgLatDiffTotal + "\t");
        stringBuilder.append("16:" + avgLngDiffTotal + "\n");

        return stringBuilder.toString();
    }


    public static List<List<Integer>> combine(int n, int k, List<List<Integer>> res) {
        if (n <= 0 || k <= 0 || k > n) {
            return res;
        }
        List<Integer> c = new ArrayList<>();
        generateCombinations(n, k, 1, c, res);
        return res;

    }

    /**
     * 回溯求所有组合结果
     *
     * @param n
     * @param k
     * @param start 开始搜索新元素的位置
     * @param c     当前已经找到的组合
     */
    private static void generateCombinations(int n, int k, int start, List<Integer> c, List<List<Integer>> res) {
        if (c.size() == k) {
            //这里需要注意java的值传递
            //此处必须使用重新创建对象的形式，否则 res 列表中存放的都是同一个引用
            res.add(new ArrayList<>(c));
//            for (Integer i : c) {
//                List list = parcelToJob.get(i);
//                list.add(res.size() - 1);
//            }
            return;
        }

        //通过终止条件，进行剪枝优化，避免无效的递归
        //c中还剩 k - c.size()个空位，所以[ i ... n]中至少要有k-c.size()个元素
        //所以i最多为 n - (k - c.size()) + 1
        for (int i = start; i <= n - (k - c.size()) + 1; i++) {
            c.add(i);
            generateCombinations(n, k, i + 1, c, res);
            //记得回溯状态啊
            c.remove(c.size() - 1);
        }
    }

    public static IloNumVar[][] getVar() {
        int stationNum = GlobalVariable.stationNum;
        int typeNum = GlobalVariable.typeNum;
        IloNumVar[][] locationVars = new IloNumVar[stationNum][typeNum];
        try {
            IloCplex cplex0 = new IloCplex();
            for (int s = 0; s < stationNum; s++) {
                for (int t = 0; t < typeNum; t++) {
                    locationVars[s][t] = cplex0.numVar(0, 1, "y_" + s + "_" + t);
                }
            }
        } catch (IloException e) {
            e.printStackTrace();
        }
        return locationVars;
    }

    public static IloNumVar[][] getVar(double[][] fixedLocationSolution) {
        int stationNum = GlobalVariable.stationNum;
        int typeNum = GlobalVariable.typeNum;
        IloNumVar[][] locationVars = new IloNumVar[stationNum][typeNum];
        try {
            IloCplex cplex0 = new IloCplex();
            for (int s = 0; s < stationNum; s++) {
                for (int t = 0; t < typeNum; t++) {
                    if (MathProgrammingUtil.doubleToBoolean(fixedLocationSolution[s][t])) {
                        locationVars[s][t] = cplex0.numVar(1, 1, "y_" + s + "_" + t);
                    } else {
                        locationVars[s][t] = cplex0.numVar(0, 0, "y_" + s + "_" + t);
                    }
                }
            }
        } catch (IloException e) {
            e.printStackTrace();
        }
        return locationVars;
    }

    public static short getDemand(Set<Customer> customers, Scenario scenario) {
        short demand = 0;
        for (Customer customer : customers) {
            demand += scenario.getCustomerDemand()[customer.getIndex()];
        }
        return demand;
    }

    public static List<Scenario> generateScenarios(Instance instance, double coeC, double coeW, int N, JDKRandomGenerator randomGenerator) {
        List<Scenario> scenarios = new ArrayList<>();

        GammaDistribution[] gammacustomers = new GammaDistribution[instance.getCustomers().size()];
        GammaDistribution[] gammaworkers = new GammaDistribution[instance.getWorkers().size()];
        for (Customer customer : instance.getCustomers()) {
            double shape = 1 / (coeC * coeC);
            double scale = coeC * coeC * customer.getDemandExpected();
            gammacustomers[customer.getIndex()] = new GammaDistribution(randomGenerator, shape, scale);
        }
        for (Worker worker : instance.getWorkers()) {
            double shape = 1 / (coeW * coeW);
            double scale = coeW * coeW * worker.getCapacity();
            gammaworkers[worker.getIndex()] = new GammaDistribution(randomGenerator, shape, scale);
        }

        double prob = 1.0 / N;
        for (int i = 0; i < N; i++) {
            List<Worker> availableWorkers = new ArrayList<>();
            Scenario scenario = new Scenario();
            int[] isWorkerAvailable = new int[instance.getWorkers().size()];
            int[] customerDemand = new int[instance.getCustomers().size()];
            int[] workerCapacity = new int[instance.getWorkers().size()];
            Arrays.fill(isWorkerAvailable, 1);
            availableWorkers.addAll(instance.getWorkers());
            int totalD = 0;
            for (int j = 0; j < instance.getCustomers().size(); j++) {
                customerDemand[j] = (int) gammacustomers[j].sample();
                totalD += customerDemand[j];
            }
            for (int j = 0; j < instance.getWorkers().size(); j++) {
                workerCapacity[j] = (int) gammaworkers[j].sample();
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

    public static double evaluate(Instance instance, Solution solution, List<Scenario> scenarioEvaluation) throws XGBoostError, IOException, IloException {
        instance.setScenarios(scenarioEvaluation);
        for (AssignmentColumn_true assignmentColumn_true : GlobalVariable.columns) {
            assignmentColumn_true.isDemandsSatisfy = new boolean[instance.getScenarios().size()];
            assignmentColumn_true.demands = new short[instance.getScenarios().size()];
            for (int xi = 0; xi < instance.getScenarios().size(); xi++) {

                Scenario scenario0 = instance.getScenarios().get(xi);
                short demand = Util.getDemand(assignmentColumn_true.customers, scenario0);

                assignmentColumn_true.isDemandsSatisfy[xi] = (demand <= scenario0.getWorkerCapacity()[assignmentColumn_true.worker.getIndex()]);
                assignmentColumn_true.demands[xi] = demand;
            }
        }
        ExecutorService executor = Executors.newFixedThreadPool(Constants.MAXTHREADS);
        List<Future<Void>> futures = new ArrayList<>(scenarioEvaluation.size());
        //generate all possible columns and calculate the travel cost
        List<LocationAssignmentCGSolver> cgSolvers = new ArrayList<>(scenarioEvaluation.size());
        double[] objForEachScenario = new double[scenarioEvaluation.size()];
        for (int m = 0; m < scenarioEvaluation.size(); m++) {
            Scenario scenario = scenarioEvaluation.get(m);

            LocationAssignment locationAssignment = new LocationAssignment(instance, solution.getCapacity());
            LocationAssignmentCGSolver cgSolver = new LocationAssignmentCGSolver(locationAssignment, scenario);
            cgSolvers.add(cgSolver);
//            cgSolver.solveCG();
            Future<Void> f = executor.submit(cgSolver);
            futures.add(f);


        }
        for (Future<Void> f : futures) {
            try {
                f.get(); //get() is a blocking procedure
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        double avg = 0;

        for (int i = 0; i < cgSolvers.size(); i++) {
            LocationAssignmentCGSolver cgSolver = cgSolvers.get(i);
            objForEachScenario[i] = cgSolver.getObjectiveValue() + instance.getUnservedPenalty() * scenarioEvaluation.get(i).getDemandTotal();
            avg += objForEachScenario[i];
        }
        avg = avg / scenarioEvaluation.size();
        avg = solution.getFirstStageObj() + avg;
        return avg;
    }

}
