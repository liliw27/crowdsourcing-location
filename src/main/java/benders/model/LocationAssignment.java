package benders.model;

import model.Instance;
import model.StationCandidate;
import org.jorlib.frameworks.columnGeneration.model.ModelInterface;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/7 18:08
 */
public class LocationAssignment implements ModelInterface {
    public final Instance instance;
    public final Set<StationCandidate> incompatibleStations = new HashSet<>();
    public final List<StationCandidate> fixedStations = new ArrayList<>();
    public final int[] capacity;

    public LocationAssignment(Instance instance, int[] capacity) {
        this.instance = instance;
        this.capacity=capacity;
        for(int s=0;s<capacity.length;s++){
           if(capacity[s]==0){
//               incompatibleStations.add(instance.getStationCandidates().get(s));
           }else if(capacity[s]>0){
//               fixedStations.add(instance.getStationCandidates().get(s));
           }
        }
    }

    @Override
    public String getName() {
        return null;
    }
}
