package benders.cg.pricing;

import benders.model.LocationAssignment;
import model.Scenario;
import model.Worker;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblem;

import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 2022/8/7 17:45
 */
public class PricingProblem extends AbstractPricingProblem<LocationAssignment> {


    public Worker worker;
    public Scenario scenario;

    /**
     * Create a new Pricing Problem
     *
     * @param dataModel Data model
     * @param name      Name of the pricing problem
     * @param worker    a specific worker (we build a specific pricing problem for each available worker)
     */
    public PricingProblem(LocationAssignment dataModel,Worker worker, String name,Scenario scenario) {
        super(dataModel, name);
        this.worker=worker;
        this.scenario=scenario;
    }

    /**
     * initialize the pricing problem
     * @param dualCostsMap
     */
    public void initPricingProblem(Map<String, double[]> dualCostsMap) {
        this.dualCostsMap = dualCostsMap;
    }

}
