package com.ezshare.server;

import com.ezshare.log.LogCustomFormatter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javax.net.ServerSocketFactory;

/**
 *
 * @author Wenhao Zhao
 */
public class ServerInstance {

    /* Configuration */
    public static final int PORT = 3000;
    public static final int MAX_THREAD_COUNT = 10;
    public static final long EXCHANGE_PERIOD = 2000;
    public static final long INTERVAL = 1000;
    
    public static final Logger logger = LogCustomFormatter.getLogger(ServerInstance.class.getName());
    
    private static final FileList fileList = new FileList();
    private static final ServerList serverList = new ServerList();
    
    /*
        Currently it is a simple fixed-volume thread pool.
        If no thread resource is available at the moment, it would be blocked until it could get one.
    */
    private static ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREAD_COUNT);
    
    /*
        A HashMap to record the mapping from a specified client to the starting time of its last connection
    */
    private static HashMap<String, Long> intervalLimit = new HashMap<>();

    
    public static void main(String[] args) {
        /* Timer running as a daemon thread schedules the regular EXCHANGE command. */
        Timer timer = new Timer(true);
        TimerTask regularExchangeTask = new TimerTask() {   
            @Override
            public void run() {
                serverList.regularExchange();
            }   
        };   
        timer.schedule(regularExchangeTask, 0, EXCHANGE_PERIOD);
        
        /* Receive requests. */
        ServerSocketFactory factory = ServerSocketFactory.getDefault();
        try (ServerSocket server = factory.createServerSocket(PORT)) {
            System.out.println("ServerSocket initialized.");
            System.out.println("Waiting for client connection..");

            /* Wait for connections. */
            while (true) {
                Socket client = server.accept();
                
                String clientIP = client.getInetAddress().getHostAddress();
                long currentTime = System.currentTimeMillis();
                if (!intervalLimit.containsKey(clientIP) || (currentTime - intervalLimit.get(clientIP) > INTERVAL)) {
                    /* Update the time record */
                    intervalLimit.put(clientIP, currentTime);
                    
                    /* Assign a worker thread for this socket. */
                    threadPool.submit(new WorkerThread(client, fileList, serverList));
                } else {
                    /* Violation */
                    client.close();
                }
            }
        } catch (IOException ex) {
            logger.warning(ex.getMessage());
        }
    }
}