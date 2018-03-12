package ed.edpapp;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

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

public class Location extends MainActivity {
    private final String TAG = "mapActivity";
    private static String ENDPOINT;
    private RequestQueue requestQueue;
    private TextView direction;
    ArrayList<stepItem> stepLocs;

    protected void pollLocation(String latitude, String longitude, String currentLat, String currentLong, Context context) {
        Log.i(TAG, latitude);
        Log.i(TAG, longitude);
        ENDPOINT ="https://maps.googleapis.com/maps/api/directions/json?origin="+ currentLat+ "," +currentLong+"&destination="+latitude+","+longitude+"&mode=walking&key=AIzaSyDIWmHtaqq6ByMYEQoJsfJIgAnEXZFfHEA";
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
        Navigator navigator = new Navigator();
        navigator.setBearings(stepLocs);
        putstepson(stepLocs);
    }

    public void putstepson(ArrayList<stepItem> steplocs){
        for(int i =0; i < steplocs.size()-1; i++){
            if(steplocs.get(i).getleft()){
                Log.i(TAG, "left");
            }else{
                Log.i(TAG, "right");
            }

        }
    }



    void updateLatLong(android.location.Location location){
        stepItem currentStep = getStep();
        if(currentStep == null){
            finished();
        }
        Navigator navigator = new Navigator();
        if(navigator.navigate(location.getLatitude(), location.getLongitude(), currentStep)){
            boolean left = nextStep();
            buzz(left);
        }

    }

    stepItem getStep(){
        for(stepItem item : stepLocs){
            if(item.getcheckActive()){
                return item;
            }
        }
        return null;
    }

    boolean nextStep(){//moves the active node onto the next one in the list. Returns whether to turn left or right.
        boolean set = false;
        int i = 0;
        boolean left = false;
        while(set == false){
            if(stepLocs.get(i).getcheckActive() == true){
                set = true;
                stepLocs.get(i).setcheckActive(false);
                left = stepLocs.get(i).getleft();
            }
            i++;
        }
        stepLocs.get(i).setcheckActive(true);
        return left;
    }

    void finished(){
        Log.i(TAG, "You have reached your destination.");
    }


}
