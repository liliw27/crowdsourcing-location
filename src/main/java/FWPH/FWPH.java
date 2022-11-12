package FWPH;

import FWPH.model.FWPHInput;
import FWPH.model.FWPHSolution;
import ilog.concert.IloException;
import io.Reader;
import model.Instance;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 7/5/22 11:51 PM
 */
public class FWPH {
    //Reference: SIAM J. OPTIM. BOLAND
    public static void main(String[] args) throws FileNotFoundException, IloException, TimeLimitExceededException {
        File file = new File("dataset/instance/instance");
        Instance instance = Reader.readInstance(file, 5, 0, 5, 10, 5, 1);
        //get instances for each scenario
        List<Instance> instanceList = FWPHUtil.getInstances(instance);
        FWPHInput fwphInput = new FWPHInput();
        fwphInput.setInstanceList(instanceList);
        double[] probability = new double[instanceList.size()];
        for (int i = 0; i < probability.length; i++) {
            instanceList.get(i).getScenarios().get(0).setProbability(1.0/instanceList.size());
            probability[i] = 1.0/instanceList.size();
        }
        fwphInput.setProbability(probability);
        FWPHSolution fwphSolution= FWPHUtil.solveFWPH(fwphInput);
    }
}




