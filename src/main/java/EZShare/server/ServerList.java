package EZShare.server;

import EZShare.Server;
import EZShare.message.ExchangeMessage;
import EZShare.message.Host;
import com.google.gson.Gson;

import javax.net.ssl.SSLSocket;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author Wenhao Zhao
 */
public class ServerList {
    public static final int SERVER_TIMEOUT = (int) Server.EXCHANGE_PERIOD / 2;
    
    private final List<Host> serverList = new ArrayList<>();
    private final List<Host> secure_serverList = new ArrayList<>();
    
//    public ServerList() {
//        /* Add a default dummy host (which is of course not available) */
//        Host host = new Host("localhorse", 9527);
//        this.serverList.add(host);
//    }
    
    public synchronized List<Host> getServerList(boolean secure) {
        if(secure){
            return secure_serverList;
        }else {
            return serverList;
        }
    }
            
    public synchronized int updateServerList(List<Host> inputServerList,boolean secure) {
        List<Host> serverList;
        ConcurrentHashMap<Socket, Subscription> relay;
        if(secure){
            serverList = this.secure_serverList;
            relay = Server.secure_relay;
        }   else {
            serverList = this.serverList;
            relay = Server.unsecure_relay;
        }
        int addCount = 0;
        for (Host inputHost : inputServerList) {
            /* 
                Discard host if (1) already in the list (2) is a local address

            */
            if (!containsHost(inputHost,secure) &&
                    !(isMyIpAddress(inputHost.getHostname()) && (inputHost.getPort()==Server.PORT||inputHost.getPort()==Server.SPORT))) {
                serverList.add(inputHost);

                //for each subscription connection.
                for (Map.Entry<Socket, Subscription> s: Server.subscriptions.entrySet()) {
                    String orgin = s.getValue().getOrigin();
                    boolean isrelay = s.getValue().getSubscribeMessage().isRelay();
                    boolean issecure = s.getKey().getClass().equals(SSLSocket.class);
                    //check whether this client need to be relayed for this exchange.
                    if(isrelay&&(issecure==secure)){
                        //relay to this server for the client
                        Server.doSingleSubscriberRelay(orgin,inputHost,s.getValue().getSubscribeMessage(),secure);
                    }
                }

                ++addCount;
            }
        }
        return addCount;
    }
    
    public synchronized void regularExchange(boolean secure) {
        List<Host> serverList;
        if(secure){
            serverList = this.secure_serverList;
        }   else {
            serverList = this.serverList;
        }

        if (serverList.size() > 0) {
            int randomIndex = ThreadLocalRandom.current().nextInt(0, serverList.size());
            Host randomHost = serverList.get(randomIndex);
            
            Socket socket = null;
            
            try {
                // Need SSL!!!
                if (secure) {
                    socket = Server.context.getSocketFactory().createSocket();
                } else {
                    socket = new Socket();
                }
                /* Set timeout for connection establishment, throwing ConnectException */
                socket.connect(new InetSocketAddress(randomHost.getHostname(), randomHost.getPort()), SERVER_TIMEOUT);


                /* Set timeout for read() (also readUTF()!), throwing SocketTimeoutException */
                socket.setSoTimeout(SERVER_TIMEOUT);
                
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());

                Server.logger.fine("Regular EXCHANGE to " + socket.getRemoteSocketAddress());
                
                ExchangeMessage exchangeMessage = new ExchangeMessage(serverList);

                String JSON = new Gson().toJson(exchangeMessage);
                output.writeUTF(JSON);
                output.flush();

                String response = input.readUTF();
                
                if (response.contains("error"))
                    Server.logger.warning("RECEIVED : " + response);
                if (response.contains("success"))
                    Server.logger.fine("RECEIVED : " + response);
            } catch (ConnectException ex) {
                Server.logger.warning(randomHost.toString() + " connection timeout");
                removeServer(randomHost,secure);
            } catch (SocketTimeoutException ex) {
                Server.logger.warning(randomHost.toString() + " readUTF() timeout");
                removeServer(randomHost,secure);
            } catch (IOException ex) {
                /* Unclassified exception */
                Server.logger.warning(randomHost.toString() + " IOException");
                removeServer(randomHost,secure);
            } finally {
                try {
                    if (socket != null)
                        socket.close();
                } catch (IOException e) {
                    Server.logger.warning("IOException! Disconnect!");
                }
            }
        }
    }
    
    public synchronized void removeServer(Host inputHost,boolean secure) {
        List<Host> serverList;
        ConcurrentHashMap<Socket, Subscription> relay;
        if(secure){
            serverList = this.secure_serverList;
            relay = Server.secure_relay;
        }   else {
            serverList = this.serverList;
            relay = Server.unsecure_relay;
        }

        //delete all subscription relayed to that server
        for (Map.Entry<Socket, Subscription> s: relay.entrySet()) {
            //if the subscription has the same target as the deleted host
            if(s.getValue().getTarget().toString().equals(inputHost.toString())){
                Server.closeSubscription(s.getKey(),s.getValue(),secure);
            }
        }

        serverList.remove(inputHost);

    }
    
    private synchronized boolean containsHost(Host inputHost,boolean secure) {
        List<Host> serverList;
        if(secure){
            serverList = this.secure_serverList;
        }   else {
            serverList = this.serverList;
        }
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
