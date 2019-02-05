/*****************************************************************************
 * Project:     Demo Client App
 * File:        RequestShape.java
 * Original:    Created on 1 June 2016 by matthewb
 * Copyright(c) 2002 - 2016 by CriticalBlue Ltd.
 ****************************************************************************/
package com.criticalblue.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import android.view.View;
import android.widget.TextView;

import com.criticalblue.attestationlibrary.ApproovAttestation;
import com.criticalblue.attestationlibrary.TokenInterface.ApproovResults;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.tls.OkHostnameVerifier;

/**
 * Activity used to retrieve the shape from the demo server using the
 * OKHttp Library over a pinned connection.
 * <p>
 * This Activity calls the Approov attestation library to retrieve a token which is
 * then used to authenticate access to the demo server API
 */
public class RequestShape extends Activity {

    // Local customized HttpClient
    OkHttpClient httpClient;

    // Local customized HttpClient using interceptor
    OkHttpClient interceptorClient;

    // The view displayed by this activity. Shows simple text to feed information to user
    TextView textView;

    // Domain hostname
    static final String DEMO_SERVER_HOSTNAME = "demo-server.approovr.io";

    // Log tag for searching in logcat
    static final String TAG = "RequestShape";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.request_text);
        Shapes shapesApp = (Shapes) getApplicationContext();

        // Create a custom HostnameVerifier which supports DynamicPinning
        // This uses the current HostnameVerifier from OkHttpClient
        DynamicPinningHostnameVerifier pinningHostnameVerifier
                = new DynamicPinningHostnameVerifier(OkHostnameVerifier.INSTANCE);

        // Build a new instance of the okHttpClient for this requester to use.
        httpClient = shapesApp.getHttpClient().newBuilder()
                .hostnameVerifier(pinningHostnameVerifier)
                .build();

        interceptorClient = shapesApp.getHttpClient().newBuilder()
                .hostnameVerifier(pinningHostnameVerifier)
                .addNetworkInterceptor(new ApproovTokenInterceptor())
                .build();

    }

    /**
     * defaultRequestShape() is called from a button press in the activity. It calls an api to
     * retrieve a random shape from the server using a HttpsURLConnection
     *
     * @param view the view that triggered the request
     */
    public void defaultRequestShape(View view) {
        Log.i(TAG, "Request Button Pressed. Fetching shape with HttpsURLConnection");

        // Run our HTTP request in a background thread to avoid blocking the UI thread
        AsyncTask.execute(new Runnable() {

            @Override
            public void run() {
                
                String shape = "none";
                // We may need to retry if there is a
                // pinning failure first time through
                boolean retry = false;
                while (true) {
                    // Fetch an Approov Token using the SDK
                    ApproovResults approovResults = ApproovAttestation.shared().fetchApproovTokenAndWait(DEMO_SERVER_HOSTNAME);

                    // Always check the return status of the fetch call.
                    String token;
                    if (approovResults.getResult() == ApproovAttestation.AttestationResult.SUCCESS) {
                        Log.i(TAG, "Received a token from the Approov SDK.");
                        token = approovResults.getToken();
                        Log.i(TAG, token);

                    } else {
                        // A fail here means that the SDK could not reach the Approov servers
                        // before timing out. Set the token field to a known value to communicate
                        // this state (rather than leaving empty or excluding from the header)
                        Log.w(TAG, "Approov SDK token fetch failed");
                        token = "NOTOKEN";
                    }

                    HttpsURLConnection connection = null;
                    try {
                        // Create a request to send to the /shapes endpoint on the demo server
                        URL aUrl = new URL("https://" + DEMO_SERVER_HOSTNAME + "/shapes");

                        // Get our HTTPS connection object
                        connection = (HttpsURLConnection) aUrl.openConnection();
                        // Set this request as a GET
                        connection.setRequestMethod("GET");
                        // Set a timeout for the request
                        connection.setConnectTimeout(500);

                        // Create a hostname verifier using our dynamic pinning approach
                        DynamicPinningHostnameVerifier verifier
                                = new DynamicPinningHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
                        connection.setHostnameVerifier(verifier);

                        connection.addRequestProperty("Approov-Token", token);
                        connection.connect();

                        // Contact the server to get the shape
                        int responseCode = connection.getResponseCode();
                        if (responseCode == 200) {
                            // Retrieve the shape data from the HTTP response
                            Log.i(TAG, "Received a response from the demo server. Sending an intent to draw the shape.");
                            // Retrieve the shape data from the HTTP response
                            shape = readHttpInputStreamToString(connection);
                        } else if (responseCode == 400) {
                            // Invalid token - Log the failure and return an error message
                            Log.e(TAG, "Error code on GET request: " + Integer.toString(responseCode) +
                                    ". Did you forget to register the app, or have you been tampering with it? ");
                            updateDisplayText(R.string.server_declined);
                            if(connection != null)
                                connection.disconnect();
                            return;
                        } else {
                            // Oops, we don't know what happened. Log the failure and return an error message
                            Log.e(TAG, "Error code on GET request: " + Integer.toString(responseCode));
                            updateDisplayText(R.string.unknown_server);
                            if(connection != null)
                                connection.disconnect();
                            return;
                        }

                        if(connection != null)
                            connection.disconnect();

                        // Success - break the retry loop
                        break;
                    } catch (SSLPeerUnverifiedException ex) {
                            // Pinning test failed.
                            // This happens if the certificate from the server
                            // does not match the one cached in the SDK
                            if (!retry) {
                                // This is our first attempt so
                                // Go around the retry loop including the token
                                // and certificate refetch.
                                Log.w(TAG, "Server Cert mismatch. Retrying");
                                retry = true;
                            } else {
                                // This was our second attempt so we cannot
                                // get pinning check to pass. Break without
                                // setting the result
                                Log.e(TAG, "Server Cert mismatch retry failed.");
                                break;
                            }
                    } catch(Exception e) {
                        Log.e(TAG, "Unhandled Exception.");
                        if(connection != null)
                            connection.disconnect();
                        break;
                    }
                }

                // Send an intent to start the the DisplayShape Activity and display
                // the shape retrieved from the demo server
                Intent intent = new Intent(getBaseContext(), DisplayShape.class);
                intent.putExtra("Shape", shape);
                startActivity(intent);

                // We are done with the activity
                finish();
            }
        });
    }

    /**
     * okHttpRequestShape() is called from a button press in the activity. It calls an api to
     * retrieve a random shape from the server using the OkHttp library.
     *
     * @param view the view that triggered the request
     */
    public void okHttpRequestShape(View view) {
        Log.i(TAG, "Request Button Pressed. Fetching shape with okHttp");

        // Run our HTTP request in a background thread to avoid blocking the UI thread
        AsyncTask.execute(new Runnable() {
            boolean timeOut = false;

            @Override
            public void run() {

                String shape = "none";
                // We may need to retry if there is a
                // pinning failure first time through
                boolean retry = false;
                while (true) {
                    // Fetch an Approov Token using the SDK
                    ApproovResults approovResults = ApproovAttestation.shared().fetchApproovTokenAndWait(DEMO_SERVER_HOSTNAME);

                    // Always check the return status of the fetch call.
                    String token;
                    if (approovResults.getResult() == ApproovAttestation.AttestationResult.SUCCESS) {
                        Log.i(TAG, "Received a token from the Approov SDK.");
                        token = approovResults.getToken();
                        Log.i(TAG, token);

                    } else {
                        // A fail here means that the SDK could not reach the Approov servers
                        // before timing out. Set the token field to a known value to communicate
                        // this state (rather than leaving empty or excluding from the header)
                        Log.w(TAG, "Approov SDK token fetch failed");
                        token = "NOTOKEN";
                    }

                    // Create a request to send to the /shapes endpoint on the demo server
                    Request request;
                    try {
                        request = new Request.Builder()
                                .url("https://" + DEMO_SERVER_HOSTNAME + "/shapes")
                                .addHeader("Approov-Token", token)
                                .get()
                                .build();
                    } catch (IllegalArgumentException ex) {
                        Log.e(TAG, ex.getMessage());
                        return;
                    }

                    // Contact the server to get the shape
                    try (Response response = httpClient.newCall(request).execute()) {

                        // Pop up a message in our UI while we load the new Activity
                        updateDisplayText(R.string.fetching);

                        if (response.isSuccessful()) {
                            // Retrieve the shape data from the HTTP response
                            Log.i(TAG, "Received a response from the demo server. Sending an intent to draw the shape.");
                            shape = response.body().string();
                        } else if (response.code() == 400) {
                            // Invalid token - Log the failure and return an error message
                            Log.e(TAG, "Error code on GET request: " + Integer.toString(response.code()) +
                                    ". Did you forget to register the app, or have you been tampering with it? ");

                            updateDisplayText(R.string.server_declined);
                            return;
                        } else {
                            // Oops, we don't know what happened. Log the failure and return an error message
                            Log.e(TAG, "Error code on GET request: " + Integer.toString(response.code()));

                            updateDisplayText(R.string.unknown_server);
                            return;
                        }
                        // Success - break the retry loop
                        break;

                    } catch (SSLPeerUnverifiedException ex) {
                        // Pinning test failed.
                        // This happens if the certificate from the server
                        // does not match the one cached in the SDK
                        if (!retry) {
                            // This is our first attempt so
                            // Go around the retry loop including the token
                            // and certificate refetch.
                            Log.w(TAG, "Server Cert mismatch. Retrying");
                            retry = true;
                        } else {
                            // This was our second attempt so we cannot
                            // get pinning check to pass. Break without
                            // setting the result
                            Log.e(TAG, "Server Cert mismatch retry failed.");
                            break;
                        }
                    } catch (IOException ex) {
                        // Log an error and go around the retry loop again
                        Log.e(TAG, ex.getMessage());
                    }
                }

                // Send an intent to start the the DisplayShape Activity and display
                // the shape retrieved from the demo server
                Intent intent = new Intent(getBaseContext(), DisplayShape.class);
                intent.putExtra("Shape", shape);
                startActivity(intent);

                // We are done with the activity
                finish();
            }
        });

    }

    /**
     * okHttpRequestShapeInterceptor() is called from a button press in the activity. It calls an api to
     * retrieve a random shape from the server using the OkHttp library.
     *
     * @param view the view that triggered the request
     */
    public void okHttpRequestShapeInterceptor(View view) {
        Log.i(TAG, "Request Button Pressed. Fetching shape with okHttp Interceptor ");

        // Run our HTTP request in a background thread to avoid blocking the UI thread
        AsyncTask.execute(new Runnable() {
            boolean timeOut = false;

            @Override
            public void run() {

                String shape = "none";
                // We may need to retry if there is a
                // pinning failure first time through
                boolean retry = false;
                while (true) {

                    // Create a request to send to the /shapes endpoint on the demo server
                    Request request;
                    try {
                        request = new Request.Builder()
                                .url("https://" + DEMO_SERVER_HOSTNAME + "/shapes")
                                .get()
                                .build();
                    } catch (IllegalArgumentException ex) {
                        Log.e(TAG, ex.getMessage());
                        return;
                    }

                    // Contact the server to get the shape
                    try (Response response = interceptorClient.newCall(request).execute()) {

                        // Pop up a message in our UI while we load the new Activity
                        updateDisplayText(R.string.fetching);

                        if (response.isSuccessful()) {
                            // Retrieve the shape data from the HTTP response
                            Log.i(TAG, "Received a response from the demo server. Sending an intent to draw the shape.");
                            shape = response.body().string();
                        } else if (response.code() == 400) {
                            // Ivalid token - Log the failure and return an error message
                            Log.e(TAG, "Error code on GET request: " + Integer.toString(response.code()) +
                                    ". Did you forget to register the app, or have you been tampering with it? ");
                            updateDisplayText(R.string.server_declined);
                            return;
                        } else {
                            // Oops, we don't know what happened. Log the failure and return an error message
                            Log.e(TAG, "Error code on GET request: " + Integer.toString(response.code()));
                            updateDisplayText(R.string.unknown_server);
                            return;
                        }
                        // Success - break the retry loop
                        break;

                    } catch (SSLPeerUnverifiedException ex) {
                        // Pinning test failed.
                        // This happens if the certificate from the server
                        // does not match the one cached in the SDK
                        if (!retry) {
                            // This is our first attempt so
                            // Go around the retry loop including the token
                            // and certificate refetch.
                            Log.w(TAG, "Server Cert mismatch. Retrying");
                            retry = true;
                        } else {
                            // This was our second attempt so we cannot
                            // get pinning check to pass. Break without
                            // setting the result
                            Log.e(TAG, "Server Cert mismatch retry failed.");
                            break;
                        }
                    } catch (IOException ex) {
                        // Log an error and go around the retry loop again
                        Log.e(TAG, ex.getMessage());
                    }
                }

                // Send an intent to start the the DisplayShape Activity and display
                // the shape retrieved from the demo server
                Intent intent = new Intent(getBaseContext(), DisplayShape.class);
                intent.putExtra("Shape", shape);
                startActivity(intent);

                // We are done with the activity
                finish();
            }
        });

    }


    /**
     * Method to update the displayed text in the UI thread for our Activity
     */
    private void updateDisplayText(final int textResource) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                textView.setText(textResource);
            }
        });
    }


    /**
     * Method to read a string from an HTTP request from https://gist.github.com/ssawchenko/9282300
     */
    private String readHttpInputStreamToString(HttpsURLConnection connection) {
        String result = null;
        StringBuilder sb = new StringBuilder();
        InputStream is = null;

        try {
            is = new BufferedInputStream(connection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
            }
            result = sb.toString();
        }
        catch (Exception e) {
            Log.i(TAG, "Error reading InputStream");
            result = null;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    Log.i(TAG, "Error closing InputStream");
                }
            }
        }

        return result;
    }
}
