package org.thaliproject.p2p.connectorlib;

import android.bluetooth.BluetoothSocket;

/**
 * Created by juksilve on 27.8.2015.
 */
public interface ConnectionCallback {
    void GotConnection(BluetoothSocket socket,String peerId,String peerName,String peerAddress);
    void ListeningFailed(String reason);
    void Connected(BluetoothSocket socket,String peerId,String peerName,String peerAddress);
    void ConnectionFailed(String reason,String peerId,String peerName,String peerAddress);
}
