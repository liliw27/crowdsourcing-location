package FWPH.model;

import lombok.Data;

/**
 * @author Wang Li
 * @description
 * @date 7/9/22 9:22 AM
 */
@Data
public class SolutionValue {
    private double[][] x;//Location
//    private double[][] y1;//allocationIK
//    private double[][]y2;//allocationSK
//    private double[] y3;//unServed
    private double obj;
    private double objSecond;
    private double objFirst;
}
