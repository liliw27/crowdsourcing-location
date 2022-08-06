package model;

import lombok.Data;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 6/19/22 6:30 PM
 */
@Data
public class Solution {
    private double obj;
    private double expectedValue;
    private Solution1stage solution1stage;
    private List<Solution2Stage> solution2Stages;

    public String toString(){
        StringBuffer stringBuffer=new StringBuffer();
        stringBuffer.append("the objective: "+obj+"\n");
        stringBuffer.append("the expected value: "+expectedValue+"\n");
        if(solution1stage!=null){
            stringBuffer.append(solution1stage);
        }
        for(Solution2Stage solution2Stage:solution2Stages){
            stringBuffer.append(solution2Stage);
        }
        return stringBuffer.toString();
    }
}
