package util;

/**
 * @author Wang Li
 * @description
 * @date 6/20/22 4:04 PM
 */
public class Util {
    public static int calTravelTime(int lat1, int lng1, int lat2, int lng2) {

        double distance = Math.sqrt((lat1 - lat2) * (lat1 - lat2) + (lng1 - lng2) * (lng1 - lng2));
//        int travelTime = (int) (distance * 20 / (Constants.speed * 1000) * 60);
        int travelTime = (int) distance ;
        if(travelTime==0){
            travelTime=1;
        }

        return travelTime;
    }
}
