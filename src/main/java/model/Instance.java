package model;

import lombok.Data;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 6/19/22 6:38 PM
 */
@Data
public class Instance {
    private List<Worker> workers;
    private List<Customer> customers;
    private List<StationCandidate> stationCandidates;
    //    private int[][] travelCostMatrixOS;
//    private int[][] travelCostMatrixSI;
//    private int[][] travelCostMatrixII;
//    private int[][] travelCostMatrixID;
    private int[][] travelCostMatrix;
    private List<Scenario> scenarios;
    private int workerCapacityNum = 3;
    private int coordinateMax = 1000;
    private int coordinateSDinOneRoute = coordinateMax * workerCapacityNum * workerCapacityNum;
    private double[] modelCoe = new double[30];
    private String name;
    private double[][] lambda;
    private int[] type;
    private boolean isMultipleCut=false;
    private double unservedPenalty=7.5;//7.5
    private double compensation=18;
    private boolean isCVaR=false;
    private double totalPenalty;
    private double[] penalties;

}
