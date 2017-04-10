package com.ezshare.message;

/**
 * Encapsulation of Fetch Command.
 * Created by jason on 11/4/17.
 */
public class FetchMessage extends Message{
    private final ResourceTemplate resourceTemplate;

    public FetchMessage(ResourceTemplate resource){
        super("FETCH");
        this.resourceTemplate = resource;
    }

    public ResourceTemplate getResource() {
        return resourceTemplate;
    }

    @Override
    public String validator() {
        return null;
    }
}
