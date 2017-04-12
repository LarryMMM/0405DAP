package com.ezshare.message;


/**
 * Encapsulation of Publish Command.
 * Created by jason on 10/4/17.
 */
public class PublishMessage extends Message{

    private final ResourceTemplate resource;

    public PublishMessage(ResourceTemplate resource){
        super("PUBLISH");
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
