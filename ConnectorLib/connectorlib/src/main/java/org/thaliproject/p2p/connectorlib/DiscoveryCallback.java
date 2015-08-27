package org.thaliproject.p2p.connectorlib;

import java.util.List;

/**
 * Created by juksilve on 27.8.2015.
 */
interface DiscoveryCallback {
    public enum State{
        DiscoveryIdle,
        DiscoveryNotInitialized,
        DiscoveryFindingPeers,
        DiscoveryFindingServices
    }
    void gotServicesList(List<ServiceItem> list);
    void foundService(ServiceItem item);
    void DiscoveryStateChanged(State newState);
}
