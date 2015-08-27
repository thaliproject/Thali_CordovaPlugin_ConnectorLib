// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.connectorlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


/**
 * Created by juksilve on 13.3.2015.
 */
public class ConnectorLib implements APIStatusChangedCallback,ConnectorLib_Callback {

    private final ConnectorLib that = this;

    public class WifiBtStatus{
        public WifiBtStatus(){
            isWifiOk = false;
            isBtOk = false;
            isWifiEnabled = false;
            isBtEnabled = false;
            isBLESupported = false;
            isBLEAdvertisingSupported = false;
        }

        public boolean isWifiOk;
        public boolean isBtOk;
        public boolean isWifiEnabled;
        public boolean isBtEnabled;
        public boolean isBLESupported;
        public boolean isBLEAdvertisingSupported;
    }

    private BluetoothBase mBluetoothBase = null;
    private Connector_Connection mConnector_Connection = null;
    private Connector_Discovery mConnector_Discovery = null;

    private String mInstanceString = "";
    static final String JSON_ID_PEERID   = "pi";
    static final String JSON_ID_PEERNAME = "pn";
    static final String JSON_ID_BTADRRES = "ra";

    private final ConnectorLib_Callback callback;
    private final Context context;
    private final Handler mHandler;

    private final ConnectorSettings ConSettings;

    public ConnectorLib(Context Context, ConnectorLib_Callback Callback, ConnectorSettings settings){
        this.context = Context;
        this.callback = Callback;
        this.mHandler = new Handler(this.context.getMainLooper());
        this.ConSettings = settings;
    }

    public WifiBtStatus Start(String peerIdentifier, String peerName) {
        //initialize the system, and
        // make sure BT & Wifi is enabled before we start running

        WifiBtStatus ret = new WifiBtStatus();
        Stop();

        BluetoothBase tmpBTbase = new BluetoothBase(this.context, this);

        ret.isBtOk = tmpBTbase.Start();
        ret.isBtEnabled = tmpBTbase.isBluetoothEnabled();

        JSONObject jsonobj = new JSONObject();
        try {
            jsonobj.put(JSON_ID_PEERID, peerIdentifier);
            jsonobj.put(JSON_ID_PEERNAME, peerName);
            jsonobj.put(JSON_ID_BTADRRES, tmpBTbase.getAddress());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mInstanceString = jsonobj.toString();

        Log.i("", " mInstanceString : " + mInstanceString);

        //for compatibility
        ret.isWifiOk = true;
        ret.isWifiEnabled = true;

        // these are needed with BLE discovery
        ret.isBLESupported =  BLEBase.isBLESupported(context);
        ret.isBLEAdvertisingSupported =  BLEBase.isBLEAdvertisingSupported(context);

        //set the global values with our local ones
        mBluetoothBase = tmpBTbase;

        if (!ret.isWifiOk || !ret.isBtOk || !ret.isBLESupported || !ret.isBLEAdvertisingSupported) {
            // the HW is not supporting all needed stuff
            Log.i("", "BT available: " + ret.isBtOk + ", wifi available: " + ret.isWifiOk);
            setState(DiscoveryState.NotInitialized);
            return ret;
        }

        if (!ret.isBtEnabled  || !ret.isWifiEnabled) {
            //we will be waiting until both Wifi & BT are turned on
            setState(DiscoveryState.WaitingStateChange);
            return ret;
        }

        //all is good, so lets get started
        Log.i("", "All stuff available and enabled");
        startAll();
        return ret;
    }

    public void Stop() {
        stopAll();

        BluetoothBase tmpb =mBluetoothBase;
        mBluetoothBase = null;
        if (tmpb != null) {
            tmpb.Stop();
        }
    }

    public enum TryConnectReturnValues{
        Connecting,
        AlreadyAttemptingToConnect, // this test is wrong, don't want to change for compatibility
        NoSelectedDevice,
        BTDeviceFetchFailed
    }

    public TryConnectReturnValues TryConnect(ServiceItem selectedDevice) {

        if(selectedDevice == null) {
            return TryConnectReturnValues.NoSelectedDevice;
        }

        BluetoothBase tmoBase = mBluetoothBase;
        if(tmoBase == null) {// should never happen, would indicate uninitialized system
            throw new RuntimeException("BluetoothBase is not initialized properly");
        }

        BluetoothDevice device = tmoBase.getRemoteDevice(selectedDevice.peerAddress);
        if (device == null) {
            return TryConnectReturnValues.BTDeviceFetchFailed;
        }

        Connector_Connection tmpConn = mConnector_Connection;
        if(tmpConn == null) { // should never happen, would indicate uninitialized system
            throw new RuntimeException("Connector class is not initialized properly");
        }

        // actually the ret will now be always true, since mConnector_Connection only checks if device is non-null
        tmpConn.TryConnect(device, this.ConSettings.MY_UUID, selectedDevice.peerId, selectedDevice.peerName, selectedDevice.peerAddress);
        return TryConnectReturnValues.Connecting;
    }

    private void startServices() {
        stopServices();

        Log.i("", "Starting services address: " + mInstanceString + ", " + ConSettings);
        Connector_Discovery tmpDisc= new Connector_Discovery(this.context,this,ConSettings.SERVICE_TYPE,mInstanceString);
        tmpDisc.Start();
        mConnector_Discovery = tmpDisc;
    }

    private  void stopServices() {
        Log.i("", "Stopping services");
        Connector_Discovery tmp = mConnector_Discovery;
        mConnector_Discovery = null;
        if (tmp != null) {
            tmp.Stop();
        }
    }

    private  void startBluetooth() {
        stopBluetooth();
        BluetoothAdapter tmp = null;

        BluetoothBase tmpBase = mBluetoothBase;
        if (tmpBase != null) {
            tmp = tmpBase.getAdapter();
        }
        Log.i("", "StartBluetooth listener");
        Connector_Connection tmpconn = new Connector_Connection(this.context,this,tmp,this.ConSettings.MY_UUID, this.ConSettings.MY_NAME,this.mInstanceString);
        tmpconn.StartListening();
        mConnector_Connection = tmpconn;
    }

    private  void stopBluetooth() {
        Log.i("", "Stop Bluetooth");
        Connector_Connection tmp = mConnector_Connection;
        mConnector_Connection = null;
        if(tmp != null){
            tmp.Stop();
        }
    }

    private void stopAll() {
        Log.i("", "Stoping All");
        stopServices();
        stopBluetooth();
    }

    private void startAll() {
        stopAll();
        Log.i("", "Starting All");
        startServices();
        startBluetooth();
    }

    @Override
    public void BluetoothStateChanged(int state) {

        if (state == BluetoothAdapter.SCAN_MODE_NONE) {
            Log.i("BT", "Bluetooth DISABLED, stopping");
            stopAll();
            // indicate the waiting with state change
            setState(DiscoveryState.WaitingStateChange);
            return;
        }

        if (mConnector_Discovery != null) {
            Log.i("WB", "We already were running, thus doing nothing");
            return;
        }

        // we got bt back, and Wifi is already on, thus we can re-start now
        Log.i("BT", "Bluetooth enabled, re-starting");
        startAll();
    }

    @Override
    public void Connected(BluetoothSocket socket, boolean incoming, String peerId, String peerName, String peerAddress) {
        final BluetoothSocket socketTmp = socket;
        final boolean incomingTmp = incoming;
        final String peerIdTmp = peerId;
        final String peerNameTmp = peerName;
        final String peerAddressTmp = peerAddress;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.Connected(socketTmp, incomingTmp, peerIdTmp, peerNameTmp, peerAddressTmp);
            }
        });
    }

    @Override
    public void ConnectionFailed(String peerId, String peerName, String peerAddress) {
        final String peerIdTmp = peerId;
        final String peerNameTmp = peerName;
        final String peerAddressTmp = peerAddress;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.ConnectionFailed(peerIdTmp, peerNameTmp, peerAddressTmp);
            }
        });
    }

    @Override
    public void DiscoveryStateChanged(DiscoveryState newState) {
        final DiscoveryState tmpNewState = newState;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                that.callback.DiscoveryStateChanged(tmpNewState);
            }
        });
    }

    @Override
    public void CurrentPeersList(List<ServiceItem> available) {
        final List<ServiceItem> availableTmp = available;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                that.callback.CurrentPeersList(availableTmp);
            }
        });
    }

    @Override
    public void PeerDiscovered(ServiceItem service) {
        final ServiceItem serviceTmp = service;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                that.callback.PeerDiscovered(serviceTmp);
            }
        });
    }

    @Override
    public void ConnectionStateChanged(ConnectionState newState) {
        final ConnectionState tmpState = newState;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.ConnectionStateChanged(tmpState);
            }
        });
    }

    private void setState(DiscoveryState newState) {
        final DiscoveryState tmpState = newState;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                that.callback.DiscoveryStateChanged(tmpState);
            }
        });
    }
}
