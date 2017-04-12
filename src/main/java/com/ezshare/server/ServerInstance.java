package com.ezshare.server;

import com.ezshare.log.LogCustomFormatter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

    private static final FileList fileList = new FileList();
    private static final ServerList serverList = new ServerList();
    
    /*
        Currently it is a simple fixed-volume thread pool.
        If no thread resource is available at the moment, it would be blocked until it could get one.
    */
    private static ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREAD_COUNT);
    
    public static final Logger logger = LogCustomFormatter.getLogger(ServerInstance.class.getName());

    
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
                
                /* Assign a worker thread for this socket. */
                threadPool.submit(new WorkerThread(client, fileList, serverList));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
