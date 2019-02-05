/*****************************************************************************
 * Project:     Demo Client App
 * File:        Shapes.java
 * Original:    Created on 3 February 2017 by craigt
 * Copyright(c) 2016 - 2017 by CriticalBlue Ltd.
 ****************************************************************************/
package com.criticalblue.demo;

import android.app.Application;
import android.util.Log;

import com.criticalblue.attestationlibrary.ApproovAttestation;
import com.criticalblue.attestationlibrary.ApproovConfig;

import java.net.MalformedURLException;

import okhttp3.OkHttpClient;

/**
 * Top level application-derived class that holds the Approov Java library
 * Allows access to the ApproovAttestation object from anywhere in the app
 */
public class Shapes extends Application {

    // Shared Http Client object for the app.
    private OkHttpClient httpClient;

    // Log tag for searching in logcat
    private static final String TAG = "ShapesApp";

    @Override
    public void onCreate(){
        super.onCreate();

        // Initialize the Approov SDK
        try {
            ApproovConfig config = ApproovConfig.getDefaultConfig(getApplicationContext());
            ApproovAttestation.initialize(config);
        } catch (IllegalArgumentException | MalformedURLException ex) {
            Log.e(TAG, ex.getMessage());
        }

        // Create shared OkHttpClient object for the app.
        httpClient = new OkHttpClient();
    }

    // Returns a hundle for the Approov attestation object
    public ApproovAttestation getApproovAttestation(){
        return ApproovAttestation.shared();
    }

    // Accessor for Http Client object
    // Allows for customized stacks on top of the core client
    public OkHttpClient getHttpClient(){
        return httpClient;
    }

}
