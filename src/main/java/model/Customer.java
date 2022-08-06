package model;

import lombok.Data;

/**
 * @author Wang Li
 * @description
 * @date 6/19/22 8:11 PM
 */
@Data
public class Customer {
    private int index;
    private int nodeIndex;
    private int lat;
    private int lng;
    private int demandExpected;
    private int unservedPenalty=100;
}
