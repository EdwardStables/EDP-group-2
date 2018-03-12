package ed.edpapp;

/**
 * Created by Edward Stables on 12/03/2018.
 */

public class stepItem {
    public String LocationLat;
    public String LocationLong;
    public double heading;
    public boolean left;
    public boolean checkActive;

    public stepItem(String LocationLat, String LocationLong){
        this.LocationLat = LocationLat;
        this.LocationLong = LocationLong;
    }

    public String getLat(){ return LocationLat; }

    public String getLong(){
        return LocationLong;
    }

    public double getHeading() {return heading;}

    public void setHeading(double heading) {this.heading = heading;}

    public boolean getleft() { return left; }

    public void setleft(boolean left) { this.left = left; }

    public boolean getcheckActive() { return checkActive; }

    public void setcheckActive(boolean checkActive) { this.checkActive = checkActive; }
}