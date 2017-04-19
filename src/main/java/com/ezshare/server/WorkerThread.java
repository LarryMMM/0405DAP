package com.ezshare.server;

import java.io.*;
import java.net.*;
import java.util.*;

import com.ezshare.message.*;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;



/**
 *
 * @author Yuqing Liu
 */
public class WorkerThread extends Thread {

    private Socket client;
    private DataOutputStream output;
    private DataInputStream input;
    private FileList fileList;
    private ServerList serverList;
    private String ClientAddress;
    private Gson gson = new Gson();

    /**
     * Initialize worker thread, create IO streams.
     * @param client    the socket.
     * @param fileList  reference of file list.
     * @param serverList    reference of server list.
     * @throws IOException  when unable to create IO streams.
     */
    public WorkerThread(Socket client, FileList fileList, ServerList serverList) throws IOException {
        this.client = client;
        this.client.setSoTimeout(3000);
        this.fileList = fileList;
        this.serverList = serverList;
        this.ClientAddress = client.getRemoteSocketAddress().toString();
        this.input = new DataInputStream(client.getInputStream());
        this.output = new DataOutputStream(client.getOutputStream());
    }

    @Override
    public synchronized void run() {
        try{
            ServerInstance.logger.info(this.ClientAddress+": Connected!");
            //remove \0 in order to prevent crashing.
            String JSON = input.readUTF().replace("\0","");

            Message message = gson.fromJson(JSON,Message.class);

            if(message.getCommand()==null){
                throw new JsonSyntaxException("missing command");
            }else {
                switch (message.getCommand()) {
                    case "PUBLISH":
                        processPublish(JSON);
                        break;
                    case "SHARE":
                        processShare(JSON);
                        break;
                    case "REMOVE":
                        processRemove(JSON);
                        break;
                    case "EXCHANGE":
                        processExchange(JSON);
                        break;
                    case "FETCH":
                        processFetch(JSON);
                        break;
                    case "QUERY":
                        processQuery(JSON);
                        break;
                    default:
                        ServerInstance.logger.warning(this.ClientAddress + ": invalid command");
                        sendMessage(getErrorMessage("invalid command"));
                }
            }

        }catch (JsonSyntaxException e){
            //cannot parse Json to Message object.
            ServerInstance.logger.warning(this.ClientAddress+": missing or incorrect type for command");
            sendMessage(getErrorMessage("missing or incorrect type for command"));
        }catch (SocketTimeoutException e){
            //Socket time out during communication.
            ServerInstance.logger.warning(this.ClientAddress+": Socket Timeout");
        }catch (IOException e){
            //Socket time out in establishing.
            ServerInstance.logger.warning(this.ClientAddress+": IOException!");}
        finally {
            try {
                //try to close socket any way.
                client.close();
                ServerInstance.logger.info(this.ClientAddress+": Disconnected!");
            }catch (IOException e){
                ServerInstance.logger.warning(this.ClientAddress+": Unable to disconnect!");}
            }
        }

    /**
     * Process publish request.
     * @param JSON  request
     */
    private void processPublish(String JSON){
        try {
            PublishMessage publishMessage = gson.fromJson(JSON,PublishMessage.class);

            if(publishMessage.getResource()==null){
                throw new JsonSyntaxException("missing resource");
            }else {
                ResourceTemplate r = publishMessage.getResource();
                r.setEzserver(ServerInstance.HOST);
                if (!publishMessage.isValid()) {
                    ServerInstance.logger.warning(this.ClientAddress + ": invalid resource");
                    sendMessage(getErrorMessage("invalid resource"));
                } else if (!fileList.add(r)) {
                    ServerInstance.logger.warning(this.ClientAddress + ": cannot publish resource");
                    sendMessage(getErrorMessage("cannot publish resource"));

                } else {
                    ServerInstance.logger.fine(this.ClientAddress + ": resource published!");
                    sendMessage(getSuccessMessage());
                }
            }

        }catch (JsonSyntaxException e){
            ServerInstance.logger.warning(this.ClientAddress+": missing resource");
            sendMessage(getErrorMessage("missing resource"));
        }
    }

    /**
     * Process remove request.
     * @param JSON  request
     */
    private void processRemove(String JSON){
        try {
            RemoveMessage removeMessage = gson.fromJson(JSON,RemoveMessage.class);
            if(removeMessage.getResource()==null) {
                throw new JsonSyntaxException("missing resource");
            }else {
                ResourceTemplate r = removeMessage.getResource();
                if (!removeMessage.isValid()) {
                    ServerInstance.logger.warning(this.ClientAddress + ": invalid resource");
                    sendMessage(getErrorMessage("invalid resource"));

                } else if (!fileList.remove(r)) {
                    ServerInstance.logger.warning(this.ClientAddress + ": cannot remove resource");
                    sendMessage(getErrorMessage("cannot publish resource"));

                } else {
                    ServerInstance.logger.fine(this.ClientAddress + ": resource removed!");
                    sendMessage(getSuccessMessage());
                }
            }

        }catch (JsonSyntaxException e){
            ServerInstance.logger.warning(this.ClientAddress+": missing resource");
            sendMessage(getErrorMessage("missing resource"));
        }
    }

    /**
     * Process share request.
     * @param JSON  request
     */
    private void processShare(String JSON){
        try {
            ShareMessage shareMessage = gson.fromJson(JSON,ShareMessage.class);

            if(shareMessage.getResource()==null||shareMessage.getSecret()==null){
                throw new JsonSyntaxException("missing secret and/or resource");
            }else {
                ResourceTemplate r = shareMessage.getResource();
                r.setEzserver(ServerInstance.HOST);
                if (!shareMessage.isValid()) {
                    //resource not valid
                    ServerInstance.logger.warning(this.ClientAddress + ": invalid resource");
                    sendMessage(getErrorMessage("invalid resource"));
                } else if (!shareMessage.getSecret().equals(ServerInstance.SECRET)) {
                    //secret incorrect
                    ServerInstance.logger.warning(this.ClientAddress + ": incorrect secret");
                    sendMessage(getErrorMessage("incorrect secret"));
                } else {
                    String message;
                    File f = new File(new URI(r.getUri()).getPath());
                    ServerInstance.logger.info(this.ClientAddress + ": request for sharing " + r.getUri());
                    if (f.exists()) {
                        //file exist
                        if (fileList.add(r)) {
                            //file successfully added
                            ServerInstance.logger.fine(this.ClientAddress + ": resource shared!");
                            message = getSuccessMessage();
                        } else {
                            //file exist but cannot be added
                            ServerInstance.logger.warning(this.ClientAddress + ": resource unable to be added!");
                            message = getErrorMessage("cannot share resource");
                        }
                    } else {
                        //file don't exist
                        ServerInstance.logger.warning(this.ClientAddress + ": resource does not exist");
                        message = getErrorMessage("cannot share resource");
                    }
                    sendMessage(message);
                }
            }


        }catch (JsonSyntaxException e){
            ServerInstance.logger.warning(this.ClientAddress+": missing resource and/or secret");
            sendMessage(getErrorMessage("missing resource and/or secret"));
        }catch (URISyntaxException e){
            ServerInstance.logger.warning(this.ClientAddress+": unable to create URI");
            sendMessage(getErrorMessage("cannot share resource"));
        }

    }

    /**
     * Process exchange request.
     * @param JSON  request
     */
    private void processExchange(String JSON){
        try {
            ExchangeMessage exchangeMessage = gson.fromJson(JSON,ExchangeMessage.class);

            if(exchangeMessage.getServerList()==null||exchangeMessage.getServerList().isEmpty()){
                throw new JsonSyntaxException("missing server");
            }else {

                List<Host> serverList = exchangeMessage.getServerList();

                if (exchangeMessage.isValid()) {
                    //all servers valid, add to server list.
                    this.serverList.updateServerList(serverList);
                    ServerInstance.logger.fine(this.ClientAddress + ": servers added");
                    sendMessage(getSuccessMessage());
                } else {
                    sendMessage(getErrorMessage("invalid server record"));
                }
            }
        }catch (JsonSyntaxException e){
            ServerInstance.logger.warning(this.ClientAddress+": missing or invalid server list");
            sendMessage(getErrorMessage("missing or invalid server list"));
        }

    }

    /**
     * Process plain query and query relay requests.
     * @param JSON  request
     */
    public void processQuery(String JSON){
        try {
            QueryMessage queryMessage = gson.fromJson(JSON,QueryMessage.class);
            if(queryMessage.getResourceTemplate()==null){
                throw new JsonSyntaxException("missing resource");
            }else {
                ResourceTemplate r = queryMessage.getResourceTemplate();

                ServerInstance.logger.info(client.getRemoteSocketAddress() + " querying for " + r.toString());

                if (!queryMessage.isValid()) {
                    ServerInstance.logger.warning(this.ClientAddress + ": invalid resourceTemplate");
                    sendMessage(getErrorMessage("invalid resourceTemplate"));
                } else if (!queryMessage.isRelay()) {
                    //relay is false,only query local resource
                    List<ResourceTemplate> result = this.fileList.query(r);

                    sendMessage(getSuccessMessage());

                    ServerInstance.logger.fine("Query Success");

                    if (!result.isEmpty()) {
                        for (ResourceTemplate rt : result) {
                            sendMessage(rt.toString());
                        }
                    }
                    sendMessage(getResultSize(((long) result.size())));
                } else {
                    //relay is true, query local resource first
                    List<ResourceTemplate> result = this.fileList.query(r);

                    QueryMessage relayMessage = gson.fromJson(JSON, QueryMessage.class);

                    relayMessage.setRelay(false);
                    relayMessage.getResourceTemplate().setOwner("");
                    relayMessage.getResourceTemplate().setChannel("");

                    //append result set by querying remote servers
                    for (Host h : this.serverList.getServerList()) {
                        List<ResourceTemplate> rtl = queryRelay(h, relayMessage);
                        result.addAll(rtl);
                    }

                    sendMessage(getSuccessMessage());
                    for (ResourceTemplate rt : result) {
                        sendMessage(rt.toString());
                    }
                    sendMessage(getResultSize(((long) result.size())));
                    ServerInstance.logger.fine("Query relay Success");
                }
            }

        }catch (JsonSyntaxException e){
            ServerInstance.logger.warning(this.ClientAddress+": missing resourceTemplate");
            sendMessage(getErrorMessage("missing resourceTemplate"));
        }

    }

    /**
     * Process fetch request.
     * @param JSON  request
     */
    private void processFetch(String JSON){
        try{
            FetchMessage fetchMessage = gson.fromJson(JSON,FetchMessage.class);
            if(fetchMessage.getResource()==null){
                throw new JsonSyntaxException("missing resource");
            }else {
                ResourceTemplate r = fetchMessage.getResource();

                if (!fetchMessage.isValid()) {
                    ServerInstance.logger.warning(this.ClientAddress + ": invalid resourceTemplate");
                    sendMessage(getErrorMessage("invalid resourceTemplate"));
                } else {
                    List<ResourceTemplate> result = this.fileList.query(r);

                    if (!result.isEmpty()) {
                        RandomAccessFile file = new RandomAccessFile(new File(new URI(r.getUri()).getPath()), "r");
                        //file is exist.
                        sendMessage(getSuccessMessage());
                        sendMessage(gson.toJson(new FileTemplate(result.get(0), file.length())));

                        byte[] sendingBuffer = new byte[1024 * 1024];
                        int num;
                        // While there are still bytes to send..
                        ServerInstance.logger.info(this.ClientAddress + ": start sending file " + r.getUri());
                        while ((num = file.read(sendingBuffer)) > 0) {
                            output.write(Arrays.copyOf(sendingBuffer, num));
                        }
                        file.close();
                        sendMessage(getResultSize((long) 1));
                        ServerInstance.logger.fine(this.ClientAddress + ": successfully sent " + r.getUri());
                    } else {
                        sendMessage(getSuccessMessage());
                        sendMessage(getResultSize((long) 0));
                        ServerInstance.logger.fine(this.ClientAddress + ": no matched file");
                    }
                }
            }
        }catch (JsonSyntaxException e){
            ServerInstance.logger.warning(this.ClientAddress+": missing resourceTemplate");
            sendMessage(getErrorMessage("missing resourceTemplate"));
        }catch (FileNotFoundException e){
            ServerInstance.logger.warning(this.ClientAddress+": file not found");
            sendMessage(getErrorMessage("file not found"));
        }catch (IOException e){
            ServerInstance.logger.warning(this.ClientAddress+": IOException when fetching");
        }catch (URISyntaxException e){
            ServerInstance.logger.warning(this.ClientAddress+": unable to create URI");
            sendMessage(getErrorMessage("cannot fetch resource"));
        }
    }

    /**
     * Create JSON string of error message.
     * @param errorMessage  The message in errorMessage field.
     * @return  JSON string
     */
    private String getErrorMessage(String errorMessage){
        Map<String,String> response = new LinkedHashMap<>();
        response.put("response","error");
        response.put("errorMessage",errorMessage);
        return gson.toJson(response,LinkedHashMap.class);
    }

    /**
     * Create JSON string of success message.
     * @return  JSON string
     */
    private String getSuccessMessage(){
        Map<String,String> response = new LinkedHashMap<>();
        response.put("response","success");
        return gson.toJson(response,LinkedHashMap.class);
    }

    /**
     * Create JSON string of resultSize message
     * @param resultSize    value in resultSize field.
     * @return  JSON string.
     */
    private String getResultSize(Long resultSize){
        Map<String,Long> response = new LinkedHashMap<>();
        response.put("resultSize",resultSize);
        return gson.toJson(response,LinkedHashMap.class);
    }

    /**
     * Send JSON message via output stream and IOException handling.
     * @param JSON  JSON message to be sent.
     */
    private void sendMessage(String JSON){
        try {
            output.writeUTF(JSON);
            output.flush();
        }catch (IOException e){
            ServerInstance.logger.warning(this.ClientAddress+": IOException when sending message!");
        }
    }

    /**
     * Process query relay request to other remote server in server list, almost identical to that of in Client.
     * @param host  Host to query.
     * @param queryMessage  The queryMessage.
     * @return  Result set of the query.
     */
    public List<ResourceTemplate> queryRelay(Host host,QueryMessage queryMessage){

        List<ResourceTemplate> result = new ArrayList<>();

        try(Socket socket = new Socket(host.getHostname(),host.getPort())) {

            ServerInstance.logger.fine("querying to "+socket.getRemoteSocketAddress().toString());
            socket.setSoTimeout(3000);

            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            String JSON = gson.toJson(queryMessage);

            outputStream.writeUTF(JSON);
            outputStream.flush();

            String response = inputStream.readUTF();

            if(response.contains("success")){
                response = inputStream.readUTF(); //discard success message.
                while (!response.contains("resultSize")){   //only read resource part.
                    ResourceTemplate r = gson.fromJson(response,ResourceTemplate.class);
                    //encrypt owner field if necessary.
                    if(!r.getOwner().equals("")){
                        r.setOwner("*");
                    }
                    result.add(r);
                    response = inputStream.readUTF();   //read next response.
                }
                ServerInstance.logger.fine("successfully queried "+socket.getRemoteSocketAddress().toString());
            }else {
                ServerInstance.logger.warning(response);
            }
            socket.close();

        }catch (SocketTimeoutException e){
            ServerInstance.logger.warning(host.toString()+" timeout when query relay");
            this.serverList.removeServer(host);
        }catch (ConnectException e){
            ServerInstance.logger.warning(host.toString()+" timeout when create relay socket");
            this.serverList.removeServer(host);
        } catch (IOException e){
            ServerInstance.logger.warning(host.toString()+" IOException when query relay");
            this.serverList.removeServer(host);
        }
       return result;
    }



}