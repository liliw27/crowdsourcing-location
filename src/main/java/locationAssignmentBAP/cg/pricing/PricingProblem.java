package locationAssignmentBAP.cg.pricing;

import locationAssignmentBAP.model.LocationAssignment;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblem;

import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/7 17:45
 */
public class PricingProblem extends AbstractPricingProblem<LocationAssignment> {

//    public Map<String, double[]> dualCostsMap;
    public double[][] dualWS;
    public double[][] dualWC;

    public Worker worker;
    /**
     * Create a new Pricing Problem
     *
     * @param dataModel Data model
     * @param name      Name of the pricing problem
     * @param worker    a specific worker (we build a specific pricing problem for each available worker)
     */
    public PricingProblem(LocationAssignment dataModel,Worker worker, String name) {
        super(dataModel, name);
        this.worker=worker;
    }

    /**
     * initialize the pricing problem
     * @param dualCostsMap
     */
    public void initPricingProblem(Map<String, double[]> dualCostsMap,double[][] dualWS,double[][] dualWC) {
        this.dualCostsMap = dualCostsMap;
        this.dualWS=dualWS;
        this.dualWC=dualWC;
    }

}
