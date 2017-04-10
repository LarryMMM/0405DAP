package com.ezshare.message;

import org.apache.commons.cli.*;
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

    public ResourceTemplate(CommandLine line) throws URISyntaxException {
        this.channel = line.getOptionValue("channel","");
        this.name = line.getOptionValue("name","");
        this.tags = (line.hasOption("tags"))?line.getOptionValue("tags").split(","):null;
        this.description = line.getOptionValue("description","");
        //URI check
        this.uri = (line.hasOption("uri"))?new URI(line.getOptionValue("uri")).toString():"";
        this.owner = line.getOptionValue("owner","");
        this.ezserver = line.getOptionValue("ezserver","");
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
        return name;
    }
}
