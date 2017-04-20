package EZShare.server;

import EZShare.message.ResourceTemplate;

import java.util.ArrayList;
import java.util.List;
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
     * @param query  Resource in query.
     * @return querylist    List of resources that match the query.
     *
     * */
    public List<ResourceTemplate> query(ResourceTemplate query) {
        lock.readLock().lock();
        List<ResourceTemplate> queryList = new ArrayList<>();
        try{
            for(ResourceTemplate candidate : resourceTemplateList) {
                if (query.match(candidate)) {
                    queryList.add(candidate);
                }
            }
            return queryList;
        }finally {
            lock.readLock().unlock();
        }
    }
    
    /*
        I guess the query rule of "fetch" is different from that of "query"?
    */
    public List<ResourceTemplate> fetch(ResourceTemplate query) {
        lock.readLock().lock();
        List<ResourceTemplate> fetch = new ArrayList<>();
        try{
            for(ResourceTemplate candidate : resourceTemplateList) {
                if (query.getChannel().equals(candidate.getChannel()) && 
                    query.getUri().equals(candidate.getUri())) {
                    fetch.add(candidate);
                    return fetch;
                }
            }
            return fetch;
        }finally {
            lock.readLock().unlock();
        }
    }    
}