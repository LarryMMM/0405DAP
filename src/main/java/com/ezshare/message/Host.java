package com.ezshare.message;

/**
 * Encapsulation of Hostname and port #
 * Created by jason on 9/4/17.
 */
public class Host {
    private final String hostname;
    private final Integer port;

    public Host() {
        this.hostname = "localhost";
        this.port = 3780;
    }


    public Host(String hostname, Integer port) {
        this.hostname = hostname;
        this.port = port;
    }

    public Integer getPort() {
        return port;
    }

    public String getHostname() {
        return hostname;
    }
}
