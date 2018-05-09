package EZShare.server;

import EZShare.message.*;
import EZShare.Nodes;

import java.io.*;
import java.net.*;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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

    private boolean secure;
    private DataOutputStream output;
    private DataInputStream input;
    private String ClientAddress;
    private Gson gson = new Gson();

    /**
     * Initialize worker thread, create IO streams.
     *
     * @param client     the socket.
     * @param fileList   reference of file list.
     * @param serverList reference of server list.
     */
    public WorkerThread(Socket client, FileList fileList, ServerList serverList,boolean secure,boolean isUltraNode) {
        this.client = client;
        this.fileList = fileList;
        this.serverList = serverList;
        this.isUltraNode = isUltraNode;
        // Is it a Secure Socket?
        this.secure = secure;
        Nodes.logger.log(Level.INFO, "SecureSocket : {0}", this.secure);
        Nodes.logger.log(Level.INFO, "is ultra node : {0}", this.isUltraNode);
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
            e.printStackTrace();
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

    public void processSubscribe(List<String> outputJsons, String JSON) throws IOException {
        try {
            SubscribeMessage subscribeMessage = gson.fromJson(JSON, SubscribeMessage.class);

            if (subscribeMessage.getResourceTemplate() == null) {
                throw new JsonSyntaxException("missing resourceTemplate");
            }


            if (!subscribeMessage.isValid()) {
                Nodes.logger.log(Level.WARNING, "{0} : invalid resourceTemplate", this.ClientAddress);
                outputJsons.add(getErrorMessageJson("invalid resourceTemplate"));

                //handle unrelayed subscription.
            } else if (!subscribeMessage.isRelay()) {

                //send success message.
                String response = getSubscribeSuccessMessageJson(subscribeMessage.getId());

                this.output.writeUTF(response);
                this.output.flush();

                //put the subscription in list
                Nodes.subscriptions.put(this.client, new Subscription(subscribeMessage, this.ClientAddress, secure));

                Nodes.logger.log(Level.FINE, "{0} : Resource subscribed!(relay=false)", this.ClientAddress);

                //block until user terminate.

                while (true) {
                    String next;
                    try {
                        if ((next = this.input.readUTF()) != null) {
                            if (next.contains("UNSUBSCRIBE")) {
                                //unsub for this subscription
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

                //put the subscription in list
                Nodes.subscriptions.put(this.client, new Subscription(subscribeMessage, this.ClientAddress, secure));

                SubscribeMessage forwarded = new SubscribeMessage(false, subscribeMessage.getId(), subscribeMessage.getResourceTemplate());
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


    public void processPublish(List<String> outputJsons, String JSON) {
        try {
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
        }
    }

    public void processRemove(List<String> outputJsons, String JSON) {
        try {
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
        }
    }

    public void processShare(List<String> outputJsons, String JSON) {
        try {
            ShareMessage shareMessage = gson.fromJson(JSON, ShareMessage.class);

            if (shareMessage.getResource() == null) {
                throw new JsonSyntaxException("missing secret and/or resource");
            }

            ResourceTemplate r = shareMessage.getResource();

            r.setEzserver(Nodes.HOST + ":" + Nodes.PORT);

            if (!shareMessage.isValid()) {
                //resource not valid
                Nodes.logger.log(Level.WARNING, "{0} : invalid resource", this.ClientAddress);
                outputJsons.add(getErrorMessageJson("invalid resource"));
            }
//            else if (!shareMessage.getSecret().equals(Nodes.SECRET)) {
//                //secret incorrect
//                Nodes.logger.log(Level.WARNING, "{0} : incorrect secret", this.ClientAddress);
//                outputJsons.add(getErrorMessageJson("incorrect secret"));
//            }
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
            Nodes.logger.log(Level.WARNING, "{0} : missing resource and/or secret", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("missing resource and/or secret"));
        } catch (URISyntaxException e) {
            Nodes.logger.log(Level.WARNING, "{0} : unable to create URI", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("cannot share resource"));
        }
    }

    public void processExchange(List<String> outputJsons, String JSON) {
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

    public void processQuery(List<String> outputJsons, String JSON) {
        try {
            QueryMessage queryMessage = gson.fromJson(JSON, QueryMessage.class);
//            System.out.println("query message is relay:"+queryMessage.isRelay());//@@@larry
            //queryMessage.isRelay() is always true since message passed in is true
//            System.out.println("queryMsg.class print:"+QueryMessage.class);
            if (queryMessage.getResourceTemplate() == null) {
                throw new JsonSyntaxException("missing resource");
            }
            ResourceTemplate r = queryMessage.getResourceTemplate();

            Nodes.logger.log(Level.INFO, "{0} querying for {1}", new Object[]{client.getRemoteSocketAddress(), r.toString()});

            if (!queryMessage.isValid()) {
                Nodes.logger.log(Level.WARNING, "{0} : invalid resourceTemplate", this.ClientAddress);
                outputJsons.add(getErrorMessageJson("invalid resourceTemplate"));
            } else{
                //relay is always true, query local resource first
                //do not change original query
                List<ResourceTemplate> result = this.fileList.query(r);
                QueryMessage relayMessage = gson.fromJson(JSON, QueryMessage.class);

                //relayMessage.setRelay(false);//should always be true to relay forward

                //append result set by querying remote servers
                try {
                    for (Host h : this.serverList.getServerList()) {
                        List<ResourceTemplate> rtl = doSingleQueryRelay(h, relayMessage);
                        if (!rtl.isEmpty())
                            result.addAll(rtl);
                    }
                }catch (Exception e){
                    Nodes.logger.log(Level.WARNING,"{0} : No available server :",this.serverList.getServerList());
                }

                outputJsons.add(getSuccessMessageJson());
                for (ResourceTemplate rt : result) {
                    if (!rt.getOwner().equals("")) {
                        rt.setOwner("*");
                    }
                    outputJsons.add(rt.toString());
                }
                outputJsons.add(getResultSizeJson(((long) result.size())));
                Nodes.logger.fine("Query relay Success");
            }
        } catch (JsonSyntaxException e) {
            Nodes.logger.log(Level.WARNING, "{0} : missing resourceTemplate", this.ClientAddress);
            outputJsons.add(getErrorMessageJson("missing resourceTemplate"));
        }

    }

    public void processFetch(List<String> outputJsons, String JSON) {
        try {
            FetchMessage fetchMessage = gson.fromJson(JSON, FetchMessage.class);
            if (fetchMessage.getResource() == null) {
                throw new JsonSyntaxException("missing resource");
            }
            ResourceTemplate r = fetchMessage.getResource();

            if (!fetchMessage.isValid()) {
                Nodes.logger.log(Level.WARNING, "{0} : invalid resourceTemplate", this.ClientAddress);
                outputJsons.add(getErrorMessageJson("invalid resourceTemplate"));
            } else {
                List<ResourceTemplate> result = this.fileList.fetch(r);

                if (!result.isEmpty()) {
                    RandomAccessFile file;
                    file = new RandomAccessFile(new File(new URI(r.getUri()).getPath()), "r");

                    //file existed.
                    outputJsons.add(getSuccessMessageJson());
                    outputJsons.add(gson.toJson(new FileTemplate(result.get(0), file.length())));

                    file.close();

                    outputJsons.add(r.getUri());

                    outputJsons.add(getResultSizeJson((long) 1));

                } else {
                    outputJsons.add(getSuccessMessageJson());
                    outputJsons.add(getResultSizeJson((long) 0));
                    Nodes.logger.log(Level.FINE, "{0} : no matched file", this.ClientAddress);
                }
            }

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
            this.serverList.removeServer(host, secure);
        } catch (ConnectException e) {
            Nodes.logger.log(Level.WARNING, "{0} timeout when create relay socket", host.toString());
//            Nodes.logger.info("server list now have :"+ this.serverList.getServerList());
            this.serverList.removeServer(host, secure);
//            Nodes.logger.info("server list then have :"+ this.serverList.getServerList());
        } catch (IOException e) {
            Nodes.logger.log(Level.WARNING, "{0} IOException when query relay", host.toString());
            this.serverList.removeServer(host, secure);
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

    // For test
    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public Socket getClient() {
        return client;
    }

    public void setClient(Socket client) {
        this.client = client;
    }
}
