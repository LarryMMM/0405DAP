package com.ezshare.message;

import com.google.gson.Gson;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Encapsulation of Query Message.
 * Created by jason on 10/4/17.
 */
public class ResourceTemplate {
    private final String name;
    private final String[] tags;
    private final String description;
    private final String uri;
    private final String channel;
    private final String owner;
    private final String ezserver;

    public ResourceTemplate(String channel, String name, String[] tags,String description, String uri,String owner,String ezserver) throws URISyntaxException{
        this.channel = channel;
        this.name = name;
        this.tags = tags;
        this.description = description;
        this.uri = uri;
        this.owner = owner;
        this.ezserver = ezserver;
    }


    public String getChannel() {
        return channel;
    }

    public String getDescription() {
        return description;
    }

    public String getEzserver() {
        return ezserver;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public String getUri() {
        return uri;
    }

    public String[] getTag() {
        return tags;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
