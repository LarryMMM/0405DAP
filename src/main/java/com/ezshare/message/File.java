package com.ezshare.message;

import java.net.URISyntaxException;

/**
 *
 * @author Wenhao Zhao
 */
public class File extends ResourceTemplate {
    /*
        
        TO-DO!
        
     */
    private int resourceSize;

    public File(String channel, String name, String[] tags, String description, String uri, String owner, String ezserver) throws URISyntaxException {
        super(channel, name, tags, description, uri, owner, ezserver);
        this.resourceSize = 0;
    }
    
}
