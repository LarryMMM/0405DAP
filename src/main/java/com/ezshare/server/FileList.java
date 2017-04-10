package com.ezshare.server;

import com.ezshare.message.File;
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
    private List<File> fileList = new ArrayList<>();

    public void add() {

    }

    public void remove() {

    }

    public void update() {

    }

    public void query() {

    }
}
