package model;

import lombok.Data;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 6/19/22 6:39 PM
 */
@Data
public class Scenario {
    private int index;
    private int[] isWorkerAvailable;
    private List<Worker> availableWorkers;
    private int[] customerDemand;
    private int[] workerCapacity;
    private double probability;
}
