package com.ezshare.message;

/**
 * Encapsulation of Remove Command.
 * Created by jason on 11/4/17.
 */
public class RemoveMessage extends Message{

    private final ResourceTemplate resource;

    public RemoveMessage(ResourceTemplate resource){
        super("REMOVE");
        this.resource = resource;
    }

    public ResourceTemplate getResource() {
        return resource;
    }

    @Override
    public boolean validator() {
        return false;
    }

}
