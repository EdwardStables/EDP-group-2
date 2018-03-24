package ed.edpapp;

/**
 * Created by Edward Stables on 12/03/2018.
 */
//A class that contains all the information for each waypoint, and the get/set methods needed to access them

public class stepItem {
    public String LocationLat;//waypoint latitude
    public String LocationLong;//waypoint longitude
    public double heading;//bearing to the next waypoint
    public boolean left;//whether this waypoint requires a left or right turn
    public boolean checkActive;//whether this is the active waypoint

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