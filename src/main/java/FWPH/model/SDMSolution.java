package FWPH.model;

import lombok.Data;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 7/9/22 10:56 AM
 */
@Data
public class SDMSolution {
    private SolutionValue XY_s;
    private List<SolutionValue> Vs;
    private double phi_s;
}
