package com.ezshare.server;

import com.ezshare.message.FileTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author Wenhao Zhao
 */
public class FileList {

    private ReadWriteLock rwl = new ReentrantReadWriteLock();
    private List<FileTemplate> fileTemplateList = new ArrayList<>();

    public void add() {
        /*
        
            TO-DO!
        
         */
    }

    public void remove() {
        /*
        
            TO-DO!
        
         */
    }

    public void update() {
        /*
        
            TO-DO!
        
         */
    }

    public void query() {
        /*
        
            TO-DO!
        
         */
    }
}
