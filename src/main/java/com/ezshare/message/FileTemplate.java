package com.ezshare.message;

import java.net.URISyntaxException;

/**
 *
 * @author Wenhao Zhao
 */
public class FileTemplate extends ResourceTemplate {
    /*
        
        TO-DO!
        
     */
    private final int resourceSize;

    public FileTemplate(String channel, String name, String[] tags, String description, String uri, String owner, String ezserver,int resourceSize) throws URISyntaxException {
        super(channel, name, tags, description, uri, owner, ezserver);
        this.resourceSize = resourceSize;
    }

    public int getResourceSize() {
        return resourceSize;
    }
}