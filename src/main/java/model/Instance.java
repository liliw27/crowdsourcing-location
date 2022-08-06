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
    private int workerCapacityNum =10;
    private int coordinateMax=1000;
    private int coordinateSDinOneRoute=coordinateMax*workerCapacityNum*workerCapacityNum;
    private double[] modelCoe=new double[30];
    private String name;
    private double[][] lambda;
    private int[] type;
}
