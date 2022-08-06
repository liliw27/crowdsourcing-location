package FWPH.model;

import lombok.Data;
import model.Instance;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 7/9/22 10:57 AM
 */
@Data
public class FWPHInput {
    List<Instance> instanceList ;
    double[] probability;
    double rou=1000;
    double alpha=1;
    double epsilon=0.01;
    int kmax=100;
    int tmax=1;

}
