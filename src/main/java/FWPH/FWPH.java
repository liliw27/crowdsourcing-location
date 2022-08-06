package FWPH;

import FWPH.model.FWPHInput;
import FWPH.model.FWPHSolution;
import MIP.mipDeterministic.MipD;
import ilog.concert.IloException;
import io.Reader;
import model.Instance;
import FWPH.model.SolutionValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 7/5/22 11:51 PM
 */
public class FWPH {
    //Reference: SIAM J. OPTIM. BOLAND
    public static void main(String[] args) throws FileNotFoundException, IloException {
        File file = new File("dataset/instance/instance");
        Instance instance = Reader.readInstance(file, 5, 0, 5, 20, 2, 1);
        //get instances for each scenario
        List<Instance> instanceList = Util.getInstances(instance);
        FWPHInput fwphInput = new FWPHInput();
        fwphInput.setInstanceList(instanceList);
        double[] probability = new double[instanceList.size()];
        for (int i = 0; i < probability.length; i++) {
            instanceList.get(i).getScenarios().get(0).setProbability(1.0/instanceList.size());
            probability[i] = 1.0/instanceList.size();
        }
        fwphInput.setProbability(probability);
        FWPHSolution fwphSolution=Util.solveFWPH(fwphInput);
    }
}




