package org.thaliproject.p2p.connectorlib;

import android.bluetooth.BluetoothSocket;

import java.util.List;

/**
 * Created by juksilve on 27.8.2015.
 */
public interface ConnectorLib_Callback {

    public enum DiscoveryState{
        Idle,
        NotInitialized,
        WaitingStateChange,
        FindingPeers,
        FindingServices
    }
    public enum ConnectionState{
        Idle,
        Listening,
        Connecting,
        Connected
    }

    void ConnectionStateChanged(ConnectionState newState);
    void Connected(BluetoothSocket socket, boolean incoming,String peerId,String peerName,String peerAddress);
    void ConnectionFailed(String peerId,String peerName,String peerAddress);
    void DiscoveryStateChanged(DiscoveryState newState);
    void CurrentPeersList(List<ServiceItem> available);
    void PeerDiscovered(ServiceItem service);
}

