package locationAssignmentBAP.model;

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
    public final Set<StationCandidate> fixedStations = new HashSet<>();

    public LocationAssignment(Instance instance) {
        this.instance = instance;
    }
    public LocationAssignment(Instance instance,double[][] fixedLocationSolution) {
        this.instance = instance;
        for(int s=0;s<fixedLocationSolution.length;s++){
            boolean isChosen=false;
            for (int t=0;t<fixedLocationSolution[0].length;t++){
                if(MathProgrammingUtil.doubleToBoolean(fixedLocationSolution[s][t])){
                    isChosen=true;
                    this.fixedStations.add(instance.getStationCandidates().get(s));
                }
            }
            if(!isChosen){
                this.incompatibleStations.add(instance.getStationCandidates().get(s));
            }
        }
    }

    @Override
    public String getName() {
        return null;
    }
}
