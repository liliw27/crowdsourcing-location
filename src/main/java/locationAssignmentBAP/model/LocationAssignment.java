package locationAssignmentBAP.model;

import model.Instance;
import org.jorlib.frameworks.columnGeneration.model.ModelInterface;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/7 18:08
 */
public class LocationAssignment implements ModelInterface {
    public final Instance instance;

    public LocationAssignment(Instance instance) {
        this.instance = instance;
    }

    @Override
    public String getName() {
        return null;
    }
}
