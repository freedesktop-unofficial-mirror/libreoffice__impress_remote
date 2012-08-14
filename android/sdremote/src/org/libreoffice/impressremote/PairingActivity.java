/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.impressremote;

import java.text.MessageFormat;

import org.libreoffice.impressremote.communication.CommunicationService;
import org.libreoffice.impressremote.communication.CommunicationService.State;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;

public class PairingActivity extends Activity {
    private CommunicationService mCommunicationService;
    private boolean mIsBound = false;
    private TextView mPinText;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bindService(new Intent(this, CommunicationService.class), mConnection,
                        Context.BIND_IMPORTANT);
        mIsBound = true;

        IntentFilter aFilter = new IntentFilter(
                        CommunicationService.MSG_PAIRING_STARTED);
        aFilter.addAction(CommunicationService.MSG_PAIRING_SUCCESSFUL);
        LocalBroadcastManager.getInstance(this).registerReceiver(mListener,
                        aFilter);

    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName aClassName,
                        IBinder aService) {
            setContentView(R.layout.activity_pairing);
            mPinText = (TextView) findViewById(R.id.pairing_pin);
            mCommunicationService = ((CommunicationService.CBinder) aService)
                            .getService();
            ((TextView) findViewById(R.id.pairing_instruction2_deviceName))
                            .setText(MessageFormat
                                            .format(getResources()
                                                            .getString(R.string.pairing_instructions_2_deviceName),
                                                            mCommunicationService
                                                                            .getDeviceName()));

            if (mCommunicationService.getState() == State.CONNECTING) {
                mPinText.setText(mCommunicationService.getPairingPin());
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName aClassName) {
            mCommunicationService = null;
        }
    };

    private BroadcastReceiver mListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context aContext, Intent aIntent) {
            if (aIntent.getAction().equals(
                            CommunicationService.MSG_PAIRING_STARTED)) {
                String aPin = aIntent.getStringExtra("PIN");
                mPinText.setText(aPin);
                //                refreshLists();
            } else if (aIntent.getAction().equals(
                            CommunicationService.MSG_PAIRING_SUCCESSFUL)) {
                Intent nIntent = new Intent(PairingActivity.this,
                                StartPresentationActivity.class);
                startActivity(nIntent);
            }

        }
    };

}