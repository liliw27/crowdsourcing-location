package FWPH.model;

import lombok.Data;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 7/9/22 10:40 AM
 */
@Data
public class FWPHSolution {
    private double[][][] lambda;
    private SolutionValue[] solutionValues;
    private double phi;//lower bound
    private List<SolutionValue>[] V;
    private double[][] Z;
}
