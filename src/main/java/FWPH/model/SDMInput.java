package FWPH.model;

import lombok.Data;
import model.Instance;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 7/9/22 10:58 AM
 */
@Data
public class SDMInput {
    private Instance instance;
    private SolutionValue solutionValue_kmiuns1;
    private double[][] Xs;
    private double[][] Z;
    private double[][] lambdas;
    List<SolutionValue> Vs;
    double rou;
    int tmax=1;
    double tao=0.01;
}
