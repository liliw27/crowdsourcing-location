package util;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import io.StopMsgException;
import locationAssignmentBAP.cg.column.AssignmentColumn;
import locationAssignmentBAP.cg.column.AssignmentColumn_true;
import model.Customer;
import model.Instance;
import model.Scenario;
import model.StationCandidate;
import model.Worker;
import org.jgrapht.util.VertexPair;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 6/20/22 4:04 PM
 */
public class Util {
    public static int calTravelTime(int lat1, int lng1, int lat2, int lng2) {

        double distance = Math.sqrt((lat1 - lat2) * (lat1 - lat2) + (lng1 - lng2) * (lng1 - lng2));
//        int travelTime = (int) (distance * 20 / (Constants.speed * 1000) * 60);
        int travelTime = (int) distance;
        if (travelTime == 0) {
            travelTime = 1;
        }

        return travelTime;
    }

    public static double getCost( Set<Customer> customers,Worker worker, StationCandidate stationCandidate, Instance instance) {
        double cost = 0;
        //cost+=GlobalVariable.daysNum*dataModel.instance.getModelCoe()[x]*
        //1. number of location n_k^\xi
        cost += GlobalVariable.daysNum * instance.getModelCoe()[1] * customers.size();
        //3. D_{1k}^\xi the longest distance between the station and the customer locations
        double D1 = 0;
        double sumD = 0;
        for (Customer customer : customers) {
            cost-=GlobalVariable.daysNum*customer.getUnservedPenalty();
            int travelTime = calTravelTime(customer.getLat(), customer.getLng(), stationCandidate.getLat(), stationCandidate.getLng());
            sumD += travelTime;
            if (D1 < travelTime) {
                D1 = travelTime;
            }
        }
        cost += GlobalVariable.daysNum * instance.getModelCoe()[3] * D1;
        //4. \bar{d}_{1k}^\xi the average distance between the station and customer locations
        double bard1 = sumD / customers.size();
        cost+=GlobalVariable.daysNum * instance.getModelCoe()[4]*bard1;
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
        cost+=GlobalVariable.daysNum * instance.getModelCoe()[7]*bard2;
        //20. gamma_9 a_1n(a_1 The maximum latitudinal difference between a pair of customer locations)
        int minlat=Integer.MAX_VALUE;
        int maxlat=Integer.MIN_VALUE;
        int minlng=Integer.MAX_VALUE;
        int maxlng=Integer.MIN_VALUE;
        for(Customer customer:customers){
            if(minlat>customer.getLat()){
                minlat=customer.getLat();
            }
            if(maxlat<customer.getLat()){
                maxlat=customer.getLat();
            }
            if(minlng>customer.getLng()){
                minlng=customer.getLng();
            }
            if(maxlng<customer.getLng()){
                maxlng=customer.getLng();
            }
        }
        cost+=GlobalVariable.daysNum * instance.getModelCoe()[20]*(maxlat-minlat)*customers.size();

        //22. gamma_11 b_1n(b_1 The maximum longitudinal difference between a pair of customer locations)

        cost+=GlobalVariable.daysNum * instance.getModelCoe()[22]*(maxlng-minlng)*customers.size();
        return cost;
    }

    public static IloNumVar[][] getVar(){
        int stationNum=GlobalVariable.stationNum;
        int typeNum=GlobalVariable.typeNum;
        IloNumVar[][] locationVars=new IloNumVar[stationNum][typeNum];
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

    public static IloNumVar[][] getVar(double[][] fixedLocationSolution){
        int stationNum=GlobalVariable.stationNum;
        int typeNum=GlobalVariable.typeNum;
        IloNumVar[][] locationVars=new IloNumVar[stationNum][typeNum];
        try {
            IloCplex cplex0 = new IloCplex();
            for (int s = 0; s < stationNum; s++) {
                for (int t = 0; t < typeNum; t++) {
                    if(MathProgrammingUtil.doubleToBoolean(fixedLocationSolution[s][t])){
                        locationVars[s][t] = cplex0.numVar(1, 1, "y_" + s + "_" + t);
                    }else {
                        locationVars[s][t] = cplex0.numVar(0, 0, "y_" + s + "_" + t);
                    }
                }
            }
        } catch (IloException e) {
            e.printStackTrace();
        }
        return locationVars;
    }
//    public static List<AssignmentColumn> getInitialSolution(Instance instance, List<PricingProblem> pricingProblems) {
//        List<AssignmentColumn> assignmentColumns = new ArrayList<>();
//        int stationIndex = 0;
//        int accumulatedOccupiedCapS = 0;
//        int workerIndex = 0;
//        int accumulatedOccupiedCapW = 0;
//        Worker worker = instance.getScenarios().get(0).getAvailableWorkers().get(workerIndex);
//        StationCandidate stationCandidate = instance.getStationCandidates().get(stationIndex);
//        Set<Customer> customers = new HashSet<>();
//
//        for (Customer customer : instance.getCustomers()) {
//            int accuS = accumulatedOccupiedCapS + instance.getScenarios().get(0).getCustomerDemand()[customer.getIndex()];
//            int accuW = accumulatedOccupiedCapW + instance.getScenarios().get(0).getCustomerDemand()[customer.getIndex()];
//            if (accuS <= instance.getType()[instance.getType().length - 1] && accuW <= worker.getCapacity() && customers.size() < instance.getWorkerCapacityNum()) {
//                accumulatedOccupiedCapS = accuS;
//                accumulatedOccupiedCapW = accuW;
//                customers.add(customer);
//            } else if (accuS > instance.getType()[instance.getType().length - 1]) {
//                double cost = Util.getCost(customers, worker, stationCandidate, instance);
//
//                AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblems.get(workerIndex), false, "artificialInitial", cost, worker, customers, stationCandidate);
//                assignmentColumns.add(assignmentColumn_true);
//                accumulatedOccupiedCapS = instance.getScenarios().get(0).getCustomerDemand()[customer.getIndex()];
//                accumulatedOccupiedCapW = instance.getScenarios().get(0).getCustomerDemand()[customer.getIndex()];
//                stationIndex++;
//                workerIndex++;
//
//                worker = instance.getScenarios().get(0).getAvailableWorkers().get(workerIndex);
//                stationCandidate = instance.getStationCandidates().get(stationIndex);
//                customers = new HashSet<>();
//                customers.add(customer);
//                continue;
//            } else if (accuW > worker.getCapacity() || customers.size() >= instance.getWorkerCapacityNum()) {
//                double cost = Util.getCost(customers, worker, stationCandidate, instance);
//                AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblems.get(workerIndex), true, "artificialInitial", cost, worker, customers, stationCandidate);
//                assignmentColumns.add(assignmentColumn_true);
//
//                accumulatedOccupiedCapS = accuS;
//                accumulatedOccupiedCapW = instance.getScenarios().get(0).getCustomerDemand()[customer.getIndex()];
//                workerIndex++;
//                if(workerIndex==instance.getWorkers().size()){
//                    break;
//                }
//                worker = instance.getWorkers().get(workerIndex);
//                customers = new HashSet<>();
//                customers.add(customer);
//                continue;
//            }
//            if (customer.getIndex() == instance.getCustomers().size() - 1) {
//                double cost = Util.getCost(customers, worker, stationCandidate, instance);
//                AssignmentColumn_true assignmentColumn_true = new AssignmentColumn_true(pricingProblems.get(workerIndex),  true, "artificialInitial", cost, worker, customers, stationCandidate);
//                assignmentColumns.add(assignmentColumn_true);
//            }
//        }
//        List<AssignmentColumn_true> column_trueList=new ArrayList<>();
//        for(AssignmentColumn column:assignmentColumns){
//            AssignmentColumn_true column_true=(AssignmentColumn_true) column;
//            for(StationCandidate stationCandidate1:instance.getStationCandidates()){
//                if(stationCandidate1.equals(column_true.stationCandidate)){
//                    continue;
//                }
//                for(Worker worker1:instance.getWorkers()){
//                    if(worker1.equals(column_true.worker)){
//                        continue;
//                    }
//                    double cost=Util.getCost(column_true.customers,worker1,stationCandidate1,instance);
//                    AssignmentColumn_true column_truenew=new AssignmentColumn_true(pricingProblems.get(worker1.getIndex()),  true, "artificialInitial",cost,worker1,column_true.customers,stationCandidate1);
//                    column_trueList.add(column_truenew);
//                }
//
//            }
//        }
//        assignmentColumns.addAll(column_trueList);
//        return assignmentColumns;
//    }

    public static int getDemand(Set<Customer> customers, Scenario scenario){
        int demand=0;
        for(Customer customer:customers){
            demand+=scenario.getCustomerDemand()[customer.getIndex()];
        }
        return demand;
    }






}
