package com.ezshare.server;

import com.ezshare.message.Host;
import java.util.List;

/**
 *
 * @author Wenhao Zhao
 */
public class ServerList {
    private List<Host> serverList;
    
    public synchronized List<Host> getServerList() {
        return serverList;
    }
            
    public synchronized void updateServerList(List<Host> inputServerList) {
        for (Host inputHost : inputServerList) {
            if (!containsHost(inputHost)) {
                serverList.add(inputHost);
            }
        }
    }
    
    private synchronized boolean containsHost(Host inputHost) {
        for (Host host : serverList) {
            if (host.getHostname().equals(inputHost.getHostname()) && host.getPort().equals(inputHost.getPort())) {
                return true;
            }
        }
        return false;
    }
}
