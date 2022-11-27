package util;

import benders.cg.column.AssignmentColumn_true;
import model.Worker;

import java.util.Map;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 2021/10/29 13:08
 */
public class GlobalVariable {
    public static long MAXBRANCHBOUNDTIME = 7200; //Max branch and bound time in (s), default: 600
    public static int stationNum=0;
    public static int typeNum=0;
    public static int daysNum=1;
    public static Map<Worker,Set<AssignmentColumn_true>> columnsMap;
    public static Set<AssignmentColumn_true> columns;

    public static boolean ENUMERATE=true;
    public static double obj=Double.MAX_VALUE;
    public static double alpha=0.7;
    public static double lambda=0;

    public static long timeStart;
    public static long timeLimit=7200000;


}
