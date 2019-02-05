/*****************************************************************************
 * Project:     Demo Client App
 * File:        DisplayShape.java
 * Original:    Created on 14 Sep 2016
 * Copyright(c) 2002 - 2016 by CriticalBlue Ltd.
 ****************************************************************************/

package com.criticalblue.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

/**
 * Activity used to display a shape based on the information passe in the intent
 */
public class DisplayShape extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_display);

        Bundle aExtras = getIntent().getExtras();

        WebView aWebview = (WebView) findViewById(R.id.webView);
        aWebview.loadUrl("file:///android_asset/"+aExtras.getString("Shape")+".html");

    }

    /**
     * Called by a button click in the view. Will return to first activity.
     *
     * @param view the view that triggered the request
     */
    public void reset(View view){
        // Send intent to return to request a new shape
        Intent intent = new Intent(getBaseContext(), RequestShape.class);
        startActivity(intent);
        finish();
    }

}
