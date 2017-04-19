package com.ezshare.server;

import com.ezshare.message.FileTemplate;
import com.ezshare.message.ResourceTemplate;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author Ying Li
 */
public class FileList {

    private List<ResourceTemplate> resourceTemplateList = new ArrayList<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * add a new file to filelist
     *
     * @param resourceTemplate Resource to be added.
     * @return boolean  Whether the resource is successfully added.
     *
     * */
    public boolean add(ResourceTemplate resourceTemplate) {
        lock.writeLock().lock();
        try{
            if(resourceTemplateList.isEmpty()){
                resourceTemplateList.add(resourceTemplate);
                return true;
            }

            else{
                for(int i = 0 ; i < resourceTemplateList.size(); i++) {
                    ResourceTemplate f = resourceTemplateList.get(i);
                    if(f.getChannel().equals(resourceTemplate.getChannel()) && f.getUri().equals(resourceTemplate.getUri())){
                        if(f.getOwner().equals(resourceTemplate.getOwner())){
                            resourceTemplateList.set(i, resourceTemplate);
                            return true;
                        }
                        return false;
                    }

                }
                resourceTemplateList.add(resourceTemplate);
                return true;
            }
        }finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * delete a file from filelist
     *
     *
     * @param resourceTemplate  Resource to be removed.
     * @return boolean  Whether the resource is successfully removed.
     *
     * */
    public boolean remove(ResourceTemplate resourceTemplate) {
        lock.writeLock().lock();
        try{
            for(int i = 0 ; i < resourceTemplateList.size(); i++) {
                ResourceTemplate f = resourceTemplateList.get(i);
                if (f.getChannel().equals(resourceTemplate.getChannel()) && f.getUri().equals(resourceTemplate.getUri()) && f.getOwner().equals(resourceTemplate.getOwner())) {
                    resourceTemplateList.remove(i);
                    return true;
                }
            }
            return false;
        }finally {
            lock.writeLock().unlock();
        }
    }


    /**
     * search a certain list of file by owner, uri and channel in filelist
     *
     *
     * @param resourceTemplate  Resource in query.
     * @return querylist    List of resources that match the query.
     *
     * */
    public List<ResourceTemplate> query(ResourceTemplate resourceTemplate) {
        lock.readLock().lock();
        List<ResourceTemplate> queryList = new ArrayList<>();
        try{
            for(ResourceTemplate f : resourceTemplateList) {
                if (resourceTemplate.match(f)) {
                    queryList.add(f);
                }
            }
            return queryList;
        }finally {
            lock.readLock().unlock();
        }
    }
    
    
}
