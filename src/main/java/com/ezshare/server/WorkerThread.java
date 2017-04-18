package com.ezshare.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ezshare.message.*;
import com.google.gson.Gson;


/**
 *
 * @author Wenhao Zhao
 */
public class WorkerThread extends Thread {

    private Socket client;
    private FileList fileList;
    private ServerList serverList;
    private InputStream w_input;
    private OutputStream w_output;

    public WorkerThread(Socket client, FileList fileList, ServerList serverList) {
        this.client = client;
        this.fileList = fileList;
        this.serverList = serverList;
    }

    @Override
    public synchronized void run() {
        System.out.println("A socket is established!");
        /*
        
            JSON Message Processing...
            FileTemplate Operations...
            Passive ServerList Exchange Receiver...
            Active Query Relay Sender...
            TO-DO!
        
         */
        try {
			w_input = client.getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        DataInputStream input = new DataInputStream(w_input);
        try {
			String JSON = input.readUTF();
			//remove all '\0'
			JSON = JSON.replaceAll("\0{1,}", "");
			Gson gson = new Gson();
			Message message = gson.fromJson(JSON, Message.class);
			switch(message.getCommand()){
			case "QUERY":
				System.out.println("command type: QUERY");
				QueryMessage queryMessage = gson.fromJson(JSON, QueryMessage.class);
				JSON = procQueryCommand(queryMessage,fileList);
				break;
			case "SHARE":
				System.out.println("command type: SHARE");
				break;
			case "PUBLISH":
				System.out.println("command type: PUBLISH");
				PublishMessage publishMessage = gson.fromJson(JSON, PublishMessage.class);
				JSON = procPublishCommand(publishMessage,fileList);
				break;
			case "REMOVE":
				System.out.println("command type: REMOVE");
				RemoveMessage removeMessage = gson.fromJson(JSON, RemoveMessage.class);
				JSON = procRemoveCommand(removeMessage,fileList);
				break;
			case "EXCHANGE":
				System.out.println("command type: EXCHANGE");
				break;
			case "FETCH":
				System.out.println("command type: FETCH");
				FetchMessage fetchMessage = gson.fromJson(JSON, FetchMessage.class);
				JSON = procFetchCommand(fetchMessage,fileList);
				break;
			default:
				break;		
			
			}
			
			w_output = client.getOutputStream();
			DataOutputStream output = new DataOutputStream(w_output);
			output.writeUTF(JSON);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        closeAll();
        
    }
    
    private String procPublishCommand(PublishMessage publishMessage, FileList fileList){
    	ResourceTemplate resourceTemplate = publishMessage.getResource();
    	Map<String, String> responMessage = new HashMap<String, String>();
    	Gson gson = new Gson();
    	String JSON;
    	//if the resource described by client does not exist
    	if(resourceTemplate.equals(null)){
    		responMessage.put("response", "error");
    		responMessage.put("errorMessage", "missing resource");
    		JSON = gson.toJson(responMessage);
    		return JSON;
    	}
    	else{
    		//if the resource is not valid
    		if(publishMessage.isValid()){
    			responMessage.put("response", "error");
        		responMessage.put("errorMessage", "invalid resource");
        		JSON = gson.toJson(responMessage);
        		return JSON;
    		}
    		else{
    			// successfully publish a resource
    			if(fileList.add(resourceTemplate)){
    				responMessage.put("response", "success");
    				JSON = gson.toJson(responMessage);
            		return JSON;
    			}
    			//if the resource contradicts with another resource(same channel,same URI but different owner)
    			else{
    				responMessage.put("response", "error");
            		responMessage.put("errorMessage", "cannot publish resource");
            		JSON = gson.toJson(responMessage);
            		return JSON;
    			}
    		}
    	}
    }
    
    
    private void procShareCommand(ShareMessage shareMessage){
    	ResourceTemplate resourceTemplate = shareMessage.getResource();
    } 
    private String procRemoveCommand(RemoveMessage removeMessage, FileList fileList){
    	ResourceTemplate resourceTemplate = removeMessage.getResource();
    	Map<String, String> responMessage = new HashMap<String, String>();
    	Gson gson = new Gson();
    	String JSON;
    	//if the resource described by client does not exist
    	if(resourceTemplate.equals(null)){
    		responMessage.put("response", "error");
    		responMessage.put("errorMessage", "missing resource");
    		JSON = gson.toJson(responMessage);
    		return JSON;
    	}
    	else{
    		//if the resource is not valid
    		if(removeMessage.isValid()){
    			responMessage.put("response", "error");
        		responMessage.put("errorMessage", "invalid resource");
        		JSON = gson.toJson(responMessage);
        		return JSON;
    		}
    		else{
    			// successfully remove a resource
    			if(fileList.remove(resourceTemplate)){
    				responMessage.put("response", "success");
    				JSON = gson.toJson(responMessage);
            		return JSON;
    			}
    			else{
    				responMessage.put("response", "error");
            		responMessage.put("errorMessage", "cannot remove resource");
            		JSON = gson.toJson(responMessage);
            		return JSON;
    			}
    		}
    	}
    } 
    private String procQueryCommand(QueryMessage queryMessage, FileList fileList){
    	ResourceTemplate resourceTemplate = queryMessage.getResourceTemplate();
    	Map<String, String> responMessage = new HashMap<String, String>();
    	Gson gson = new Gson();
    	String JSON;
    	//if the resource does not exist
    	if(resourceTemplate.equals(null)){
    		responMessage.put("response", "error");
    		responMessage.put("errorMessage", "missing resourceTemplate");
    		JSON = gson.toJson(responMessage);
    		return JSON;
    	}
    	else{
    		//if the resource is not valid
    		if(queryMessage.isValid()){
    			responMessage.put("response", "error");
        		responMessage.put("errorMessage", "invalid resourceTemplate");
        		JSON = gson.toJson(responMessage);
        		return JSON;
    		}
    		else{
    			// successfully fetch a resource
    			List<ResourceTemplate> queryList = fileList.query(resourceTemplate);
    			int resultSize = queryList.size();
    			responMessage.put("response", "success");
				JSON = gson.toJson(responMessage);
    			if(resultSize==0){
    				JSON = JSON + "{\"resultSize\" : "+resultSize+"}";
    				return JSON;
    			}
    			else{
    				for(ResourceTemplate rt : queryList){    					
    					String resource = gson.toJson(rt);
    					FileTemplate ft = (FileTemplate)rt;
    					int resourceSize = ft.getResourceSize();
    					if(!rt.getOwner().isEmpty()){
    						resource.replaceAll("\"owner\"( )?:( )?\"[A-Za-z0-9]{1,}\"", "\"owner\" : \"*\"");
    					}    					
    					JSON += resource;
    				}
    				JSON = JSON + "{\"resultSize\" : "+resultSize+"}";
    				return JSON;
    			}
    			
    		}
    	}
    }
    private String procFetchCommand(FetchMessage fetchMessage, FileList fileList){
    	ResourceTemplate resourceTemplate = fetchMessage.getResource();
    	Map<String, String> responMessage = new HashMap<String, String>();
    	Gson gson = new Gson();
    	String JSON;
    	//if the resource does not exist
    	if(resourceTemplate.equals(null)){
    		responMessage.put("response", "error");
    		responMessage.put("errorMessage", "missing resourceTemplate");
    		JSON = gson.toJson(responMessage);
    		return JSON;
    	}
    	else{
    		//if the resource is not valid
    		if(fetchMessage.isValid()){
    			responMessage.put("response", "error");
        		responMessage.put("errorMessage", "invalid resourceTemplate");
        		JSON = gson.toJson(responMessage);
        		return JSON;
    		}
    		else{
    			// successfully fetch a resource
    			List<ResourceTemplate> queryList = fileList.query(resourceTemplate);
    			int resultSize = queryList.size();
    			responMessage.put("response", "success");
				JSON = gson.toJson(responMessage);
    			if(resultSize==0){
    				JSON = JSON + "{\"resultSize\" : "+resultSize+"}";
    				return JSON;
    			}
    			else{
    				for(ResourceTemplate rt : queryList){    					
    					String resource = gson.toJson(rt);
    					if(!rt.getOwner().isEmpty()){
    						resource.replaceAll("\"owner\"( )?:( )?\"[A-Za-z0-9]{1,}\"", "\"owner\" : \"*\"");
    					}    					
    					JSON += resource;
    				}
    				JSON = JSON + "{\"resultSize\" : "+resultSize+"}";
    				return JSON;
    			}
    			
    		}
    	}
    }
    private void procExchangeCommand(){
    	
    }
    
    private void closeAll() {
		if (w_input != null) {
			try {
				w_input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			w_input = null;
		}
		if (w_output != null) {
			try {
				w_output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			w_output = null;
		}
		if (client != null) {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			client = null;
		}
	}
}