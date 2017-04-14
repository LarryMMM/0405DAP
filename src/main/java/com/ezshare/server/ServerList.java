package com.ezshare.server;

import com.ezshare.message.ExchangeMessage;
import com.ezshare.message.Host;
import com.google.gson.Gson;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author Wenhao Zhao
 */
public class ServerList {
    public static final int SERVER_TIMEOUT = (int) ServerInstance.EXCHANGE_PERIOD / 2;
    
    /* TO-DO: What if serverList contains itself? */
    private final List<Host> serverList = new ArrayList<>();
    
    public ServerList() {
        /* Add a default dummy host (which is of course not available) */
        Host host = new Host("localhorse", 9527);
        this.serverList.add(host);
    }
    
    public synchronized List<Host> getServerList() {
        return serverList;
    }
            
    public synchronized int updateServerList(List<Host> inputServerList) {
        int addCount = 0;
        for (Host inputHost : inputServerList) {
            /* 
                Discard host if (1) already in the list (2) is a local address
            */
            if (!containsHost(inputHost) && !isMyIpAddress(inputHost.getHostname())) {
                serverList.add(inputHost);
                ++addCount;
            }
        }
        return addCount;
    }
    
    public synchronized void regularExchange() {
        if (serverList.size() > 0) {
            int randomIndex = ThreadLocalRandom.current().nextInt(0, serverList.size());
            Host randomHost = serverList.get(randomIndex);
            
            Socket socket = new Socket();
            
            try {
                /* Set timeout for connection establishment, throwing ConnectException */
                socket.connect(new InetSocketAddress(randomHost.getHostname(), randomHost.getPort()), SERVER_TIMEOUT);
                /* Set timeout for read() (also readUTF()!), throwing SocketTimeoutException */
                socket.setSoTimeout(SERVER_TIMEOUT);
                
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());

                ServerInstance.logger.fine("Regular EXCHANGE to " + socket.getRemoteSocketAddress());
                
                ExchangeMessage exchangeMessage = new ExchangeMessage(serverList);
                
                String JSON = new Gson().toJson(exchangeMessage);
                output.writeUTF(JSON);
                output.flush();

                String response = input.readUTF();
                
                if (response.contains("error"))
                    ServerInstance.logger.warning("RECEIVED : " + response);
                if (response.contains("success"))
                    ServerInstance.logger.fine("RECEIVED : " + response);
            } catch (ConnectException ex) {
                ServerInstance.logger.warning(randomHost.toString() + " connection timeout");
                removeServer(randomHost);
            } catch (SocketTimeoutException ex) {
                ServerInstance.logger.warning(randomHost.toString() + " readUTF() timeout");
                removeServer(randomHost);
            } catch (IOException ex) {
                /* Unclassified exception */
                ServerInstance.logger.warning(randomHost.toString() + " IOException");
                removeServer(randomHost);
            }
        }
    }
    
    private synchronized void removeServer(Host inputHost) {
        serverList.remove(inputHost);
    }
    
    private synchronized boolean containsHost(Host inputHost) {
        for (Host host : serverList) {
            if (host.getHostname().equals(inputHost.getHostname()) && host.getPort().equals(inputHost.getPort())) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isMyIpAddress(String ipAddress) {
        InetAddress addr;
        try {
            addr = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException ex) {
            /* False-positive!!! If error occurred, the address should not be added to the server list. */
            return true;
        }
        /* Check if the address is a valid special local or loop back */
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
            return true;
        }

        /* Check if the address is defined on any interface */
        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
        }
    }
}
