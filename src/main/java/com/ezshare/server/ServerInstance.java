package com.ezshare.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ServerSocketFactory;

/**
 *
 * @author Wenhao Zhao
 */
public class ServerInstance {

    /* Configuration */
    private static int PORT = 3000;
    private static final int MAX_THREAD_COUNT = 10;

    private static FileList fileList = new FileList();
    private static ServerList serverList = new ServerList();
    
    /*
        Currently it is a simple fixed-volume thread pool.
        If no thread resource is available at the moment, it would be blocked until it could get one.
    */
    private static ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREAD_COUNT);

    public static void main(String[] args) {
        /*
        
            Timer & Active ServerList Exchange Sending...
            TO-DO!
        
         */
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
