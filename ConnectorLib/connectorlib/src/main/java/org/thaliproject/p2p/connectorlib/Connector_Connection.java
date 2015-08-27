// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.connectorlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by juksilve on 13.3.2015.
 */
public class Connector_Connection implements ConnectionCallback{

    private final Connector_Connection that = this;

    private final BluetoothAdapter mBluetoothAdapter;
    private BTListenerThread mBTListenerThread = null;
    private BTConnectToThread mBTConnectToThread = null;

    private final ConnectorLib_Callback callback;
    private final UUID BluetoothUUID;
    private final String BluetoothName;
    private final String mInstanceString;
    private final Handler mHandler;

    // implementation which forwards any uncaught exception from threads to the UI app's thread
    private final Thread.UncaughtExceptionHandler mThreadUncaughtExceptionHandler;

    public Connector_Connection(Context Context, ConnectorLib_Callback Callback, BluetoothAdapter adapter, UUID BtUuid, String btName, String instanceLine){
        this.callback = Callback;
        this.mBluetoothAdapter = adapter;
        this.BluetoothUUID = BtUuid;
        this.BluetoothName = btName;
        this.mInstanceString = instanceLine;
        this.mHandler = new Handler(Context.getMainLooper());
        this.mThreadUncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                final Throwable tmpException = ex;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        throw new RuntimeException(tmpException);
                    }
                });
            }
        };

        setState(ConnectorLib_Callback.ConnectionState.Idle);
    }

    public void StartListening() {

        BTListenerThread tmpList = mBTListenerThread;
        mBTListenerThread = null;
        if (tmpList != null) {
            tmpList.Stop();
        }

        Log.i("", "StartBluetooth listener");
        try {
            tmpList = new BTListenerThread(that, mBluetoothAdapter, BluetoothUUID, BluetoothName);
        }catch (IOException e){
            e.printStackTrace();
            // in this point of time we can not accept any incoming connections, thus what should we do ?
            return;
        }
        tmpList.setDefaultUncaughtExceptionHandler(mThreadUncaughtExceptionHandler);
        setState(ConnectorLib_Callback.ConnectionState.Listening);
        tmpList.start();
        mBTListenerThread = tmpList;
    }

    public boolean TryConnect(BluetoothDevice device,UUID BtUUID, String peerId,String peerName, String peerAddress) {

        if (device == null) {
            Log.i("", "No devices selected");
            return false;
        }

        BTConnectToThread tmp = mBTConnectToThread;
        mBTConnectToThread = null;
        if (tmp != null) {
            tmp.Stop();
        }

        Log.i("", "Selected device address: " + device.getAddress() + ", name: " + device.getName());

        try {
            tmp = new BTConnectToThread(that, device, BtUUID, peerId, peerName, peerAddress, mInstanceString);
        }catch (IOException e){
            e.printStackTrace();
            //lets inform that outgoing connection just failed.
            ConnectionFailed(e.toString(),peerId,peerName,peerAddress);
            return false;
        }
        tmp.setDefaultUncaughtExceptionHandler(mThreadUncaughtExceptionHandler);
        tmp.start();
        mBTConnectToThread = tmp;

        setState(ConnectorLib_Callback.ConnectionState.Connecting);
        Log.i("", "Connecting to " + device.getName() + ", at " + device.getAddress());

        return true;
    }

    public void Stop() {
        Log.i("", "Stop Bluetooth");

        BTListenerThread tmpList = mBTListenerThread;
        mBTListenerThread = null;
        if (tmpList != null) {
            tmpList.Stop();
        }

        BTConnectToThread tmpConn = mBTConnectToThread;
        mBTConnectToThread = null;
        if (tmpConn != null) {
            tmpConn.Stop();
        }
    }

    @Override
    public void Connected(BluetoothSocket socket,String peerId,String peerName,String peerAddress) {
        mBTConnectToThread = null;
        final BluetoothSocket tmp = socket;

        Log.i("HS", "Hand Shake finished outgoing for : " + peerName);

        final String peerIdTmp = peerId;
        final String peerNaTmp = peerName;
        final String peerAdTmp = peerAddress;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tmp.isConnected()) {
                    setState(ConnectorLib_Callback.ConnectionState.Connected);
                    that.callback.Connected(tmp, false,peerIdTmp,peerNaTmp,peerAdTmp);
                } else {
                    ConnectionFailed("Disconnected",peerIdTmp,peerNaTmp,peerAdTmp);
                }
            }
        });
    }

    @Override
    public void GotConnection(BluetoothSocket socket,String peerId,String peerName,String peerAddress) {
        final BluetoothSocket tmp = socket;
        Log.i("HS", "Incoming connection Hand Shake finished for : " + peerName);

        final String peerIdTmp = peerId;
        final String peerNaTmp = peerName;
        final String peerAdTmp = peerAddress;

        StartListening(); // re-start listening for incoming connections.

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tmp.isConnected()) {
                    setState(ConnectorLib_Callback.ConnectionState.Connected);
                    that.callback.Connected(tmp, true,peerIdTmp,peerNaTmp,peerAdTmp);
                } else {
                    ListeningFailed("Disconnected");
                }
            }
        });
    }

    @Override
    public void ConnectionFailed(String reason,String peerId,String peerName,String peerAddress) {
        final String tmp = reason;
        final String peerIdTmp = peerId;
        final String peerNaTmp = peerName;
        final String peerAdTmp = peerAddress;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i("CONNEC", "Error: " + tmp);

                that.callback.ConnectionFailed(peerIdTmp,peerNaTmp,peerAdTmp);

                //only care if we have not stopped & nulled the instance
                BTConnectToThread tmp = mBTConnectToThread;
                mBTConnectToThread = null;
                if (tmp != null) {
                    tmp.Stop();
                }
            }
        });
    }

    @Override
    public void ListeningFailed(String reason) {
        setState(ConnectorLib_Callback.ConnectionState.Idle);
        final String tmp = reason;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i("LISTEN", "Error: " + tmp);
                StartListening();
            }
        });
    }

    private void setState(ConnectorLib_Callback.ConnectionState newState) {
        final ConnectorLib_Callback.ConnectionState tmpState = newState;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                that.callback.ConnectionStateChanged(tmpState);
            }
        });
    }
}
