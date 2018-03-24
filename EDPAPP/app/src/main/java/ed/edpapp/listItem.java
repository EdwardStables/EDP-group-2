package ed.edpapp;

/**
 * Created by Edward Stables on 12/03/2018.
 */
//A simple class containing the destination locations and the required get methods
public class listItem {
    public String Location;
    public String LocationLat;
    public String LocationLong;

    public listItem(String Location, String LocationLat, String LocationLong){
        this.Location = Location;
        this.LocationLat = LocationLat;
        this.LocationLong = LocationLong;

    }
    public String getLat(){ return LocationLat; }
    public String getLong(){ return LocationLong; }
    public String getLoc(){ return Location; }
}
