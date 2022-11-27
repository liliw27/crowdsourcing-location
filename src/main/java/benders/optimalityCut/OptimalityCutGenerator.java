package benders.optimalityCut;

import benders.master.MipData;
import ilog.concert.IloException;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloRange;
import model.Instance;
import model.Scenario;
import util.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 2022/11/17 01:28
 */
public class OptimalityCutGenerator extends CutGenerator {
    List<IloRange> optimalityCuts;
    public double valueQ = 0;
    public double[] valuesQ;
    public double valuez;
    private double q=0;

    public OptimalityCutGenerator(Instance instance, MipData mipData, double[] valuesQ, double valueQ, double valuez, List<IloRange> optimalityCuts,double q) {
        super(instance, mipData);
        this.valueQ = valueQ;
        this.valuesQ = valuesQ;
        this.valuez = valuez;
        this.optimalityCuts = optimalityCuts;
        this.q=q;
    }

    @Override
    public List<IloRange> generateInqualities() throws IloException {
        Map<String, double[]>[] dualCostsMaps = mipData.dualCostsMaps;

        double[] objForEachScenario = mipData.objForEachScenario;
        List<IloRange> inequalities = new ArrayList<>();
        if (instance.isMultipleCut()) {
            for (int xi = 0; xi < instance.getScenarios().size(); xi++) {
                Scenario scenario = instance.getScenarios().get(xi);
                q = objForEachScenario[xi];

                if (q > valuesQ[xi] + Constants.EPSILON) {
                    System.out.println("q: " + q + "valueQ: " + valuesQ[xi]);
                    IloLinearNumExpr expr = mipData.cplex.linearNumExpr();
                    expr.addTerm(-1, mipData.varsQ[xi]);
                    double dualSum = 0;

                    for (int i = 0; i < scenario.getCustomerDemand().length; i++) {
                        dualSum -= dualCostsMaps[xi].get("oneVisitPerCustomerAtMost")[i];
                    }
                    for (int k = 0; k < instance.getWorkers().size(); k++) {
                        dualSum -= dualCostsMaps[xi].get("oneRoutePerWorkerAtMost")[k];
                    }

                    for (int s = 0; s < instance.getStationCandidates().size(); s++) {
                        expr.addTerm(dualCostsMaps[xi].get("stationCapConstraint")[s], mipData.varsCapacity[s]);
                    }

                    IloRange iloRange = mipData.cplex.le(expr, dualSum, "optimalityCut" + optimalityCuts.size());
                    inequalities.add(iloRange);
                    optimalityCuts.add(iloRange);
                    System.out.println("add one cut!! total number of cuts" + optimalityCuts.size());
                }
            }
        } else {
            //CVaR cut
            if (instance.isCVaR()) {
                IloLinearNumExpr expr0 = mipData.cplex.linearNumExpr();
                expr0.addTerm(1, mipData.vart);
                double t = 0;
                for (int xi = 0; xi < instance.getScenarios().size(); xi++) {
                    Scenario scenario = instance.getScenarios().get(xi);
//                    if (objForEachScenario[xi] > valuez + Constants.cutGenerationPrecision) {
                    if ((objForEachScenario[xi]-valuez)/objForEachScenario[xi]  > Constants.cutGenerationPrecision) {
                        expr0.addTerm(scenario.getProbability(), mipData.varz);


                        for (int i = 0; i < scenario.getCustomerDemand().length; i++) {
                            t += scenario.getProbability() * dualCostsMaps[xi].get("oneVisitPerCustomerAtMost_" + scenario.getIndex())[i];
                        }

                        for (int k = 0; k < instance.getWorkers().size(); k++) {
                            t += scenario.getProbability() * scenario.getWorkerCapacity()[k] * dualCostsMaps[xi].get("oneRoutePerWorkerAtMost_" + scenario.getIndex())[k];
                        }

                        t += scenario.getProbability() * instance.getUnservedPenalty() * scenario.getDemandTotal();
                        for (int s = 0; s < instance.getStationCandidates().size(); s++) {
                            expr0.addTerm(-scenario.getProbability() * dualCostsMaps[xi].get("stationCapConstraint_" + scenario.getIndex())[s], mipData.varsCapacity[s]);
                        }


                    }
                }
                IloRange iloRange0 = mipData.cplex.ge(expr0, t, "CVARCut" + optimalityCuts.size());
                inequalities.add(iloRange0);
                optimalityCuts.add(iloRange0);
                System.out.println("add one CVaR cut!! total number of cuts" + optimalityCuts.size());
            }
            //optimality cut
//            if (q > valueQ + Constants.cutGenerationPrecision) {
            if ((q - valueQ)/q > Constants.cutGenerationPrecision) {
                System.out.println("q: " + q + "valueQ: " + valueQ);
                IloLinearNumExpr expr = mipData.cplex.linearNumExpr();
                expr.addTerm(-1, mipData.varQ);
                double dualSum = 0;
                for (int xi = 0; xi < instance.getScenarios().size(); xi++) {
                    Scenario scenario = instance.getScenarios().get(xi);
                    for (int i = 0; i < scenario.getCustomerDemand().length; i++) {
                        dualSum -= scenario.getProbability() * dualCostsMaps[xi].get("oneVisitPerCustomerAtMost_" + scenario.getIndex())[i];
                    }

                    for (int k = 0; k < instance.getWorkers().size(); k++) {
                        dualSum -= scenario.getProbability() * scenario.getWorkerCapacity()[k] * dualCostsMaps[xi].get("oneRoutePerWorkerAtMost_" + scenario.getIndex())[k];
                    }
                    for (int s = 0; s < instance.getStationCandidates().size(); s++) {
                        expr.addTerm(scenario.getProbability() * dualCostsMaps[xi].get("stationCapConstraint_" + scenario.getIndex())[s], mipData.varsCapacity[s]);
                    }
                }
                dualSum -= instance.getTotalPenalty();
                IloRange iloRange = mipData.cplex.le(expr, dualSum, "optimalityCut" + optimalityCuts.size());
                inequalities.add(iloRange);
                optimalityCuts.add(iloRange);

                System.out.println("add one optimality cut!! total number of cuts" + optimalityCuts.size());

            }

            if(inequalities.size()==0) {
                System.out.println("No cut is found!");
            }
        }


        return inequalities;
    }
}
