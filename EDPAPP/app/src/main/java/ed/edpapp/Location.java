package ed.edpapp;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;

/**
 * Created by Edward Stables on 12/03/2018.
 */

public class Location {
    private final String TAG = "mapActivity";
    private static String ENDPOINT;
    private RequestQueue requestQueue;
    ArrayList<stepItem> stepLocs;

    Navigator navigator = new Navigator();

    protected void pollLocation(String latitude, String longitude, String currentLat,
                                String currentLong, Context context) {
        Log.i(TAG, latitude);
        Log.i(TAG, longitude);
        ENDPOINT ="https://maps.googleapis.com/maps/api/directions/json?origin="+
                currentLat+ "," +currentLong+"&destination="+latitude+","+longitude+
                "&mode=walking&key=AIzaSyDIWmHtaqq6ByMYEQoJsfJIgAnEXZFfHEA";
        Log.i(TAG, ENDPOINT);
        requestQueue = Volley.newRequestQueue(context);

        fetchPosts();
        Log.i(TAG, "finished");
    }

    private void fetchPosts(){
        StringRequest request = new StringRequest(Request.Method.GET, ENDPOINT, onPostsLoaded, onPostsError);

        requestQueue.add(request);
    }

    private final Response.Listener<String> onPostsLoaded = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            Log.i("PostActivity", "recieved response");
            getSteps(response);
        }
    };

    private final Response.ErrorListener onPostsError = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("PostActivity", error.toString());
        }
    };


    void getSteps(String Response){
        //this block gets an array of steps from the google response
        JsonParser parser = new JsonParser();
        JsonObject obj = parser.parse(Response).getAsJsonObject();
        JsonArray routes = obj.get("routes").getAsJsonArray();
        JsonObject subRoute = routes.get(0).getAsJsonObject();
        JsonArray legs = subRoute.get("legs").getAsJsonArray();
        JsonObject subLeg = legs.get(0).getAsJsonObject();
        JsonArray steps = subLeg.get("steps").getAsJsonArray();

        stepLocs = new ArrayList<>();

        JsonObject first_step = steps.get(0).getAsJsonObject();
        JsonObject location = first_step.get("start_location").getAsJsonObject();
        String tempLat = location.get("lat").getAsString();
        String tempLong = location.get("lng").getAsString();
        stepItem tempLoc = new stepItem(tempLat, tempLong);
        stepLocs.add(tempLoc);

        location = first_step.get("end_location").getAsJsonObject();
        tempLat = location.get("lat").getAsString();
        tempLong = location.get("lng").getAsString();
        tempLoc = new stepItem(tempLat, tempLong);
        stepLocs.add(tempLoc);
        stepLocs.get(0).setcheckActive(true);

        for(int i = 1; i < steps.size(); i++){
            JsonObject sub_step = steps.get(i).getAsJsonObject();
            JsonObject end_location = sub_step.get("end_location").getAsJsonObject();
            tempLat = end_location.get("lat").getAsString();
            tempLong = end_location.get("lng").getAsString();

            tempLoc = new stepItem(tempLat, tempLong);
            stepLocs.add(tempLoc);
        }
        stepLocs.get(0).setcheckActive(true);
        navigator.setBearings(stepLocs);
        putstepson(stepLocs);
    }


    public void putstepson(ArrayList<stepItem> steplocs){
        String displayString = "";
        for(int i =0; i < steplocs.size()-1; i++) {
            if (steplocs.get(i).getleft()) {
                displayString = displayString + "Left   " + steplocs.get(i).getLat() + ", " + steplocs.get(i).getLong() + "\n";
            } else {
                displayString = displayString + "Right   " + steplocs.get(i).getLat() + ", " + steplocs.get(i).getLong() + "\n";
            }
        }
        Log.i(TAG, displayString);
    }
    public boolean goLeft(){
        return lastLocation().getleft();
    }

    public boolean isAtLocation(android.location.Location location){
        if(navigator.navigate(location.getLatitude(), location.getLongitude(), nextLocation())){
            stepStep();
            return true;
        }
        return false;
    }

    public void stepStep(){
        for(int i = 0; i < stepLocs.size()-1; i++){
            if(stepLocs.get(i).checkActive){
                stepLocs.get(i).setcheckActive(false);
                stepLocs.get(i+1).setcheckActive(true);
                break;
            }
        }
    }
    public stepItem lastLocation(){
        stepItem tempItem = stepLocs.get(0);
        for(int i = 1; i < stepLocs.size(); i++){
            if(stepLocs.get(i).checkActive){
                break;
            }else{
                tempItem = stepLocs.get(i);
            }
        }
        return tempItem;
    }

    public stepItem nextLocation(){
        stepItem tempItem = null;
        if(stepLocs != null){
            for(int i = 0; i < stepLocs.size(); i++){
                if(stepLocs.get(i).checkActive){
                    tempItem = stepLocs.get(i);
                    break;
                }
            }
        }

        return tempItem;
    }



}
