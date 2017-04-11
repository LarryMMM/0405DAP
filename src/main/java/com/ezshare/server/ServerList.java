package com.ezshare.server;

import com.ezshare.message.Host;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Wenhao Zhao
 */
public class ServerList {
    private List<Host> serverList = new ArrayList<Host>();
    
    public synchronized List<Host> getServerList() {
        return serverList;
    }
            
    public synchronized int updateServerList(List<Host> inputServerList) {
        int addCount = 0;
        for (Host inputHost : inputServerList) {
            if (!containsHost(inputHost)) {
                serverList.add(inputHost);
                ++addCount;
            }
        }
        return addCount;
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
