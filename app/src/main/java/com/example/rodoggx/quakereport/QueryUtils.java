package com.example.rodoggx.quakereport;

/**
 *
 */

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods related to requesting and receiving earthquake data from USGS.
 */
public final class QueryUtils {

    private static final String LOG_TAG = QueryUtils.class.getSimpleName();

    /**
     * Create a private constructor because no one should ever create a {@link QueryUtils} object.
     * This class is only meant to hold static variables and methods, which can be accessed
     * directly from the class name QueryUtils (and an object instance of QueryUtils is not needed).
     */
    private QueryUtils() {
    }

    public static List<Earthquake> fetchEarthQuakeData(String requestUrl) {
        URL url = createUrl(requestUrl);
        String jsonResponse = null;
        try {
                jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "fetchEarthQuakeData: ", e);
        }
        List<Earthquake> earthquakes = extractFeatureFromJson(jsonResponse);
        return earthquakes;
    }

    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "makeHttpRequest: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "makeHttpRequest: ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "makeHttpRequest: ", e);
                }
            }
        }
        return jsonResponse;
    }

    private static String readFromStream(InputStream stream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (stream != null) {
            InputStreamReader streamReader = new InputStreamReader(stream,  Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(streamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    private static URL createUrl(String stringUrl) {
    URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "createUrl: ", e);
        }
        return url;
    }

    /**
     * Return a list of {@link Earthquake} objects that has been built up from
     * parsing the given JSON response.
     */
    public static List<Earthquake> extractFeatureFromJson(String earthquakeJSON) {
        //if JSON string is empty
        if (TextUtils.isEmpty(earthquakeJSON)) {
            return null;
        }

        // Create an empty ArrayList that we can start adding earthquakes to
        List<Earthquake> earthquakes = new ArrayList<>();

        // Try to parse the JSON response string. If there's a problem with the way the JSON
        // is formatted, a JSONException exception object will be thrown.
        // Catch the exception so the app doesn't crash, and print the error message to the logs.
        try {

            //Create a JSONObject from the JSON response string
            JSONObject baseJsonResponse = new JSONObject(earthquakeJSON);

            //Extract the JSONArray associated with the key called "features" which represents a list of features/earthquakes).
            JSONArray earthquakeArray = baseJsonResponse.getJSONArray("features");

            //for each earthquake in the JSONArray, crate Earthquake object
            for (int i = 0; i < earthquakeArray.length(); i++) {
                //get a single earthquake at position i within the list of earthquakes
                JSONObject currentEarthquake = earthquakeArray.getJSONObject(i);

                //Extract the JSONArray associated with the key called "properties" which represents a list of properties).
                JSONObject properties = currentEarthquake.getJSONObject("properties");

                //extract the values for each key
                double magnitude = properties.getDouble("mag");
                String location = properties.getString("place");
                String url = properties.getString("url");
                long time = properties.getLong("time");

                Earthquake earthquake = new Earthquake(magnitude, location, time, url);
                earthquakes.add(earthquake);
            }

        } catch (JSONException e) {
            // If an error is thrown when executing any of the above statements in the "try" block,
            // catch the exception here, so the app doesn't crash. Print a log message
            // with the message from the exception.
            Log.e("QueryUtils", "Problem parsing the earthquake JSON results", e);
        }

        // Return the list of earthquakes
        return earthquakes;
    }
}