package com.ezshare.message;

/**
 * Encapsulation of Share Command.
 * Created by jason on 11/4/17.
 */
public class ShareMessage extends Message{

    private final String secret;
    private final ResourceTemplate resource;


    public ShareMessage(ResourceTemplate resource,String secret){
        super("SHARE");
        this.resource = resource;
        this.secret = secret;
    }

    public ResourceTemplate getResource() {
        return resource;
    }

    public String getSecret() {
        return secret;
    }

    @Override
    public boolean validator() {
        return false;
    }
}
