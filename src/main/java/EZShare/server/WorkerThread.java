package EZShare.server;

import EZShare.message.*;
import EZShare.Nodes;

import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.security.PublicKey;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;


import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * @author Yuqing Liu
 */
public class WorkerThread extends Thread {

    private Socket client;
    private FileList fileList;
    private ServerList serverList;
    private boolean isUltraNode;
    private DataOutputStream output;
    private DataInputStream input;
    private String ClientAddress;
    private Gson gson = new Gson();
    private int maxHops;//maximum hops to visit
    private KeyList keyList;

    /**
     * Initialize worker thread, create IO streams.
     *
     * @param client     the socket.
     * @param fileList   reference of file list.
     * @param serverList reference of server list.
     */
    public WorkerThread(Socket client, FileList fileList, ServerList serverList,boolean isUltraNode,int maxHops,KeyList keyList) {
        this.client = client;
        this.fileList = fileList;
        this.serverList = serverList;
        this.isUltraNode = isUltraNode;
        this.maxHops = maxHops;
        this.keyList = keyList;
        Nodes.logger.log(Level.INFO, "is ultra node : {0}", this.isUltraNode);
        Nodes.logger.log(Level.INFO,"maximum hops to route:{0}",this.maxHops);
    }
    @Override
    public void run() {
        try {
            /* Socket opened. */
            this.ClientAddress = client.getRemoteSocketAddress().toString();
            this.input = new DataInputStream(client.getInputStream());
            this.output = new DataOutputStream(client.getOutputStream());

            Nodes.logger.log(Level.INFO, "{0} : Connected!", this.ClientAddress);

            /* Get input data. Remove \0 in order to prevent crashing. */
            String inputJson = input.readUTF();
//            Nodes.logger.info("input jason " + inputJson);
            inputJson = inputJson.replace("\0", "");

            /* Process and get output data. */
            List<String> outputJsons = reception(inputJson);

            /* Send back output data. */
//            Nodes.logger.info("before sendout");
            sendBackMessage(outputJsons);
//            Nodes.logger.info("after sendout");
        } catch (SocketTimeoutException e) {
            /* Socket time out during communication. */
            Nodes.logger.log(Level.WARNING, "{0} : Socket Timeout", this.ClientAddress);
        } catch (IOException e) {
            /* Socket time out in establishing. */
            Nodes.logger.log(Level.WARNING, "{0} : IOException!", this.ClientAddress);
        } finally {
            try {
                /* Close socket anyway. */
                client.close();
                Nodes.logger.log(Level.INFO, " : Disconnected!{0}", this.ClientAddress);
            } catch (IOException e) {
                Nodes.logger.log(Level.WARNING, "{0}: Unable to disconnect!", this.ClientAddress);
            }
        }
    }

    public List<String> reception(String inputJson) throws IOException {
        List<String> outputJsons = new LinkedList<>();
        boolean jsonSyntaxException = false;

        Message message = null;
        try {
            message = gson.fromJson(inputJson, Message.class);
            if (message.getCommand() == null) {
                throw new JsonSyntaxException("missing command");
            }
        } catch (JsonSyntaxException e) {
            /* Invalid syntax JSON or a JSON without field "command" */
            Nodes.logger.log(Level.WARNING, "{0} : missing or incorrect type for command", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("missing or incorrect type for command"));
            jsonSyntaxException = true;
        }

        if (!jsonSyntaxException) {
            if (!message.getCommand().equals("SUBSCRIBE"))
                this.client.setSoTimeout(3000);
            switch (message.getCommand()) {
                case "PUBLISH":
                    processPublish(outputJsons, inputJson);
                    break;
                case "SHARE":
                    processShare(outputJsons, inputJson);
                    break;
                case "REMOVE":
                    processRemove(outputJsons, inputJson);
                    break;
                case "EXCHANGE":
                    processExchange(outputJsons, inputJson);
                    break;
                case "EXCHANGEKEY":
                    processKeyExchange(outputJsons,inputJson);
                    break;
                case "FETCH":
                    processFetch(outputJsons, inputJson);
                    break;
                case "QUERY":
                    processQuery(outputJsons, inputJson);
                    break;
                case "SUBSCRIBE":
                    processSubscribe(outputJsons, inputJson);
                    break;
                default:
                    /* a JSON with field "command", but not in the list above */
                    Nodes.logger.log(Level.WARNING, "{0} : invalid command", this.ClientAddress);
                    outputJsons.add(getErrorMessageJson("invalid command"));
            }
        }
        return outputJsons;
    }

    /*subscription should be passed over if it is ultra node*/
    /*needs to check correctness for multiple subscription and termination @larry*/
    public void processSubscribe(List<String> outputJsons, String JSON) throws IOException {
        try {
            SubscribeMessage subscribeMessage = gson.fromJson(JSON, SubscribeMessage.class);

            if (subscribeMessage.getResourceTemplate() == null) {
                throw new JsonSyntaxException("missing resourceTemplate");
            }
            ResourceTemplate r = subscribeMessage.getResourceTemplate();
            Nodes.logger.log(Level.INFO, "{0} subscribing for {1}", new Object[]{client.getRemoteSocketAddress(), r.toString()});

            if (!subscribeMessage.isValid()) {
                Nodes.logger.log(Level.WARNING, "{0} : invalid resourceTemplate", this.ClientAddress);
                outputJsons.add(getErrorMessageJson("invalid resourceTemplate"));
                //handle non-relayed subscription.
            } else if (!subscribeMessage.isRelay()) {
                //send success message.asynchronous
                //relay is false,which means the last node to subscribe
                String response = getSubscribeSuccessMessageJson(subscribeMessage.getId());
                this.output.writeUTF(response);
                this.output.flush();
                //put the subscription in list
                Nodes.subscriptions.put(this.client, new Subscription(subscribeMessage, this.ClientAddress));
                Nodes.logger.log(Level.FINE, "{0} : Resource subscribed!(relay=false)", this.ClientAddress);
                //block until user terminate.
                while (true) {
                    String next;
                    try {
                        if ((next = this.input.readUTF()) != null) {
                            if (next.contains("UNSUBSCRIBE")) {
                                //unsubscribe for this subscription
                                UnsubscribeMessage unsubscribeMessage = gson.fromJson(next, UnsubscribeMessage.class);
                                Nodes.subscriptions.get(this.client).removeSubscribeMessage(unsubscribeMessage.getId());
                                String resultsize = getResultSizeJson((long) Nodes.subscriptions.get(this.client).getResultSize(unsubscribeMessage.getId()));
                                this.output.writeUTF(resultsize);
                                this.output.flush();
                                Nodes.logger.log(Level.INFO, "{0} : Terminating subscription " + unsubscribeMessage.getId() + " with resultSize:" + resultsize, this.ClientAddress);
                                if (Nodes.subscriptions.get(this.client).getSubscribeMessage().size() == 0) {
                                    break;
                                }
                            } else if (next.contains("SUBSCRIBE")) {
                                SubscribeMessage newsubscribe = gson.fromJson(next, SubscribeMessage.class);
                                Nodes.subscriptions.get(this.client).addSubscribeMessage(newsubscribe);
                            }
                        }
                    } catch (IOException e) {

                    }
                }
            } else if (subscribeMessage.isRelay()) {
                //send success message.
                String response = getSubscribeSuccessMessageJson(subscribeMessage.getId());
                this.output.writeUTF(response);
                this.output.flush();

                boolean needrefresh = true;
                for (Map.Entry<Socket, Subscription> entry : Nodes.subscriptions.entrySet()) {
                    ConcurrentHashMap<SubscribeMessage, Integer> sms = entry.getValue().getSubscribeMessage();
                    for (Map.Entry<SubscribeMessage, Integer> m : sms.entrySet()) {
                        if (m.getKey().isRelay()) {
                            needrefresh = false;
                            break;
                        }
                    }
                    if (!needrefresh) {
                        break;
                    }
                }
                if (needrefresh) {
                    serverList.refreshAllRelay();
                }
                int mxHops = subscribeMessage.getMxHops();
                if (mxHops == 1){
                    subscribeMessage.setRelay(false);//set relay as false when reaching max hops
                }
                //put the subscription in list
                Nodes.subscriptions.put(this.client, new Subscription(subscribeMessage, this.ClientAddress));
                SubscribeMessage forwarded = new SubscribeMessage(subscribeMessage.isRelay(), subscribeMessage.getId(),
                        subscribeMessage.getResourceTemplate(),(mxHops-1));
                serverList.doMessageRelay(gson.toJson(forwarded));
                Nodes.logger.log(Level.FINE, "{0} : Resource subscribed!(relay=true)", this.ClientAddress);
                //block until user terminate.
                String next;
                while (true) {
                    if ((next = this.input.readUTF()) != null) {
                        break;
                    }
                }
                serverList.doMessageRelay(gson.toJson(new UnsubscribeMessage(subscribeMessage.getId())));
                Subscription subscription = Nodes.subscriptions.get(this.client);
                int size = 0;
                for (Map.Entry<SubscribeMessage, Integer> entry : subscription.getSubscribeMessage().entrySet()) {
                    size += entry.getValue();
                }
                JSON = getResultSizeJson((long) size);
                this.output.writeUTF(JSON);
                this.output.flush();
                Nodes.subscriptions.remove(this.client);
            }
        } catch (JsonSyntaxException e) {
            Nodes.logger.log(Level.WARNING, "{0} : missing resourceTemplate", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("missing resourceTemplate"));
        }
    }

    /*cannot publish to ultra node,but normal to local or friend nodes*/
    public void processPublish(List<String> outputJsons, String JSON) {
        try {
            if (isUltraNode){
                throw new Exception("cannot publish resource to ultra node");
            }
            PublishMessage publishMessage = gson.fromJson(JSON, PublishMessage.class);

            if (publishMessage.getResource() == null) {
                throw new JsonSyntaxException("missing resource");
            }

            ResourceTemplate r = publishMessage.getResource();

            r.setEzserver(Nodes.HOST + ":" + Nodes.PORT);

            if (!publishMessage.isValid()) {
                Nodes.logger.log(Level.WARNING, "{0} : invalid resource", this.ClientAddress);
                outputJsons.add(getErrorMessageJson("invalid resource"));
            } else if (!fileList.add(r)) {
                Nodes.logger.log(Level.WARNING, "{0} : cannot publish resource", this.ClientAddress);
                outputJsons.add(getErrorMessageJson("cannot publish resource"));
            } else {
                Nodes.logger.log(Level.FINE, "{0} : resource published!", this.ClientAddress);
                outputJsons.add(getSuccessMessageJson());
            }
        } catch (JsonSyntaxException e) {
            Nodes.logger.log(Level.WARNING, "{0} : missing resource", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("missing resource"));
        }catch(Exception e){
            Nodes.logger.log(Level.INFO,"{0}:cannot publish resource to ultra node",this.ClientAddress);
            outputJsons.add(getErrorMessageJson("cannot publish resource to ultra node"));
        }
    }

    /*cannot remove anything in ultra node,but normal to local or friend nodes*/
    public void processRemove(List<String> outputJsons, String JSON) {
        try {
            if (isUltraNode){
                throw new Exception("no resource to remove in ultra node");
            }
            RemoveMessage removeMessage = gson.fromJson(JSON, RemoveMessage.class);

            if (removeMessage.getResource() == null) {
                throw new JsonSyntaxException("missing resource");
            }

            ResourceTemplate r = removeMessage.getResource();
            if (!removeMessage.isValid()) {
                Nodes.logger.log(Level.WARNING, "{0} : invalid resource", this.ClientAddress);
                outputJsons.add(getErrorMessageJson("invalid resource"));

            } else if (!fileList.remove(r)) {
                Nodes.logger.log(Level.WARNING, "{0} : cannot remove resource", this.ClientAddress);
                outputJsons.add(getErrorMessageJson("cannot remove resource"));

            } else {
                Nodes.logger.log(Level.FINE, "{0} : resource removed!", this.ClientAddress);
                outputJsons.add(getSuccessMessageJson());
            }

        } catch (JsonSyntaxException e) {
            Nodes.logger.log(Level.WARNING, "{0} : missing resource", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("missing resource"));
        } catch (Exception e) {
            Nodes.logger.log(Level.INFO,"{0}:no resource to remove in ultra node",this.ClientAddress);
            outputJsons.add(getErrorMessageJson("no resource to remove in ultra node"));
        }
    }

    /*cannot share anything in ultra node since it is almost empty,but normal to local or friend nodes */
    public void processShare(List<String> outputJsons, String JSON) {
        try {
            if (isUltraNode){
                throw new Exception("no resource to share in ultra node");
            }
            ShareMessage shareMessage = gson.fromJson(JSON, ShareMessage.class);

            if (shareMessage.getResource() == null) {
                throw new JsonSyntaxException("missing resource");
            }

            ResourceTemplate r = shareMessage.getResource();

            r.setEzserver(Nodes.HOST + ":" + Nodes.PORT);

            if (!shareMessage.isValid()) {
                //resource not valid
                Nodes.logger.log(Level.WARNING, "{0} : invalid resource", this.ClientAddress);
                outputJsons.add(getErrorMessageJson("invalid resource"));
            }
            else {
                String message;
                File f = new File(new URI(r.getUri()).getPath());
                Nodes.logger.log(Level.INFO, "{0} : request for sharing {1}", new Object[]{this.ClientAddress, r.getUri()});
                if (f.exists()) {
                    //file exist
                    if (fileList.add(r)) {
                        //file successfully added
                        Nodes.logger.log(Level.FINE, "{0} : resource shared!", this.ClientAddress);
                        message = getSuccessMessageJson();
                    } else {
                        //file exist but cannot be added
                        Nodes.logger.log(Level.WARNING, "{0} : resource unable to be added!", this.ClientAddress);
                        message = getErrorMessageJson("cannot share resource");
                    }
                } else {
                    //file don't exist
                    Nodes.logger.log(Level.WARNING, "{0} : resource does not exist", this.ClientAddress);
                    message = getErrorMessageJson("cannot share resource");
                }
                outputJsons.add(message);
            }

        } catch (JsonSyntaxException e) {
            Nodes.logger.log(Level.WARNING, "{0} : missing resource", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("missing resource"));
        } catch (URISyntaxException e) {
            Nodes.logger.log(Level.WARNING, "{0} : unable to create URI", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("cannot share resource"));
        } catch (Exception e) {
            Nodes.logger.log(Level.INFO,"{0}:no resource to share in ultra node",this.ClientAddress);
            outputJsons.add(getErrorMessageJson("no resource to share in ultra node"));
        }
    }

    /*exchange no need to change,used for connecting nodes*/
    public void processExchange(List<String> outputJsons, String JSON) {
//        System.out.println(JSON);
        try {
            ExchangeMessage exchangeMessage = gson.fromJson(JSON, ExchangeMessage.class);

            if (exchangeMessage.getServerList() == null || exchangeMessage.getServerList().isEmpty()) {
                throw new JsonSyntaxException("missing server");
            }
            List<Host> inputServerList = exchangeMessage.getServerList();

//            Nodes.logger.info("exchangeMessage valid?" + exchangeMessage.isValid());
            if (exchangeMessage.isValid()) {
                //all servers valid, add to server list.
//                Nodes.logger.info("begin update server list:"+inputServerList);
                this.serverList.updateServerList(inputServerList);
                Nodes.logger.log(Level.FINE, "{0} : servers added", this.ClientAddress);
                outputJsons.add(getSuccessMessageJson());
            } else {
                outputJsons.add(getErrorMessageJson("invalid server record"));
            }

        } catch (JsonSyntaxException e) {
            Nodes.logger.log(Level.WARNING, "{0} : missing or invalid server list", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("missing or invalid server list"));
        }
    }

    public void processKeyExchange(List<String> outputJsons, String JSON){
        try{
//            System.out.println("input JSON:"+JSON);
            ExchangeKeyList exchangeKeyList = gson.fromJson(JSON,ExchangeKeyList.class);
            if (exchangeKeyList.getKeyList() == null || exchangeKeyList.getKeyList().isEmpty()) {
                throw new JsonSyntaxException("missing keys");
            }
//            ConcurrentHashMap<String, PublicKey> inputKeyList = exchangeKeyList.getKeyList();
            ConcurrentHashMap<String, String> inputKeyList = exchangeKeyList.getKeyList();
//            System.out.println("input key list:"+inputKeyList);
            if (exchangeKeyList.isValid()){
                this.keyList.updateKeyList(inputKeyList);
                Nodes.logger.log(Level.FINE, "{0} : keys added", this.ClientAddress);
                outputJsons.add(getSuccessMessageJson());
            }else {
                outputJsons.add(getErrorMessageJson("invalid server record"));
            }
        } catch (JsonSyntaxException e) {
            Nodes.logger.log(Level.WARNING, "{0} : missing or invalid key list", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("missing or invalid key list"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    /*query done*/
    public void processQuery(List<String> outputJsons, String JSON) {
        try {
            QueryMessage queryMessage = gson.fromJson(JSON, QueryMessage.class);
            if (queryMessage.getResourceTemplate() == null) {
                throw new JsonSyntaxException("missing resource");
            }
            ResourceTemplate r = queryMessage.getResourceTemplate();
            Nodes.logger.log(Level.INFO, "{0} querying for {1}", new Object[]{client.getRemoteSocketAddress(), r.toString()});
            if (!queryMessage.isValid()) {
                Nodes.logger.log(Level.WARNING, "{0} : invalid resourceTemplate", this.ClientAddress);
                outputJsons.add(getErrorMessageJson("invalid resourceTemplate"));
            } else{
                //relay is always true, query local resource first and query forward
                List<ResourceTemplate> result = this.fileList.query(r);
                int mxHops = queryMessage.getMxHops();
                if (!result.isEmpty() || mxHops == 0){
                    outputJsons.add(getSuccessMessageJson());
                    Nodes.logger.fine("Query Success");
                    for (ResourceTemplate rt : result) {
                        if (!rt.getOwner().equals("")) {
                            rt.setOwner("*");
                        }
                        outputJsons.add(rt.toString());
                    }
                }else {
                    //when hops are not cast over,i.e. mxHops>1
                    QueryMessage relayMessage = gson.fromJson(JSON, QueryMessage.class);
                    relayMessage.setMxHops(mxHops-1);
                    relayMessage.getResourceTemplate().setOwner("");
                    relayMessage.getResourceTemplate().setChannel("");
                    //append result set by querying remote servers
                    /*to make sure it wont expand node more than limited*/
                    if (Nodes.MAX_NODES_TO_EXPAND >= this.serverList.getServerList().size()){
                        //if server list is smaller than nodes to expand
                        for (Host h : this.serverList.getServerList()) {
                            List<ResourceTemplate> rtl = doSingleQueryRelay(h, relayMessage);
                            if (!rtl.isEmpty())
                                result.addAll(rtl);
                        }
                    }else {
                        //random expand nodes in server list
                        List<Host> copyserverlist = new LinkedList<Host>(serverList.getServerList());
                        Collections.shuffle(copyserverlist);
                        List<Host> subserverlist = copyserverlist.subList(0,Nodes.MAX_NODES_TO_EXPAND);
                        for (Host h : subserverlist) {
                            List<ResourceTemplate> rtl = doSingleQueryRelay(h, relayMessage);
                            if (!rtl.isEmpty())
                                result.addAll(rtl);
                        }
                    }
                    outputJsons.add(getSuccessMessageJson());
                    for (ResourceTemplate rt : result) {
                        if (!rt.getOwner().equals("")) {
                            rt.setOwner("*");
                        }
                        outputJsons.add(rt.toString());
                    }
                    Nodes.logger.fine("Query relay Success");
                }
                outputJsons.add(getResultSizeJson(((long) result.size())));
            }
        } catch (JsonSyntaxException e) {
            Nodes.logger.log(Level.WARNING, "{0} : missing resourceTemplate", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("missing resourceTemplate"));
        }
    }

    /*fetch file in local and forward if no match*/
    public void processFetch(List<String> outputJsons, String JSON) {
        try {
            FetchMessage fetchMessage = gson.fromJson(JSON, FetchMessage.class);
            if (fetchMessage.getResource() == null) {
                throw new JsonSyntaxException("missing resource");
            }
            ResourceTemplate r = fetchMessage.getResource();
            Nodes.logger.log(Level.INFO, "{0} fetching for {1}", new Object[]{client.getRemoteSocketAddress(), r.toString()});
            if (!fetchMessage.isValid()) {
                Nodes.logger.log(Level.WARNING, "{0} : invalid resourceTemplate", this.ClientAddress);
                outputJsons.add(getErrorMessageJson("invalid resourceTemplate"));
            } else {
                //fetch relay will always be true until file reached or maximum hops reached
                List<ResourceTemplate> result = this.fileList.fetch(r);
                int mxHops = fetchMessage.getMxHops();
                if (!result.isEmpty()) {
                    //only one result could be possible
                    //to anonymous all nodes channel and owner
                    if (!result.get(0).getOwner().equals("")){result.get(0).setOwner("*");}
//                    if (!result.get(0).getChannel().equals("")) {result.get(0).setChannel("*");}//larry added
                    Nodes.logger.log(Level.INFO,"fetching local");
                    //download when file or maximum hops reached,leaf node or friend node only,since ultra node is empty
                    RandomAccessFile file;
                    file = new RandomAccessFile(new File(new URI(r.getUri()).getPath()), "r");
                    //file existed.
                    outputJsons.add(getSuccessMessageJson());
                    outputJsons.add(gson.toJson(new FileTemplate(result.get(0), file.length())));
                    file.close();
                    outputJsons.add(r.getUri());
                    outputJsons.add(getResultSizeJson((long) 1));
                } else if (maxHops == 0) {
                    outputJsons.add(getSuccessMessageJson());
                    outputJsons.add(getResultSizeJson((long) 0));
                    Nodes.logger.log(Level.FINE, "{0} : no matched file", this.ClientAddress);
                } else {
                    //no local file reached but still have hops to relay
                    FetchMessage relayFetchMessage = gson.fromJson(JSON, FetchMessage.class);
//                    relayFetchMessage.getResource().setOwner("");
//                    relayFetchMessage.getResource().setChannel("");
                    relayFetchMessage.setMxHops(mxHops - 1);
                    //append result set by fetching to remote servers
                    /*to make sure it wont expand node more than limited*/
                    Nodes.logger.log(Level.INFO, "{0} :command valid and relay true", this.serverList.getServerList());
                    List<Host> serverListToRelay;
                    if (Nodes.MAX_NODES_TO_EXPAND >= this.serverList.getServerList().size()){
                        serverListToRelay = this.serverList.getServerList();
                    }else {
                        List<Host> copyserverlist = new LinkedList<>(serverList.getServerList());
                        Collections.shuffle(copyserverlist);
                        serverListToRelay = copyserverlist.subList(0, Nodes.MAX_NODES_TO_EXPAND);
                    }
                    boolean fileFound = false;
                    for (Host h : serverListToRelay) {
                        List<ResourceTemplate> rtl = doSingleFetchRelay(h, fetchMessage);
//                        System.out.println("result is empty"+rtl.isEmpty());
                        if (!rtl.isEmpty()) {
                            //eventually if it will get an result list,download only once
                            if (!rtl.get(0).getOwner().equals("")){rtl.get(0).setOwner("*");}
//                            if (!rtl.get(0).getChannel().equals("")) {rtl.get(0).setChannel("*");}//larry added
                            Nodes.logger.log(Level.INFO,"fetching relay");
                            RandomAccessFile file;
                            file = new RandomAccessFile(new File(new URI(r.getUri()).getPath()), "r");
                            outputJsons.add(getSuccessMessageJson());
                            outputJsons.add(gson.toJson(new FileTemplate(rtl.get(0), file.length())));
                            file.close();
                            outputJsons.add(r.getUri());
                            outputJsons.add(getResultSizeJson((long) 1));
                            fileFound = true;
                            break;//stop relay after one success reached
                        }
                    }
                    //still if no matched file found
                    if(!fileFound){
                        outputJsons.add(getSuccessMessageJson());
                        outputJsons.add(getResultSizeJson((long) 0));
                        Nodes.logger.log(Level.FINE, "{0} : no matched file", this.ClientAddress);
                    }
                }//server list iteration ended
            }//relay succeed
        } catch (JsonSyntaxException e) {
            Nodes.logger.log(Level.WARNING, "{0} : missing resourceTemplate", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("missing resourceTemplate"));
        } catch (FileNotFoundException e) {
            Nodes.logger.log(Level.WARNING, "{0} : file not found", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("file not found"));
        } catch (IOException e) {
            Nodes.logger.log(Level.WARNING, "{0} : IOException when fetching", this.ClientAddress);
        } catch (URISyntaxException e) {
            Nodes.logger.log(Level.WARNING, "{0} : unable to create URI", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("cannot fetch resource"));
        }
    }
    private List<ResourceTemplate> doSingleFetchRelay(Host host, FetchMessage fetchMessage){
        List<ResourceTemplate> result = new ArrayList<>();
        try{
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host.getHostname(), host.getPort()));
            Nodes.logger.log(Level.FINE, "fetching to {0}", socket.getRemoteSocketAddress().toString());
            socket.setSoTimeout(3000);

            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            String JSON = gson.toJson(fetchMessage);
            Nodes.logger.log(Level.INFO, "fetching information {0}",JSON);
            outputStream.writeUTF(JSON);
            outputStream.flush();
//            Nodes.logger.log(Level.INFO, "outputStream succeed!");
            String response = inputStream.readUTF();
//            Nodes.logger.log(Level.INFO, "inputStream succeed!");

            if (response.contains("success")) {
                response = inputStream.readUTF(); //discard success message.
                while (!response.contains("resultSize")) {   //only read resource part.
                    ResourceTemplate r = gson.fromJson(response, ResourceTemplate.class);
                    result.add(r);
                    response = inputStream.readUTF();   //read next response.
                }
                Nodes.logger.log(Level.FINE, "successfully fetched {0}", socket.getRemoteSocketAddress().toString());
                Nodes.logger.log(Level.FINE, "nunmber of result fetched {0}", result.size());
                Nodes.logger.log(Level.FINE, "result returned {0}", result);
            } else {
                Nodes.logger.warning(response);
            }
            socket.close();
        }catch (SocketTimeoutException e) {
            Nodes.logger.log(Level.WARNING, "{0} timeout when fetch relay", host.toString());
        } catch (ConnectException e) {
            Nodes.logger.log(Level.WARNING, "{0} timeout when create relay socket", host.toString());
        } catch (IOException e) {
            Nodes.logger.log(Level.WARNING, "{0} IOException when fetch relay", host.toString());
        }
        return result;
    }
    private List<ResourceTemplate> doSingleQueryRelay(Host host, QueryMessage queryMessage) {
        List<ResourceTemplate> result = new ArrayList<>();
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host.getHostname(), host.getPort()));
            Nodes.logger.log(Level.FINE, "querying to {0}", socket.getRemoteSocketAddress().toString());
            socket.setSoTimeout(3000);

            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            String JSON = gson.toJson(queryMessage);

            outputStream.writeUTF(JSON);
            outputStream.flush();

            String response = inputStream.readUTF();

            if (response.contains("success")) {
                response = inputStream.readUTF(); //discard success message.
                while (!response.contains("resultSize")) {   //only read resource part.
                    ResourceTemplate r = gson.fromJson(response, ResourceTemplate.class);
                    result.add(r);
                    response = inputStream.readUTF();   //read next response.
                }
                Nodes.logger.log(Level.FINE, "successfully queried {0}", socket.getRemoteSocketAddress().toString());
            } else {
                Nodes.logger.warning(response);
            }
            socket.close();

        } catch (SocketTimeoutException e) {
            Nodes.logger.log(Level.WARNING, "{0} timeout when query relay", host.toString());
        } catch (ConnectException e) {
            Nodes.logger.log(Level.WARNING, "{0} timeout when create relay socket", host.toString());
        } catch (IOException e) {
            Nodes.logger.log(Level.WARNING, "{0} IOException when query relay", host.toString());
        }
        return result;
    }

    private String getErrorMessageJson(String errorMessage) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("response", "error");
        response.put("errorMessage", errorMessage);
        return gson.toJson(response, LinkedHashMap.class);
    }

    private String getSuccessMessageJson() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("response", "success");
        return gson.toJson(response, LinkedHashMap.class);
    }

    private String getSubscribeSuccessMessageJson(String id) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("response", "success");
        response.put("id", id);
        return gson.toJson(response, LinkedHashMap.class);
    }

    private String getResultSizeJson(Long resultSize) {
        Map<String, Long> response = new LinkedHashMap<>();
        response.put("resultSize", resultSize);
        return gson.toJson(response, LinkedHashMap.class);
    }

    private void sendBackMessage(List<String> jsons) {
        try {
            for (String json : jsons) {
//                System.out.println("sendbackJson:"+json);
                /* If json.length() == 0 (barely happens), do nothing at the moment. */
                if (json.length() != 0) {
                    /* Let's assume that: If the string is not a json object, it must be a file URI. */
                    if (json.charAt(0) == '{') {
                        output.writeUTF(json);
                        output.flush();
                    } else {
                        RandomAccessFile file;
                        file = new RandomAccessFile(new File(new URI(json).getPath()), "r");

                        byte[] sendingBuffer = new byte[1024 * 1024];
                        int num;
                        // While there are still bytes to send..
                        Nodes.logger.log(Level.INFO, "{0} : start sending file {1}", new Object[]{this.ClientAddress, json});
                        while ((num = file.read(sendingBuffer)) > 0) {
                            output.write(Arrays.copyOf(sendingBuffer, num));
                        }
                        Nodes.logger.log(Level.FINE, "{0} : successfully sent {1}", new Object[]{this.ClientAddress, json});
                        file.close();
                    }
                }
            }
        } catch (IOException e) {
            Nodes.logger.log(Level.WARNING, "{0} : IOException when sending message!", this.ClientAddress);
        } catch (URISyntaxException ex) {
            Nodes.logger.log(Level.WARNING, "{0} : unable to create URI", this.ClientAddress);
        }
    }
}
