package com.ezshare.server;

import java.net.Socket;

/**
 *
 * @author Wenhao Zhao
 */
public class WorkerThread extends Thread {

    private Socket client;
    private FileList fileList;

    public WorkerThread(Socket client, FileList fileList) {
        this.client = client;
        this.fileList = fileList;
    }

    @Override
    public synchronized void run() {
        System.out.println("A socket is established!");
        /*
        
            TO-DO!
        
         */
    }
}
