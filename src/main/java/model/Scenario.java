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
    private int[] customerDemand;
    private double probability;
}
