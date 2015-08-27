package org.thaliproject.p2p.connectorlib;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by juksilve on 20.4.2015.
 */

class BLEScannerKitKat implements DiscoveryCallback {

    private final BLEScannerKitKat that = this;
    private final Context context;
    private final DiscoveryCallback mDiscoveryCallback;
    private final BluetoothAdapter mBluetoothAdapter;
    private final CopyOnWriteArrayList<BLEDeviceListItem> mBLEDeviceList = new CopyOnWriteArrayList<>();

    private BLEValueReader mBLEValueReader = null;

    public BLEScannerKitKat(Context Context, DiscoveryCallback CallBack,BluetoothManager Manager) {
        this.context = Context;
        this.mDiscoveryCallback = CallBack;
        this.mBluetoothAdapter = Manager.getAdapter();
    }

    public void Start() {
        Stop();
        BLEValueReader tmpValueReader = new BLEValueReader(this.context,this,mBluetoothAdapter);
        mBLEValueReader = tmpValueReader;
        mBluetoothAdapter.startLeScan(leScanCallback);
    }

    public void Stop() {
        mBluetoothAdapter.stopLeScan(leScanCallback);

        BLEValueReader tmp = mBLEValueReader;
        mBLEValueReader = null;
        if(tmp != null){
            tmp.Stop();
        }
    }

    @Override
    public void gotServicesList(List<ServiceItem> list) {
       //we clear our scanner device list, so we can check which devices we are seeing now
        mBLEDeviceList.clear();
        mDiscoveryCallback.gotServicesList(list);
    }

    @Override
    public void foundService(ServiceItem item) {
        mDiscoveryCallback.foundService(item);
    }

    @Override
    public void DiscoveryStateChanged(State newState) {
        mDiscoveryCallback.DiscoveryStateChanged(newState);
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

            if(device != null && scanRecord != null) {

                BLEDeviceListItem itemTmp = null;
                for (BLEDeviceListItem item : mBLEDeviceList) {
                    if (item != null && item.getDevice() != null) {
                        if (item.getDevice().getAddress().equalsIgnoreCase(device.getAddress())) {
                            itemTmp = item;
                        }
                    }
                }

                //in here we just care of devices we have not seen earlier.
                if (itemTmp != null) {
                    //we seen this earlier, so lets not do any further processing
                    return;
                }

                itemTmp = new BLEDeviceListItem(device, scanRecord);
                if (!itemTmp.getUUID().equalsIgnoreCase(BLEBase.SERVICE_UUID_1)) {
                    //its not our service, so we are not interested on it anymore.
                    // but to faster ignore it later scan results, we'll add it to the list
                    // but we don't give it to the value reader for any further processing
                    mBLEDeviceList.add(itemTmp);
                    return;
                }

                //new device we have not seen since last mBLEDeviceList clear, so lets get values from it
                // will also start the reader process if not started earlier
                Log.i("SCAN", "added new device : " + device.getAddress());
                //Add device will actually start the discovery process
                mBLEValueReader.AddDevice(device);
            }
        }
    };
}
