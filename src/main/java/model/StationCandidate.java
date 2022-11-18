package model;

import lombok.Data;

/**
 * @author Wang Li
 * @description
 * @date 6/19/22 6:41 PM
 */
@Data
public class StationCandidate {
    private int index;
    private int nodeIndex;
    private int lat;
    private int lng;
    private int capaLower;
    private int capaUpper;
    private int fixedCost=0;
    private double capacityCost=1;
    public int hashCode() {
        return nodeIndex;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof StationCandidate))
            return false;
        StationCandidate other = (StationCandidate) o;
        return (this.nodeIndex == other.nodeIndex);
    }
    public String toString(){
            String s = "StationCandidate: " + this.index +"\n";
            return s;

    }
}
