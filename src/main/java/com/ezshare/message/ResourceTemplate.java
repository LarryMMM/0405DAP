package com.ezshare.message;

import com.google.gson.Gson;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Encapsulation of Query Message.
 * Created by jason on 10/4/17.
 */
public class ResourceTemplate extends Validatable {
    private final String name;
    private final String[] tags;
    private final String description;
    private final String uri;
    private final String channel;
    private final String owner;
    private final String ezserver;

    public ResourceTemplate(String channel, String name, String[] tags,String description, String uri,String owner,String ezserver){
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

    /**
     * Check whether the uri is valid for publish or query.
     * @return  Validation.
     */
    public boolean isValidUri(){
        try{
            URI u = new URI(this.uri);
            return (u.getScheme()!=null&&!u.getScheme().equals("file")&&u.getAuthority()!=null&&u.isAbsolute());}
        catch (URISyntaxException e){
            return false;
        }
    }

    /**
     * Check whether the uri is valid for share.
     * @return  Validation.
     */
    public boolean isValidFile(){
        try {
            URI u = new URI(this.uri);
            return (u.getScheme().equals("file")&&u.getPath()!=null&&u.isAbsolute());
        }catch (URISyntaxException e){
            return false;
        }
    }

    @Override
    public boolean isValid() {
       return owner!=null&&owner.equals("*");
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }


    /**
     * Check if two resourceTemplate objects match in the query.
     * @param list  Resource in list.
     * @return  Match or not
     */

    public boolean match(ResourceTemplate list){

        return string_match(this.getName(),list.getName())
                &&string_match(this.getOwner(),list.getOwner())
                &&string_match(this.getChannel(),list.getChannel())
                &&string_match(this.getUri(),list.getUri())
                &&string_match(this.getDescription(),list.getDescription());
    }

    private static boolean string_match(String s1,String s2){
        return (s1==null||s2==null||s1.equals("")||s2.equals("")||s1.equals(s2));
    }


}
