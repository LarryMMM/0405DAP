package com.ezshare.server;

import com.ezshare.message.FileTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author Ying Li
 */
public class FileList {

    private List<FileTemplate> fileTemplateList = new ArrayList<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * add a new file to filelist
     *
     *
     * @param FileTemplate
     * @return boolean
     *
     * */
    public boolean add(FileTemplate fileTemplate) {
        lock.writeLock().lock();
        try{
            if(fileTemplateList.isEmpty()){
                fileTemplateList.add(fileTemplate);
                return true;
            }

            else{
                for(int i = 0 ; i < fileTemplateList.size(); i++) {
                    FileTemplate f = fileTemplateList.get(i);
                    if(f.getChannel().equals(fileTemplate.getChannel()) && f.getUri().equals(fileTemplate.getUri())){
                        if(f.getOwner().equals(fileTemplate.getOwner())){
                            fileTemplateList.set(i, fileTemplate);
                            return true;
                        }
                        return false;
                    }

                }
                fileTemplateList.add(fileTemplate);
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
     * @param FileTemplate
     * @return boolean
     *
     * */
    public boolean remove(FileTemplate fileTemplate) {
        lock.writeLock().lock();
        try{
            for(int i = 0 ; i < fileTemplateList.size(); i++) {
                FileTemplate f = fileTemplateList.get(i);
                if (f.getChannel().equals(fileTemplate.getChannel()) && f.getUri().equals(fileTemplate.getUri()) && f.getOwner().equals(fileTemplate.getOwner())) {
                    fileTemplateList.remove(i);
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
     * @param FileTemplate
     * @return querylist
     *
     * */
    public List<FileTemplate> query(FileTemplate fileTemplate) {
        lock.readLock().lock();
        List<FileTemplate> queryList = new ArrayList<>();
        try{
            for(FileTemplate f : fileTemplateList) {
                if (f.getChannel().equals(fileTemplate.getChannel()) && f.getUri().equals(fileTemplate.getUri()) && f.getOwner().equals(fileTemplate.getOwner())) {
                    queryList.add(f);
                }
            }
            return queryList;
        }finally {
            lock.readLock().unlock();
        }
    }
}
