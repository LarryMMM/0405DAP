package com.ezshare.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;

import com.ezshare.log.LogCustomFormatter;
import com.ezshare.message.*;
import com.google.gson.Gson;


/**
 *
 * @author Yuqing Liu
 */
public class WorkerThread extends Thread {

    private Socket client;
    private FileList fileList;
    private ServerList serverList;
    private InputStream w_input;
    private OutputStream w_output;
    public static final Logger logger = LogCustomFormatter.getLogger(ServerInstance.class.getName());

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
			w_output = client.getOutputStream();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DataOutputStream output = new DataOutputStream(w_output);
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
				procQueryCommand(queryMessage,fileList,serverList,output);
				break;
			case "SHARE":
				System.out.println("command type: SHARE");
				ShareMessage shareMessage = gson.fromJson(JSON, ShareMessage.class);
				procShareCommand(shareMessage, fileList,output);
				break;
			case "PUBLISH":
				System.out.println("command type: PUBLISH");
				PublishMessage publishMessage = gson.fromJson(JSON, PublishMessage.class);
				procPublishCommand(publishMessage,fileList,output);
				break;
			case "REMOVE":
				System.out.println("command type: REMOVE");
				RemoveMessage removeMessage = gson.fromJson(JSON, RemoveMessage.class);
				procRemoveCommand(removeMessage,fileList,output);
				break;
			case "EXCHANGE":
				System.out.println("command type: EXCHANGE");
				ExchangeMessage exchangeMessage = gson.fromJson(JSON, ExchangeMessage.class);
				procExchangeCommand(exchangeMessage,serverList,output);
				break;
			case "FETCH":
				System.out.println("command type: FETCH");
				FetchMessage fetchMessage = gson.fromJson(JSON, FetchMessage.class);
				procFetchCommand(fetchMessage,fileList,output);
				break;
			default:
				break;		
			
			}		
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        closeAll();
        
    }
    
    private void procPublishCommand(PublishMessage publishMessage, FileList fileList,DataOutputStream output){
    	ResourceTemplate resourceTemplate = publishMessage.getResource();
    	Map<String, String> responMessage = new HashMap<String, String>();
    	Gson gson = new Gson();
    	String JSON="";
    	//if the resource described by client does not exist
    	if(resourceTemplate.equals(null)){
    		responMessage.put("response", "error");
    		responMessage.put("errorMessage", "missing resource");
    		JSON = gson.toJson(responMessage);
    		try {
				output.writeUTF(JSON);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	else{
    		//if the resource is not valid
    		if(!publishMessage.isValid()){
    			responMessage.put("response", "error");
        		responMessage.put("errorMessage", "invalid resource");
        		JSON = gson.toJson(responMessage);
        		try {
					output.writeUTF(JSON);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    		else{
    			// successfully publish a resource
    			resourceTemplate.setEzserver(ServerInstance.HOST+":"+ServerInstance.PORT);
    			if(fileList.add(resourceTemplate)){
    				responMessage.put("response", "success");
    				JSON = gson.toJson(responMessage);
    				try {
						output.writeUTF(JSON);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    			//if the resource contradicts with another resource(same channel,same URI but different owner)
    			else{
    				responMessage.put("response", "error");
            		responMessage.put("errorMessage", "cannot publish resource");
            		JSON = gson.toJson(responMessage);
            		try {
						output.writeUTF(JSON);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    		}
    	}
    }
    
    
    private void procShareCommand(ShareMessage shareMessage, FileList fileList,DataOutputStream output){
    	ResourceTemplate resourceTemplate = shareMessage.getResource();
    	Map<String, String> responMessage = new HashMap<String, String>();
    	Gson gson = new Gson();
    	String JSON="";
    	//if the resource described by client does not exist
    	if(resourceTemplate.equals(null)||shareMessage.getSecret().equals(null)){    		
    		responMessage.put("response", "error");
    		responMessage.put("errorMessage", "missing resource and/or secret");
    		JSON = gson.toJson(responMessage);
    		try {
				output.writeUTF(JSON);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	else{
    		//if the resource is not valid
    		if(!shareMessage.isValid()){
    			responMessage.put("response", "error");
        		responMessage.put("errorMessage", "invalid resource");
        		JSON = gson.toJson(responMessage);
        		try {
					output.writeUTF(JSON);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    		else{
    			if(shareMessage.getSecret()!=ServerInstance.SECRET){
    				responMessage.put("response", "error");
            		responMessage.put("errorMessage", "incorrect secret");
            		JSON = gson.toJson(responMessage);
            		try {
						output.writeUTF(JSON);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			} 
    			else{
    				// successfully publish a resource
    				resourceTemplate.setEzserver(ServerInstance.HOST+":"+ServerInstance.PORT);
        			if(fileList.add(resourceTemplate)){
        				responMessage.put("response", "success");
        				JSON = gson.toJson(responMessage);
        				try {
    						output.writeUTF(JSON);
    					} catch (IOException e) {
    						// TODO Auto-generated catch block
    						e.printStackTrace();
    					}
        			}
        			//if the resource contradicts with another resource(same channel,same URI but different owner)
        			else{
        				responMessage.put("response", "error");
                		responMessage.put("errorMessage", "cannot share resource");
                		JSON = gson.toJson(responMessage);
                		try {
    						output.writeUTF(JSON);
    					} catch (IOException e) {
    						// TODO Auto-generated catch block
    						e.printStackTrace();
    					}
        			}
    				
    			}
    			    			
    			
    		}
    	}
    	
    } 
    private void procRemoveCommand(RemoveMessage removeMessage, FileList fileList,DataOutputStream output){
    	ResourceTemplate resourceTemplate = removeMessage.getResource();
    	Map<String, String> responMessage = new HashMap<String, String>();
    	Gson gson = new Gson();
    	String JSON="";
    	//if the resource described by client does not exist
    	if(resourceTemplate.equals(null)){
    		responMessage.put("response", "error");
    		responMessage.put("errorMessage", "missing resource");
    		JSON = gson.toJson(responMessage);
    		try {
				output.writeUTF(JSON);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	else{
    		//if the resource is not valid
    		if(!removeMessage.isValid()){
    			responMessage.put("response", "error");
        		responMessage.put("errorMessage", "invalid resource");
        		JSON = gson.toJson(responMessage);
        		try {
					output.writeUTF(JSON);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    		else{
    			// successfully remove a resource
    			if(fileList.remove(resourceTemplate)){
    				responMessage.put("response", "success");
    				JSON = gson.toJson(responMessage);
    				try {
						output.writeUTF(JSON);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    			else{
    				responMessage.put("response", "error");
            		responMessage.put("errorMessage", "cannot remove resource");
            		JSON = gson.toJson(responMessage);
            		try {
						output.writeUTF(JSON);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    		}
    	}
    } 
    private void procQueryCommand(QueryMessage queryMessage, FileList fileList,ServerList serverList,DataOutputStream output){
    	ResourceTemplate resourceTemplate = queryMessage.getResourceTemplate();
    	Map<String, String> responMessage = new HashMap<String, String>();
    	Gson gson = new Gson();
    	String JSON="";
    	//if the resource does not exist
    	if(resourceTemplate.equals(null)){
    		responMessage.put("response", "error");
    		responMessage.put("errorMessage", "missing resourceTemplate");
    		JSON = gson.toJson(responMessage);
    		try {
				output.writeUTF(JSON);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	else{
    		//if the resource is not valid
    		if(!queryMessage.isValid()){
    			responMessage.put("response", "error");
        		responMessage.put("errorMessage", "invalid resourceTemplate");
        		JSON = gson.toJson(responMessage);
        		try {
					output.writeUTF(JSON);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    		else{
    			// successfully fetch a resource
    			List<ResourceTemplate> result = new ArrayList<>();
    			if(queryMessage.isRelay()){
    				List<Host> hostList = serverList.getServerList();
    				int TIME_OUT = 3000;
    				//send query to servers in list
    				for(Host host: hostList){
    					Socket socket = new Socket();
    					try {
							socket.connect(new InetSocketAddress(host.getHostname(),host.getPort()),TIME_OUT);
							/* Set timeout for read() (also readUTF()!), throwing SocketTimeoutException */
			                socket.setSoTimeout(TIME_OUT);
						} catch (ConnectException ex) {
			                ServerInstance.logger.warning(host.toString() + " connection timeout");
			                serverList.removeServer(host);
			            } catch (SocketTimeoutException ex) {
			                ServerInstance.logger.warning(host.toString() + " readUTF() timeout");
			                serverList.removeServer(host);
			            } catch (IOException ex) {
			                /* Unclassified exception */
			                ServerInstance.logger.warning(host.toString() + " IOException");
			                serverList.removeServer(host);
			            }    					
    		            try {    		            	
    		            	
							DataOutputStream s_output = new DataOutputStream(socket.getOutputStream());
							ResourceTemplate queryRT = queryMessage.getResourceTemplate();
							queryRT.setChannel("");
							queryRT.setOwner("");
							QueryMessage relayQueryMessage = new QueryMessage(queryRT,false);
							JSON = gson.toJson(relayQueryMessage);
							s_output.writeUTF(JSON);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    		            
    		            
    		            DataInputStream input = null;
						try {
							input = new DataInputStream(socket.getInputStream());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    		            String response="";
						try {
							response = input.readUTF();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    		            //receive response
    		            if(response.contains("success")){
    		                //if success 
    		                try {
								response = input.readUTF();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} //discard success message
    		                while (!response.contains("resultSize")){
    		                    //print out resources
    		                    ResourceTemplate r = gson.fromJson(response,ResourceTemplate.class);
    		                    result.add(r);
    		                    try {
									response = input.readUTF();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
    		                }
    		                
    		            }else if(response.contains("error")){
    		                //when error occur
    		                logger.warning("RECEIVED:"+response);
    		            }
    		            try {
							socket.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    		        }
    				
    				
    				responMessage.put("response", "success");
    				JSON = gson.toJson(responMessage);
    				try {
						output.writeUTF(JSON);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    				for(ResourceTemplate rt: result){
    					JSON = gson.toJson(rt);
        				try {
							output.writeUTF(JSON);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    				}
    				
    				try {
						output.writeUTF("{\"resultSize\" : "+result.size()+"}");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    				
    				
    			} 
    			
    		
    		else {
    				List<ResourceTemplate> queryList = fileList.query(resourceTemplate);
        			int resultSize = queryList.size();
        			responMessage.put("response", "success");
    				JSON = gson.toJson(responMessage);
    				try {
    					output.writeUTF(JSON);
    				} catch (IOException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
        			if(resultSize==0){
        				try {
    						output.writeUTF("{\"resultSize\" : "+resultSize+"}");
    					} catch (IOException e) {
    						// TODO Auto-generated catch block
    						e.printStackTrace();
    					}
        			}
        			else{
        				for(ResourceTemplate rt : queryList){    					
        					if(!rt.getOwner().isEmpty()){
        						rt.setOwner("");
        					}   
        					String resource = gson.toJson(rt);
        					try {
    							output.writeUTF(resource);
    						} catch (IOException e) {
    							// TODO Auto-generated catch block
    							e.printStackTrace();
    						}
        				}
        				try {
    						output.writeUTF("{\"resultSize\" : "+resultSize+"}");
    					} catch (IOException e) {
    						// TODO Auto-generated catch block
    						e.printStackTrace();
    					}
        			}
    			}
    			
    			
    			
    		
    		}
    	}
    }
    private void procFetchCommand(FetchMessage fetchMessage, FileList fileList,DataOutputStream output){
    	ResourceTemplate resourceTemplate = fetchMessage.getResource();
    	Map<String, String> responMessage = new HashMap<String, String>();
    	Gson gson = new Gson();
    	String JSON="";
    	//if the resource does not exist
    	if(resourceTemplate.equals(null)){
    		responMessage.put("response", "error");
    		responMessage.put("errorMessage", "missing resourceTemplate");
    		JSON = gson.toJson(responMessage);
    		
    	}
    	else{
    		//if the resource is not valid
    		if(!fetchMessage.isValid()){
    			responMessage.put("response", "error");
        		responMessage.put("errorMessage", "invalid resourceTemplate");
        		JSON = gson.toJson(responMessage);
        		try {
					output.writeUTF(JSON);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    		else{
    			// to fetch a resource
    			List<ResourceTemplate> queryList = fileList.query(resourceTemplate);
    			int resultSize = queryList.size();
    			responMessage.put("response", "success");
				JSON = gson.toJson(responMessage);
				try {
					output.writeUTF(JSON);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//if there is not a file described by client existing in this server
    			if(resultSize==0){
    				JSON = "{\"resultSize\" : "+resultSize+"}";
    				try {
						output.writeUTF(JSON);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    			//send the file to client
    			else{
    				ResourceTemplate rt = queryList.get(0);
					if(!rt.getOwner().isEmpty()){
						rt.setOwner("");
					}
					String resource = gson.toJson(rt);
					try {
						output.writeUTF(resource);
					} catch (IOException e) {
						e.printStackTrace();
					}
					sendFile(rt.getUri(), output);    	
    				JSON = "{\"resultSize\" : "+resultSize+"}";
    				try {
						output.writeUTF(JSON);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    				
    			}
    			
    		}
    	}
    }
    
    
    private void procExchangeCommand(ExchangeMessage exchangeMessage,ServerList serverList,DataOutputStream output ){
    	List<Host> hostList = exchangeMessage.getServerList();
    	Map<String, String> responMessage = new HashMap<String, String>();
    	Gson gson = new Gson();
    	String JSON="";
    	//if the resource does not exist
    	if(hostList.equals(null)){
    		responMessage.put("response", "error");
    		responMessage.put("errorMessage", "missing server list");
    		JSON = gson.toJson(responMessage);
    		try {
				output.writeUTF(JSON);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	else{
    		//if the resource is not valid
    		if(!exchangeMessage.isValid()){
    			responMessage.put("response", "error");
        		responMessage.put("errorMessage", "missing or invalid server list");
        		JSON = gson.toJson(responMessage);
        		try {
					output.writeUTF(JSON);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
        		
    		}
    		else{
    			// successfully fetch a resource
    			responMessage.put("response", "success");
				JSON = gson.toJson(responMessage);
				try {
					output.writeUTF(JSON);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			serverList.updateServerList(hostList);
    			
    				
    			
    			
    		}
    	}
    }
    
    private void sendFile(String uri, DataOutputStream output ){
    	File f = new File(uri);
		if(f.exists()){
						
			try {			
				
				// Start sending file
				RandomAccessFile byteFile = new RandomAccessFile(f,"r");
				byte[] sendingBuffer = new byte[1024*1024];
				int num;
				// While there are still bytes to send..
				while((num = byteFile.read(sendingBuffer)) > 0){
					output.write(Arrays.copyOf(sendingBuffer, num));
				}
				byteFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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