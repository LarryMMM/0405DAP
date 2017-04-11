package com.ezshare.server;

import java.net.Socket;

/**
 *
 * @author Wenhao Zhao
 */
public class WorkerThread extends Thread {

    private Socket client;
    private FileList fileList;
    private ServerList serverList;

    public WorkerThread(Socket client, FileList fileList, ServerList serverList) {
        this.client = client;
        this.fileList = fileList;
        this.serverList = serverList;
    }

    @Override
    public synchronized void run() {
        System.out.println("A socket is established!");
        /*
        
            JSON Message Processing...
            FileTemplate Operations...
            Passive ServerList Exchange Receiver...
            Active Query Relay Sender...
            TO-DO!
        
         */
    }
}
