package benders.model;

import lombok.Data;
import model.Station;
import model.StationCandidate;
import util.Constants;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 2022/11/17 01:04
 */
@Data
public class Solution {
    private List<Station> stations;
    private double[] capacity;

    double firstStageObj = 0;

    public Solution(List<Station> stations,int size) {
        this.stations = stations;
        capacity=new double[size];
        for (Station station : stations) {
            if (station.getCapacity() < Constants.EPSILON) {
                station.setCapacity(0);
            }
            capacity[station.getIndex()] = station.getCapacity();
            if (station.getCapacity() > Constants.EPSILON) {
                firstStageObj += station.getFixedCost();
                firstStageObj += station.getCapacityCost() * capacity[station.getIndex()];
            }

        }

    }
}
