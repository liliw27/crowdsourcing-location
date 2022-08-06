package util;

/**
 * @author Wang Li
 * @description
 * @date 8/6/21 11:37 AM
 */
public class Constants {
    public static double EPSILON = 0.000001;

    /**
     * Max runtime of CP model (in seconds);
     **/
    public static final int MAXCPTIME = 300;

    public static final boolean ENABLEPREPROC = true;

    public static final int MAXTHREADS = 8; //Number of threads used by MIP models and column generation

    public static final int columnNumMin = 500;

    public static final int columnNumIte = 500;

    public static final int columnNumMax = 4000;

    public static final double gapCG = -EPSILON;

    public static final double costMax = 100;

    public static final double punishMax = 0;

    public static final boolean isCGAllColumns=false;

    public static final int mipIterColumnNum=3000;

    public static final int mipIter=10;


    public static final int speed=50;//(km/h)

    public static final int deltaT=60;//min
    public static boolean cut=false;
    public static boolean ispredict=true;
    public static boolean partialColumn=false;

    /**
     * Branch-and-bound configuration
     */
    public static final int MAXBRANCHBOUNDTIME = 600; //Max branch and bound time in (s), default: 600
}
