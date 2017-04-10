package com.ezshare.message;

/**
 * Base Class of All Messages
 * Created by jason on 10/4/17.
 */
public abstract class Message {

    private final String command;

    public Message(String command){
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public abstract String validator();

}
