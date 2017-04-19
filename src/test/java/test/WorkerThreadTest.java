/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import com.ezshare.server.FileList;
import com.ezshare.server.ServerList;
import com.ezshare.server.WorkerThread;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author nek
 */
public class WorkerThreadTest {

    @Test
    public void testRun() {
        try {
            WorkerThread w = new WorkerThread(new Socket(), new FileList(), new ServerList());
            List<String> outputJsons = w.reception("{command:\"haha\"}");
            Assert.assertEquals(1, outputJsons.size());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
