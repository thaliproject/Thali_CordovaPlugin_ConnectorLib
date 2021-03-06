// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.connectorlib;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Created by juksilve on 22.06.2015.
 */
public class Connector_Discovery implements AdvertiserCallback, DiscoveryCallback {

    private final Connector_Discovery that = this;
    private BLEScannerKitKat mSearchKitKat = null;
    private BLEAdvertiserLollipop mBLEAdvertiserLollipop = null;

    private final Context context;
    private final String mSERVICE_TYPE;

    private final ConnectorLib_Callback callback;
    private final BluetoothManager mBluetoothManager;
    private final BluetoothGattService mFirstService;

    public Connector_Discovery(Context Context, ConnectorLib_Callback Callback, String ServiceType, String instanceLine){
        this.context = Context;
        this.mSERVICE_TYPE = ServiceType;
        this.callback = Callback;
        this.mBluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mFirstService = new BluetoothGattService(UUID.fromString(BLEBase.SERVICE_UUID_1),BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic firstServiceChar = new BluetoothGattCharacteristic(UUID.fromString(BLEBase.CharacteristicsUID1),BluetoothGattCharacteristic.PROPERTY_READ,BluetoothGattCharacteristic.PERMISSION_READ );
        firstServiceChar.setValue(instanceLine.getBytes());

        this.mFirstService.addCharacteristic(firstServiceChar);
    }

     public void Start() {
         Log.i("Connector_Discovery", "starting");
         StartAdvertiser();
         StartScanner();
         DiscoveryStateChanged(State.DiscoveryFindingPeers);
    }

    public void Stop() {
        Log.i("Connector_Discovery", "Stopping");
        StopAdvertiser();
        StopScanner();
        DiscoveryStateChanged(State.DiscoveryIdle);
    }

    private void StartAdvertiser(){
        StopAdvertiser();
        BLEAdvertiserLollipop tmpAdvertiserLollipop = new BLEAdvertiserLollipop(that.context,that,mBluetoothManager);
        tmpAdvertiserLollipop.addService(mFirstService);
        tmpAdvertiserLollipop.Start();
        mBLEAdvertiserLollipop = tmpAdvertiserLollipop;
    }

    private void StopAdvertiser(){
        BLEAdvertiserLollipop tmpAdvertiser = mBLEAdvertiserLollipop;
        mBLEAdvertiserLollipop = null;
        if(tmpAdvertiser != null){
            tmpAdvertiser.Stop();
        }
    }

    private void StartScanner() {
        StopScanner();
        BLEScannerKitKat tmpScannerKitKat = new BLEScannerKitKat(that.context, that,that.mBluetoothManager);
        tmpScannerKitKat.Start();
        mSearchKitKat = tmpScannerKitKat;
    }

    private void StopScanner() {
        BLEScannerKitKat tmpScanner = mSearchKitKat;
        mSearchKitKat = null;
        if(tmpScanner != null){
            tmpScanner.Stop();
        }
    }

    @Override
    public void onAdvertisingStarted(String error) {
        //todo should we have a way on reporting advertising prolems ?
        Log.i("Connector_Discovery", "Started err : " + error);
    }

    @Override
    public void onAdvertisingStopped(String error) {
        Log.i("Connector_Discovery", "Stopped err : " + error);
    }

    //we are simply forwarding thw calls for DiscoveryCallback to be handled in the ConnectorLib
    @Override
    public void gotServicesList(List<ServiceItem> list) {
        callback.CurrentPeersList(list);
    }

    @Override
    public void foundService(ServiceItem item) {
        callback.PeerDiscovered(item);
    }

    @Override
    public void DiscoveryStateChanged(State newState) {

        switch(newState){
            case DiscoveryIdle:
                callback.DiscoveryStateChanged(ConnectorLib_Callback.DiscoveryState.Idle);
                break;
            case DiscoveryNotInitialized:
                callback.DiscoveryStateChanged(ConnectorLib_Callback.DiscoveryState.NotInitialized);
                break;
            case DiscoveryFindingPeers:
                callback.DiscoveryStateChanged(ConnectorLib_Callback.DiscoveryState.FindingPeers);
                break;
            case DiscoveryFindingServices:
                callback.DiscoveryStateChanged(ConnectorLib_Callback.DiscoveryState.FindingServices);
                break;
            default:
                throw new RuntimeException("Invalid value for DiscoveryCallback.State = " + newState);
        }
    }
}
