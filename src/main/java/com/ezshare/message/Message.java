package com.ezshare.message;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Base Class of All Messages
 * Created by jason on 10/4/17.
 */
public abstract class Message extends Validatable {

    private final String command;

    public Message(String command){
        this.command = command;
    }

    public String getCommand() {
        return command;
    }


    /**
     * Check whether the uri is valid for publish or query.
     * @param uri   URI to check
     * @return  Validation.
     */
    private static boolean isValidUri(String uri){
        try{
            URI u = new URI(uri);
            return (u.getScheme()!=null&&!u.getScheme().equals("file")&&u.getAuthority()!=null&&u.isAbsolute());}
        catch (URISyntaxException e){
            return false;
        }
    }

    /**
     * Check whether the uri is valid for share.
     * @param uri   URI to check
     * @return  Validation.
     */
    private static boolean isValidFile(String uri){
        try {
            URI u = new URI(uri);
            return (u.getScheme().equals("file")&&u.getPath()!=null&&u.isAbsolute());
        }catch (URISyntaxException e){
            return false;
        }
    }
}
