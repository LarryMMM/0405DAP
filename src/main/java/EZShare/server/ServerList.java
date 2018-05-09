package EZShare.server;

import EZShare.Nodes;
import EZShare.message.*;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Wenhao Zhao
 */
public class ServerList {
    private Gson gson = new Gson();

    private boolean secure;

    public static final int SERVER_TIMEOUT = (int) Nodes.EXCHANGE_PERIOD / 2;

    private final List<Host> serverList = new ArrayList<>();


    public ServerList(boolean secure) {
        this.secure = secure;
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
            if (!containsHost(inputHost) &&
                    !(isMyIpAddress(inputHost.getHostname()) && (inputHost.getPort() == Nodes.PORT ))) {

                openSubscribeRelay(inputHost);
//                Nodes.logger.info("inputHost:"+inputHost);
                serverList.add(inputHost);
//                Nodes.logger.info("ENDinputHost:"+inputHost);
                ++addCount;
            }
        }
        return addCount;
    }
/*no regular exchange*/

    public synchronized void removeServer(Host inputHost, boolean secure) {

        closeSubscribeRelay(inputHost);

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


    public void openSubscribeRelay(Host target) {
        ConcurrentHashMap<Host, Socket> relay;
        try {
            Socket socket = new Socket();
            relay = Nodes.unsecure_relay;
            /* Set timeout for connection establishment, throwing ConnectException */
            socket.connect(new InetSocketAddress(target.getHostname(), target.getPort()), SERVER_TIMEOUT);

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            //DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            //traverse all subscribers
            for (Map.Entry<Socket, Subscription> subscriber : Nodes.subscriptions.entrySet()) {
                //get all subscribe message of this subscriber
                ConcurrentHashMap<SubscribeMessage, Integer> messages = subscriber.getValue().getSubscribeMessage();
                for (Map.Entry<SubscribeMessage, Integer> subscription : messages.entrySet()) {
                    //if this message have relay=true
                    if (subscription.getKey().isRelay()) {
                        SubscribeMessage forwarded = new SubscribeMessage(false, subscription.getKey().getId(),
                                subscription.getKey().getResourceTemplate());

                        String JSON = gson.toJson(forwarded, SubscribeMessage.class);
                        outputStream.writeUTF(JSON);
                        outputStream.flush();
                    }
                }
            }
            relay.put(target, socket);
            Nodes.logger.info("relay connection opened " + target.toString());
        } catch (IOException e) {
            Nodes.logger.warning("IOException when subscribe relay to " + target.toString());
        }


    }

    public synchronized void closeSubscribeRelay(Host target) {
        Socket socket = null;
        ConcurrentHashMap<Host, Socket> relay;

        try {
            relay = Nodes.unsecure_relay;
            socket = relay.get(target);

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());


            //traverse all subscribers
            for (Map.Entry<Socket, Subscription> subscriber : Nodes.subscriptions.entrySet()) {
                //get all subscribe message of this subscriber
                ConcurrentHashMap<SubscribeMessage, Integer> messages = subscriber.getValue().getSubscribeMessage();
                for (Map.Entry<SubscribeMessage, Integer> subscription : messages.entrySet()) {
                    //if this message have relay=true
                    if (subscription.getKey().isRelay()) {
                        String JSON = gson.toJson(new UnsubscribeMessage(subscription.getKey().getId()), UnsubscribeMessage.class);
                        outputStream.writeUTF(JSON);
                        outputStream.flush();

                        /*
                        String response = inputStream.readUTF();

                        if (response.contains("resultSize")) {
                            UN.logger.warning("Terminate from: " + subscriber.getKey().getRemoteSocketAddress() +
                                    " relayed to: " + target.toString() +
                                    " for " + subscription.getKey().getId());
                        }
                        */
                    }

                }


            }
            relay.remove(target);


        } catch (IOException e) {
            Nodes.logger.warning("IOException when subscribe relay to " + target.toString());
        }
    }

    public synchronized void doMessageRelay(String JSON) {

        ConcurrentHashMap<Host, Socket> relay;

        relay = Nodes.unsecure_relay;

        try {
            for (Map.Entry<Host, Socket> entry : relay.entrySet()) {
                DataOutputStream outputStream = new DataOutputStream(entry.getValue().getOutputStream());

                entry.getValue().setSoTimeout(3000);

                outputStream.writeUTF(JSON);
                outputStream.flush();
                Nodes.logger.fine("message relayed");

            }

        } catch (IOException e) {
            Nodes.logger.warning("IOException when subscribe relay: " + e.getMessage());
            e.printStackTrace();
            System.out.println("JSON : " + JSON);
        }

    }

    public synchronized void refreshAllRelay() {
        ConcurrentHashMap<Host, Socket> relay;

        relay = Nodes.unsecure_relay;
        relay = new ConcurrentHashMap<>();

        for (Host h : this.serverList) {
            openSubscribeRelay(h);
        }
        //point??@larry

    }


}
