package com.ezshare.message;

/**
 * Encapsulation of Query Command.
 * Created by jason on 9/4/17.
 */
public class QueryMessage extends Message {

    private final boolean relay;
    private final ResourceTemplate resourceTemplate;


    public QueryMessage(ResourceTemplate resourceTemplate,boolean relay){
        super("QUERY");
        this.resourceTemplate = resourceTemplate;
        this.relay = relay;
    }

    public ResourceTemplate getResourceTemplate() {
        return resourceTemplate;
    }

    public boolean isRelay() {
        return relay;
    }

    @Override
    public String validator() {
        return null;
    }
}
