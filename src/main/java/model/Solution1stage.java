package model;

import lombok.Data;

import java.util.Arrays;
import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 6/19/22 6:35 PM
 */
@Data
public class Solution1stage {
    int[][] stationLocation;
    double obj;

    public String toString(){
        StringBuffer stringBuffer=new StringBuffer();
        stringBuffer.append("objective of the first stage: "+obj+"\n");
        stringBuffer.append("the location and capacity of the stations +\n");
        for(int s=0;s<stationLocation.length;s++){
            stringBuffer.append(Arrays.toString(stationLocation[s])+"\n");
        }
        return stringBuffer.toString();
    }
}