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
    private double unservedPenalty=7.5;
    public int hashCode() {
        return nodeIndex;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Customer))
            return false;
        Customer other = (Customer) o;
        return (this.nodeIndex == other.nodeIndex);
    }
}
