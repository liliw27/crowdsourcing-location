package benders.model;

import lombok.Data;
import model.StationCandidate;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 2022/11/17 01:04
 */
@Data
public class Solution {
   private List<StationCandidate> stationCandidates;

}
