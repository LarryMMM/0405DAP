package com.ezshare.message;

import java.util.List;

/**
 * Encapsulation of Exchange Command.
 * Created by jason on 11/4/17.
 */
public class ExchangeMessage extends Message{

    private final List<Host> serverList;

    public ExchangeMessage(List<Host> serverList){
        super("EXCHANGE");
        this.serverList = serverList;
    }

    public List<Host> getServerList() {
        return serverList;
    }

    @Override
    public boolean validator() {
        return false;
    }
}
