package thedankdevs.tcss450.uw.edu.tddevschat.HomeActivity.Weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import thedankdevs.tcss450.uw.edu.tddevschat.HomeActivity.HomeActivity;
import thedankdevs.tcss450.uw.edu.tddevschat.HomeActivity.SettingsFragment;
import thedankdevs.tcss450.uw.edu.tddevschat.HomeActivity.Utility.SettingsNode;
import thedankdevs.tcss450.uw.edu.tddevschat.R;
import thedankdevs.tcss450.uw.edu.tddevschat.WaitFragment;
import thedankdevs.tcss450.uw.edu.tddevschat.utils.SendPostAsyncTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static thedankdevs.tcss450.uw.edu.tddevschat.HomeActivity.SettingsFragment.DETERMINANT_PREF;
import static thedankdevs.tcss450.uw.edu.tddevschat.HomeActivity.SettingsFragment.METRIC_PREF;
import static thedankdevs.tcss450.uw.edu.tddevschat.HomeActivity.Utility.LocationNode.*;
import static thedankdevs.tcss450.uw.edu.tddevschat.HomeActivity.Utility.SettingsNode.CELSIUS;
import static thedankdevs.tcss450.uw.edu.tddevschat.HomeActivity.Utility.SettingsNode.Weather_Preference;

/**
 * A weather date fragment contains much of the client functionality corresponding to weather.
 * Interacts with a list fragment containing all of the weather data
 * @author Daylen Nguyen
 * believe it WEATHER or not! HAHAHAHAHAH!
 * <p>
 * note: Shows not 1 but 10 days worth of weather forcast! WAoW!
 */
public class WeatherDateFragment extends Fragment {
    /*  UNITS OF MEASUREMENT  */
    public static final String            METRIC     = "M";       // - [DEFAULT] Metric (Celcius, m/s, mm)
    public static final String            SCIENTIFIC = "S";       // - Scientific (Kelvin, m/s, mm)
    public static final String            MURICA     = "I";       // - Fahrenheit (F, mph, in)
    static final        String            TAG        = "WEATHER"; // for logcat
    /*  Response from server will push content to list  */
    private             List<WeatherDate> currentWeatherData;

    /*
        Weather Variables; data output is dependant on valid variables
        NOTE: State is the state abbreviation.
    */

    private String mState, mCity, mZip;
    private String                            mLon;
    private String                            mLat;
    private OnListFragmentInteractionListener mListener;
    private RecyclerView                      myRV;

    /*pulled from shared preferences*/
    private int    current_LocationDeterminant;
    private String current_WeatherMetric;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WeatherDateFragment() {
    }

    /**
     * Retrieves the user's location data based on preference settings
     */
    private void getLocationDataBasedOnPreference( Bundle bundle ) {
        SharedPreferences sp = Objects.requireNonNull( this.getContext() ).getSharedPreferences( Weather_Preference, 0 );
//        SharedPreferences.Editor e = sp.edit();
        switch ( current_LocationDeterminant ) {
            case SettingsNode.CITY_STATE:
                mCity = sp.getString( CITY_KEY, "TACOMA" );
                mState = sp.getString( STATE_KEY, "WA" );
                Log.w( "DAYLEN LOCATION BASED ON PREFERENCE", mCity + mState );
                break;
            case SettingsNode.GPS_DATA:
                mLat = bundle.getString( LATITUDE_KEY, "47.24515" );
                mLon = bundle.getString( LONGITUDE_KEY, "-122.437456" );
                break;
            case SettingsNode.SELECT_FROM_MAP:
                mLat = sp.getString( MAP_LAT_KEY, "47.24515");
                mLon = sp.getString( MAP_LON_KEY, "-122.437456" );
                break;
            case SettingsNode.POSTAL_CODE:
                mZip = sp.getString( ZIP_KEY, "98422" );
                break;
        }
    }

    /**
     * retrieves the user's weather metric preference
     */
    public void getAndUpdateMetricPreferences() {
        SharedPreferences settings = Objects.requireNonNull( getContext() ).getApplicationContext().getSharedPreferences( Weather_Preference, Context.MODE_PRIVATE );
        current_WeatherMetric = settings.getString( METRIC_PREF, "C" );
    }

    /**
     * updates the user's determinant location preference
     */
    public void getAndUpdateLocationDeterminantPref() {
        SharedPreferences settings = Objects.requireNonNull( getContext() ).getSharedPreferences( Weather_Preference, Context.MODE_PRIVATE );
        current_LocationDeterminant = settings.getInt( DETERMINANT_PREF, SettingsNode.GPS_DATA );
    }

    /*Once the weather request has been fulfilled, we can remove the load/wait fragment*/
    private void PreWeatherRequest() {
        mListener.onWaitFragmentInteractionShow();
        Log.i( "DAYLEN_WEATH_DATE_FRAG", "WEATHER METRIC = ".concat( String.valueOf( current_WeatherMetric ) ) );
        Log.i( "DAYLEN_WEATH_DATE_FRAG", "LOCATIONDETERMINANT = ".concat( String.valueOf( current_LocationDeterminant ) ) );

    }

    /**
     * Called once the weather async task is completed
     *
     * @param result the response from the server; in regards to our request
     */
    private void PostWeatherRequest( final String result ) {
        Log.i( "DAYLEN_WEATH_DATE_FRAG", "LOCATIONDETERMINANT = ".concat( String.valueOf( current_LocationDeterminant ) ) );
        Log.i( "DAYLEN_WEATH_DATE_FRAG", "WEATHER METRIC = ".concat( String.valueOf( current_WeatherMetric ) ) );
        Log.i( "json return", result );

        try {
            // This is the result from the web service
            currentWeatherData = new ArrayList<>();
            JSONObject res = new JSONObject( result );
            Log.i( "Daylen", String.valueOf( res ) );

            // retrieve the city and state of the current user from the
            // latitude and longitude
            mState = res.getString( "state_code" );
            mCity = res.getString( "city_name" );

            Log.i( "DAYLEN_WEATH_DATE_FRAG", mCity + mState );


            // response will contain a json array containing 16 day forecast
            // each index, 1 day
            JSONArray data = res.getJSONArray( "data" );

            // pieces of the URI
            String icoextension = getResources().getString( R.string.ep_weather_icon_file_ext );
            String icolocation  = getResources().getString( R.string.ep_weather_icon );

            for ( int i = 0; i < 10; i++ ) {
                // data will be in the form of a json object
                JSONObject currentDay = ( JSONObject ) data.get( i );

                // extract json values from response
                String date = currentDay.getString( "datetime" );
                double avg  = currentDay.getDouble( "temp" );
                double min  = currentDay.getDouble( "min_temp" );
                double max  = currentDay.getDouble( "max_temp" );

                JSONObject weather = currentDay.getJSONObject( "weather" );
                String     desc    = weather.getString( "description" );

                // retrieve the icon name for the forcast
                String iconAlias = weather.getString( "icon" );

                // join the pieces of the URI
                String iconURI = String.format( "%s%s%s", icolocation, iconAlias, icoextension );

                // encapsulate the values for retrieval by the recycler view
                WeatherDate Day = new WeatherDate( i, current_WeatherMetric, iconURI, i + " Day(s) away: " + date, min, max, avg, desc );
                if ( i == 0 ) {
                    Day = new WeatherDate( i, current_WeatherMetric, iconURI, "Today: " + date, min, max, avg, desc );
                }

                /*Add the current weather to the field [list]*/
                currentWeatherData.add( Day );
                Log.w( TAG, "\n[ i = " + i + " ]\n\tDate: " + date + "\n\tAvg:" + avg + "\n\tMin:" + min + "\n\tMax" + max + "\n\tdesc:" + desc );
            }
        } catch ( JSONException e ) {
            Log.d( TAG, e.toString() );
            e.printStackTrace();
        } finally {
            //display the location correlating to the data we are displaying
            ( ( TextView ) Objects.requireNonNull( getView() ).findViewById( R.id.city_head_textview ) ).setText( mCity );
            ( ( TextView ) getView().findViewById( R.id.state_head_textview ) ).setText( mState );
            /*After retrieving the data, make it visible and remove the wait-fragment*/
            myRV.setAdapter( new MyWeatherDateRecyclerViewAdapter( currentWeatherData, mListener ) );
            mListener.onWaitFragmentInteractionHide();
        }

    }

    /*Generates a URI string. Changes the endpoint depending on location determinant*/
    private String constructRequestLocation() {
        Uri.Builder uri_builder = new Uri.Builder()
                .scheme( "https" )
                .appendPath( getString( R.string.base_url ) )
                .appendPath( getString( R.string.ep_weather ) );
        switch ( current_LocationDeterminant ) {
            case SettingsNode.CITY_STATE:
                uri_builder.appendPath( getString( R.string.ep_weather_citystate ) );
                break;
            case SettingsNode.GPS_DATA:
                uri_builder.appendPath( getString( R.string.ep_weather_bycoordinate ) );
                break;
            case SettingsNode.SELECT_FROM_MAP:
                uri_builder.appendPath( getString( R.string.ep_weather_bycoordinate ) );
                break;
            case SettingsNode.POSTAL_CODE:
                uri_builder.appendPath( getString( R.string.ep_weather_postalcode ) );
                break;
        }
        return uri_builder.build().toString();
    }

    private void getMetricReqString( JSONObject j ) {
        try {
            switch ( current_WeatherMetric ) {
                case "F":
                    j.put( "units", "I" );
                    break;
                case "K":
                    j.put( "units", "S" );
                    break;
                case "C": /*default weather api field*/
                    j.put( "units", "M" );
                    break;
            }
        } catch ( JSONException e ) {
            e.printStackTrace();
        }
    }

    /*returns a json object, dependent on the location determinant*/
    private JSONObject constructRequestJSON() {
        JSONObject request = new JSONObject();
        try {
            switch ( current_LocationDeterminant ) {
                case SettingsNode.CITY_STATE:
                    request.put( getString( R.string.weather_lcase_city ), mCity );
                    request.put( getString( R.string.weather_lcase_state ), mState );
                    break;
                case SettingsNode.GPS_DATA:
                    request.put( getString( R.string.weather_lon_json ), mLon );
                    request.put( getString( R.string.weather_lat_json ), mLat );
                    break;
                case SettingsNode.SELECT_FROM_MAP:
                    request.put( getString( R.string.weather_lon_json ), mLon );
                    request.put( getString( R.string.weather_lat_json ), mLat );
                    break;
                case SettingsNode.POSTAL_CODE:
                    request.put( getString( R.string.weather_json_postal ), mZip );
                    break;
            }
            /*DEFAULT UNIT IS CELSIUS*/
            if ( !current_WeatherMetric.equals( SettingsNode.CELSIUS ) ) {
                switch ( current_WeatherMetric ) {
                    case SettingsNode.FAHRENHEIT:
                        request.put( "units", MURICA );
                        break;
                    case SettingsNode.KELVIN:
                        request.put( "units", SCIENTIFIC );
                        break;
                }
            } else {
                request.put("units", CELSIUS);
            }
        } catch ( Exception e ) {
            Log.e( "WEATHER", String.valueOf( e ) );
        }
        getMetricReqString( request );
        return request;
    }

    /**
     * Creates a weather request
     */
    private void Coordinates_getCurrentWeatherData() {
        String     mSendUrl = constructRequestLocation();
        JSONObject request  = constructRequestJSON();
        Log.d( TAG, mSendUrl );
        if ( request != null ) {
            Log.d( TAG, request.toString() );
        }
        new SendPostAsyncTask.Builder( mSendUrl, request )
                .onPreExecute( this::PreWeatherRequest )
                .onPostExecute( this::PostWeatherRequest )
                .onCancelled( error -> Log.e( TAG, error ) )
                .build()
                .execute();
    }

    /*
     * Ensure that the context extends our listener
     */
    @Override
    public void onAttach( Context context ) {
        super.onAttach( context );
        if ( context instanceof OnListFragmentInteractionListener ) {
            mListener = ( OnListFragmentInteractionListener ) context;

            /*initialize the weather data before display of the fragment*/
            getAndUpdateLocationDeterminantPref();
            getAndUpdateMetricPreferences();
            // Send request for weather data
            if ( getArguments() != null ) {
                Bundle bundle = getArguments();
                getLocationDataBasedOnPreference( bundle );
                Coordinates_getCurrentWeatherData();
            }

        } else {
            throw new RuntimeException( context.toString()
                    + " must implement OnListFragmentInteractionListener" );
        }
    }

    /**
     * When weather is opened on onCreate will make a post request to the server for current weather
     */
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

    }

    /*Executed after on create, we set the adapter if the current weather data has already been retrieved*/
    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.fragment_weatherdate_list, container, false );
        myRV = view.findViewById( R.id.weatherlist ); //retrieve the list contained within the fragment
        // Set the adapter and layout manager
        if ( myRV != null ) {
            Context context = myRV.getContext();
            myRV.setLayoutManager( new LinearLayoutManager( context ) );
            if ( currentWeatherData != null ) {
                myRV.setAdapter( new MyWeatherDateRecyclerViewAdapter( currentWeatherData, mListener ) );
            }
        }
        View mFAB = view.findViewById( R.id.weather_fab );
        mFAB.setOnClickListener( ( View v ) -> {
            if ( getFragmentManager() != null ) {
                ( ( HomeActivity ) Objects.requireNonNull( this.getActivity() ) ).loadFragment( new SettingsFragment() );
            }
        } );
        return view;
    }

    /**
     * Method called at the end of a fragments life. Rest in peace mr.weather date fragment
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener extends WaitFragment.OnFragmentInteractionListener {
        void onWeatherListItemFragmentInteraction( WeatherDate item );
    }
}
