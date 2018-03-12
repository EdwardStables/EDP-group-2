package ed.edpapp;

import java.util.ArrayList;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;

/**
 * Created by Edward Stables on 12/03/2018.
 */

public class Navigator {
    public void setBearings(ArrayList<stepItem> steps){

        for(int i = 0; i < steps.size()-1;i++){
            float lat1 = Float.parseFloat(steps.get(i).getLat());
            float lat2 = Float.parseFloat(steps.get(i+1).getLat());
            float long1 = Float.parseFloat(steps.get(i).getLong());
            float long2 = Float.parseFloat(steps.get(i+1).getLong());
            double theta = atan2(sin(long2-long1)*cos(lat2), cos(lat1)*sin(lat2)-sin(lat1)*cos(lat2)*cos(long2-long1));
            steps.get(i).setHeading(toDegrees(theta));
        }

        setLR(steps);

    }

    public void setLR(ArrayList<stepItem> steps){

        for(int i = 0; i < steps.size()-1; i++){
            double theta = steps.get(i).getHeading();
            double alpha = steps.get(i+1).getHeading();
            double boundary = normBearing(theta - 180);

            if(theta <= 180){
                if(alpha <= boundary && alpha > theta){
                    steps.get(i).setleft(false);
                }else{
                    steps.get(i).setleft(true);
                }
            }else{
                if(alpha <= theta && alpha > boundary){
                    steps.get(i).setleft(true);
                }else{
                    steps.get(i).setleft(false);
                }
            }
        }
    }

    public double normBearing(double bearing){

        while (bearing > 360 || bearing < 0){
            if(bearing > 360){
                bearing = bearing - 360;
            }else{
                bearing = bearing + 360;
            }
        }

        return bearing;
    }

    public boolean navigate(Double currentLat, Double currentLong, stepItem nextStep){
        double doubleLat = Double.parseDouble(nextStep.getLat());
        double doubleLong = Double.parseDouble(nextStep.getLong());
        if((currentLat < doubleLat * 1.0001 && currentLat > doubleLat * 0.9999) && (currentLong < doubleLong * 1.0001 && currentLong > doubleLong * 0.9999)){
            return true;
        }else{
            return false;
        }
    }
}
