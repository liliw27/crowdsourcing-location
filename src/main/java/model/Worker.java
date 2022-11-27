package model;

import lombok.Data;

import java.util.Arrays;

/**
 * @author Wang Li
 * @description
 * @date 6/19/22 9:22 PM
 */
@Data
public class Worker {
    private int index;
    private int indexO;
    private int indexD;
    private int latO;
    private int lngO;
    private int latD;
    private int lngD;
    private int capacity;
    private int travelTOD;
    private int maxDetour;
    private double costPerKm=0.94;

    public int hashCode() {
        return 27 * indexO + 31 * indexD;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Worker))
            return false;
        Worker other = (Worker) o;
        return (this.indexO == other.indexO && this.indexD == other.indexD);
    }

    public String toString() {
        String s = "";
        s = "Worker: " + this.index + "indexO: " + indexO + "indexD: " + indexD + "\n";
        return s;
    }
}
