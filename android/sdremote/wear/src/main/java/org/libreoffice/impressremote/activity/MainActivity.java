/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import org.libreoffice.impressremote.R;
import org.libreoffice.impressremote.communication.DataLayerListenerService;


public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{


    private GoogleApiClient mGoogleApiClient;
    private IntentsReceiver mIntentsReceiver;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full_screen_minimal);

        if(null == mGoogleApiClient) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            Log.v(TAG, "GoogleApiClient created");
        }

        if(!mGoogleApiClient.isConnected()){
            mGoogleApiClient.connect();
            Log.v(TAG, "Connecting to GoogleApiClient..");
        }

        registerIntentsReceiver();

       this.startService(new Intent(this, DataLayerListenerService.class));

    }
    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        unregisterIntentsReceiver();
        this.stopService(new Intent(this, DataLayerListenerService.class));
        super.onDestroy();
    }

    @Override
     protected void onPause(){
        Log.v(TAG, "onPause");
        DataLayerListenerService.commandAppPaused();
        super.onPause();
    }

    @Override
    protected void onStop(){
        Log.v(TAG, "onStop");
        super.onStop();
    }


    private void registerIntentsReceiver() {
        mIntentsReceiver = new IntentsReceiver(this);
        IntentFilter aIntentFilter = buildIntentsReceiverFilter();

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mIntentsReceiver, aIntentFilter);
    }

    private void unregisterIntentsReceiver() {
        try {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mIntentsReceiver);
        } catch (IllegalArgumentException ignored) {

        }
    }

    private IntentFilter buildIntentsReceiverFilter() {
        IntentFilter aIntentFilter = new IntentFilter();
        aIntentFilter.addAction("SLIDE_COUNT");
        return aIntentFilter;
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.v(TAG, "onConnectionSuspended called");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG,"onConnectionFailed called");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.v(TAG,"onConnected called");
        DataLayerListenerService.commandConnect();
    }

    public void onButtonClickedPrevious(View target) {
        DataLayerListenerService.commandPrevious();
    }
    public void onButtonClickedNext(View target) {
        DataLayerListenerService.commandNext();
    }
    public void onButtonClickedPauseResume(View target) {
        DataLayerListenerService.commandPauseResume();
    }
    private void changeSlideCount(String count){
        TextView textView;
        textView = (TextView) findViewById(R.id.textView_counter);
        textView.setText(count);
    }
    private static final class IntentsReceiver extends BroadcastReceiver {
        private final MainActivity mainActivity;
        private IntentsReceiver(MainActivity aMainActivity) {
            mainActivity = aMainActivity;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("SLIDE_COUNT")) {
                mainActivity.changeSlideCount(intent.getStringExtra("DATA"));
            }
            Log.d("receiver", "Got message: " + intent.getStringExtra("DATA"));
        }
    }


}
