package EZShare;

import EZShare.log.LogCustomFormatter;
import EZShare.message.*;
import EZShare.server.FileList;
import EZShare.server.ServerList;
import EZShare.server.Subscription;
import EZShare.server.WorkerThread;
import com.google.gson.Gson;
import org.apache.commons.cli.*;

import javax.net.ServerSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Nodes {
    /* Default configuration */
    public static String HOST = "localhost";
    public static int PORT = 3785;
    public static final int MAX_THREAD_COUNT = 50;
    public static long INTERVAL = 1000;
    public static long EXCHANGE_PERIOD = 600000;
    public static boolean isUltraNode = false;
    public static final int TIME_OUT = 30000;//each connection time out
    public static final String download_path = "Downloads/";

    /* Data structures and utilities */
    private static final FileList fileList = new FileList();
    private static final ServerList serverList = new ServerList(false);
    public static final Logger logger = LogCustomFormatter.getLogger(Nodes.class.getName());
    private static final Gson gson = new Gson();

    /*
     A HashMap to record the mapping from a specified client to the starting time of its last connection
 */
    private static ConcurrentHashMap<String, Long> intervalLimit = new ConcurrentHashMap<>();
    /*
    Currently it is a simple fixed-volume thread pool.
    If no thread resource is available at the moment, it would be blocked until it could get one.
*/
    private static ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREAD_COUNT);
    public static ConcurrentHashMap<Host, Socket> unsecure_relay = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Socket, Subscription> subscriptions = new ConcurrentHashMap<>();

    private static Options commandOptions() {
        //Build up command line options
        Options options = new Options();

        //server command
        options.addOption("advertisedhostname", true, "advertised hostname");
        options.addOption("connectionintervallimit", true, "connection interval limit in seconds");
//        options.addOption("exchangeinterval", true, "exchange interval in seconds");
        options.addOption("selfport", true, "server port, an integer");
        options.addOption("debug", false, "print debug information");
//        options.addOption("sport", true, "server secure port, an integer");
        options.addOption("setserver", false, "set up server");
        //client command
        options.addOption("id", true, "set the ID for subscribe request");
        options.addOption("subscribe", false, "subscribe resource from server");
        options.addOption("fetch", false, "fetch resource from server");
        options.addOption("channel", true, "channel");
        options.addOption("description", true, "resource description");
        options.addOption("exchange", false, "exchange server list with server");
        options.addOption("host", true, "server host, a domain name or IP address");
        options.addOption("name", true, "resource name");
        options.addOption("owner", true, "owner");
        options.addOption("targetport", true, "server port, an integer");
        options.addOption("publish", false, "publish resources from server");
        options.addOption("query", false, "query for resources from server");
        options.addOption("remove", false, "remove resource from server");
        options.addOption("servers", true, "server list, host1:port1,host2:port2,...");
        options.addOption("share", false, "share resource on server");
        options.addOption("tags", true, "resource tags, tag1,tag2,tag3,...");
        options.addOption("uri", true, "resource URI");
        options.addOption("help", false, "help");

        options.addOption("isUltraNode",true,"set as ultra node");
        //parse command line arguments
        return options;
    }
    private synchronized static void forward(Socket socket){
        try{
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            socket.setSoTimeout(1);
            String resource = inputStream.readUTF();

            if (!resource.contains("resultSize")&&!resource.contains("error")&&!resource.contains("success")){
                ResourceTemplate resourceTemplate = gson.fromJson(resource,ResourceTemplate.class);
                fileList.sendNotification(resourceTemplate);
            }
        }catch (IOException e){

        }
    }
    /**
     * Validator of Command Line options. Prevent user typing multiple commands.
     *
     * @param line command line args after parsing.
     * @return validation of the command line args.
     */
    private static boolean optionsValidator(CommandLine line) {
        //make sure only one command option appears in commandline args
        int count = 0;
        if (line.hasOption("query")) {
            count++;
        }
        if (line.hasOption("publish")) {
            count++;
        }
        if (line.hasOption("share")) {
            count++;
        }
        if (line.hasOption("exchange")) {
            count++;
        }
        if (line.hasOption("remove")) {
            count++;
        }
        if (line.hasOption("fetch")) {
            count++;
        }
        if (line.hasOption("subscribe")) {
            count++;
        }
        return (count <= 1);
    }
    /**
     * Construct Host object via command line args
     *
     * @param line command line args
     * @return Host object
     */

    private static Host getHost(CommandLine line) {
        //parse commandline args to Host object
        String hostname = line.getOptionValue("host", "localhost");
        Integer port = Integer.valueOf(line.getOptionValue("targetport", "3785"));
        return new Host(hostname, port);
    }

    /**
     * Construct ResourceTemplate object from command line args.
     *
     * @param line command line args.
     * @return ResourceTemplate object.
     */
    private static ResourceTemplate getResourceTemplate(CommandLine line) {
        //parse commandline args to ResourceTemplate object
        String channel = line.getOptionValue("channel", "");
        String name = line.getOptionValue("name", "");
        String[] tags = {};
        if (line.hasOption("tags")) {
            tags = line.getOptionValue("tags").split(",");
        }
        String description = line.getOptionValue("description", "");
        String uri = (line.hasOption("uri")) ? line.getOptionValue("uri") : "";
        String owner = line.getOptionValue("owner", "");
        String ezserver = line.getOptionValue("servers", "");

        return new ResourceTemplate(channel, name, tags, description, uri, owner, ezserver);
    }

    /**
     * Send Messages to the server.
     *
     * @param output the output stream of the socket.
     * @param JSON   the json string to be sent.
     */
    private static void sendMessage(DataOutputStream output, String JSON) throws IOException {
        //send message to server
        output.writeUTF(JSON);
        output.flush();

        //log
        logger.fine("SENT:" + JSON);
    }

    /**
     * Process query command. Can also be utilized in Server WorkerThread
     *
     * @param socket           The socket connected to target server.
     * @param resourceTemplate The encapsulation of the resource.
     */
    private static void queryCommand(Socket socket, ResourceTemplate resourceTemplate) throws IOException {

        socket.setSoTimeout(TIME_OUT);
        List<ResourceTemplate> result = new ArrayList<>();

        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());

        logger.fine("querying to :" + socket.getRemoteSocketAddress());

        QueryMessage queryMessage = new QueryMessage(resourceTemplate, true);

        String JSON = gson.toJson(queryMessage);
        sendMessage(output, JSON);

        String response = input.readUTF();

        //receive response
        if (response.contains("success")) {
            //if success print resources
            logger.fine("RECEIVE :" + response);
            response = input.readUTF(); //discard success message
            while (!response.contains("resultSize")) {
                //print out resources
                ResourceTemplate r = gson.fromJson(response, ResourceTemplate.class);
                result.add(r);
                System.out.println(response);
                response = input.readUTF();
            }
            //receive result size for successful request
            logger.fine("RECEIVE_ALL:" + response);
        } else if (response.contains("error")) {
            //when error occur
            logger.warning("RECEIVED:" + response);
        }
    }

    /**
     * Process publish command.
     *
     * @param socket           The socket connected to target server.
     * @param resourceTemplate The encapsulation of the resource.
     */
    private static void publishCommand(Socket socket, ResourceTemplate resourceTemplate) throws IOException {
        socket.setSoTimeout(TIME_OUT);
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());

        logger.fine("publishing to " + socket.getRemoteSocketAddress());

        PublishMessage publishMessage = new PublishMessage(resourceTemplate);

        String JSON = gson.toJson(publishMessage);
        //encyptedJson
        //signature
        //encryptedMessage
        //sendMessage(output,encryptedMessage);
        sendMessage(output, JSON);

        String response = input.readUTF();

        //verify response signature
        //decrypt response Json
        //valid response
        if (response.contains("error")) {
            logger.warning("RECEIVED:" + response);
        }
        if (response.contains("success")) {
            logger.fine("RECEIVED:" + response);
        }

    }

    /**
     * Proceed share command
     * @param socket           The socket connected to target server.
     * @param resourceTemplate The encapsulation of the resource.
     */
    private static void shareCommand(Socket socket, ResourceTemplate resourceTemplate) throws IOException {

        socket.setSoTimeout(TIME_OUT);
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());

        logger.fine("sharing to " + socket.getRemoteSocketAddress());

        ShareMessage shareMessage = new ShareMessage(resourceTemplate);

        String JSON = gson.toJson(shareMessage);
        sendMessage(output, JSON);

        String response = input.readUTF();
        if (response.contains("error")) {
            logger.warning("RECEIVED:" + response);
        }
        if (response.contains("success")) {
            logger.fine("RECEIVED:" + response);
        }

    }

    /**
     * Process remove command.
     *
     * @param socket           The socket connected to target server.
     * @param resourceTemplate The encapsulation of the resource.
     */
    private static void removeCommand(Socket socket, ResourceTemplate resourceTemplate) throws IOException {

        socket.setSoTimeout(TIME_OUT);
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());

        logger.fine("removing to " + socket.getRemoteSocketAddress());

        RemoveMessage removeMessage = new RemoveMessage(resourceTemplate);

        String JSON = gson.toJson(removeMessage);
        sendMessage(output, JSON);

        String response = input.readUTF();
        if (response.contains("error")) {
            logger.warning("RECEIVED:" + response);
        }
        if (response.contains("success")) {
            logger.fine("RECEIVED:" + response);
        }
    }

    /**
     * Process exchange command.
     *
     * @param socket     The socket connected to target server.
     * @param serverList The servers in exchange request.
     */
    private static void exchangeCommand(Socket socket, List<Host> serverList) throws IOException {

        socket.setSoTimeout(TIME_OUT);
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());

        logger.fine("exchanging to :" + socket.getRemoteSocketAddress());

        ExchangeMessage exchangeMessage = new ExchangeMessage(serverList);

        String JSON = gson.toJson(exchangeMessage);
        sendMessage(output, JSON);

        String response = input.readUTF();
        if (response.contains("error")) {
            logger.warning("RECEIVED:" + response);
        }
        if (response.contains("success")) {
            logger.fine("RECEIVED:" + response);
        }

    }

    /**
     * Process fetch command.
     *
     * @param socket           The socket connected to target server.
     * @param resourceTemplate The encapsulation of the resource.
     */
    private static void fetchCommand(Socket socket, ResourceTemplate resourceTemplate) throws IOException {

        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());

        logger.fine("fetching to : " + socket.getRemoteSocketAddress());

        FetchMessage fetchMessage = new FetchMessage(resourceTemplate);

        String JSON = gson.toJson(fetchMessage);
        sendMessage(output, JSON);

        String response = input.readUTF();
        if (response.contains("success")) {

            logger.fine("RECEIVED:" + response);
            //try to read file template
            String file_template = input.readUTF();

            //if result size is 0
            if (file_template.contains("resultSize")) {
                logger.warning("RECEIVED_ALL:" + file_template);
            } else {
                //result exist! parse resource template
                logger.fine("RECEIVE:" + file_template);
                FileTemplate receivedFileTemplate = gson.fromJson(file_template, FileTemplate.class);

                int resource_size = (int) receivedFileTemplate.getResourceSize();

                String name = new File(receivedFileTemplate.getUri()).getName();
                //System.out.println(name);

                //check download directory
                File download_directory = new File(download_path);
                if (!download_directory.exists()) {
                    download_directory.mkdir();
                }

                //create file
                RandomAccessFile randomAccessFile = new RandomAccessFile(download_path + name, "rw");

                //set read buffer size
                int buffer_size = 1024;

                buffer_size = resource_size > buffer_size ? buffer_size : resource_size;

                byte[] buffer = new byte[buffer_size];

                // # of bytes to be received
                int to_receive = resource_size;

                // # of bytes received per time
                int received;

                //read byte from socket until the last chunk
                while (to_receive > buffer_size && (received = input.read(buffer)) != -1) {
                    //write file
                    randomAccessFile.write(Arrays.copyOf(buffer, received));
                    //note down how many bytes received
                    to_receive -= received;

                    //System.out.println(to_receive);
                    //if there is only one chunk to receive, break to prevent the lost of result_size information

                }

                if (to_receive > 0) {
                    //set the buffer to the length of the last chunk
                    buffer = new byte[to_receive];

                    //read last chunk and write to file
                    received = input.read(buffer);
                    randomAccessFile.write(Arrays.copyOf(buffer, received));
                }
                //close file
                randomAccessFile.close();

                //read resourceSize
                response = input.readUTF();
                logger.fine("RECEIVED:" + response);

            }

        } else if (response.contains("error")) {
            logger.warning("RECEIVED:" + response);
        }

    }

    /**
     * Process subscribe command.
     *
     * @param socket           The socket connected to target server.
     * @param resourceTemplate The query condition of subscribed resources.
     * @param relay            Whether the subscribe command will be relayed to other servers.
     * @param id               The id of the subscription.
     * @throws IOException Exception in data stream.
     */
    private static void subscribeCommand(Socket socket, ResourceTemplate resourceTemplate, boolean relay, String id) throws IOException {

        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());

        logger.fine("subscribing to :" + socket.getRemoteSocketAddress());

        //construct subscribe message.
        SubscribeMessage subscribeMessage = new SubscribeMessage(relay, id, resourceTemplate);

        String JSON = gson.toJson(subscribeMessage);
        sendMessage(output, JSON);

        String response = input.readUTF();

        //if successfully subscribed
        if (response.contains("success")) {
            logger.fine("RECEIVED:" + response);

            //hold connection until press enter.
            socket.setSoTimeout(1);
            while (System.in.available() == 0) {

                //check available resource and print out.
                try {
                    String resource = input.readUTF();
                    System.out.println(resource);
                } catch (IOException e) {
                    //just to prevent blocking in SSLSocket.
                }

            }

            socket.setSoTimeout(3000);
            //Termination
            //construct unsubscribe message.
            UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(id);

            JSON = gson.toJson(unsubscribeMessage);
            sendMessage(output, JSON);

            //read result size
            response = input.readUTF();

            logger.info("RECEIVED:" + response);


        } else if (response.contains("error")) {
            logger.warning("RECEIVED:" + response);
        }


    }

    private static void commandParse(CommandLine cmdLine) {
        try {
            if (cmdLine.hasOption("advertisedhostname")) {
                HOST = cmdLine.getOptionValue("advertisedhostname");
//                System.err.println("test hostname");
            }
            if (cmdLine.hasOption("connectionintervallimit")) {
                INTERVAL = Integer.parseInt(cmdLine.getOptionValue("connectionintervallimit"));
            }
            if (cmdLine.hasOption("selfport")) {
                PORT = Integer.parseInt(cmdLine.getOptionValue("selfport"));
            }
            if (cmdLine.hasOption("isUltraNode")) {
                isUltraNode = Boolean.parseBoolean(cmdLine.getOptionValue("isUltraNode"));
                logger.info("Is ultra node:" + isUltraNode);
//                System.err.println(cmdLine.getOptionValue("isUltraNode"));
            }
            // if debug not toggle, cancel all logs.
            if (!cmdLine.hasOption("debug")) {
                logger.setFilter((LogRecord record) -> (false));
//                System.err.println("no debug on");
            } else {
                logger.info("setting debug on");
            }
            if (cmdLine.hasOption("setserver")) {
//            System.err.println("command line parsed end");
                logger.info("Using advertised hostname: " + HOST);
                logger.info(String.valueOf("Using connection interval limit: " + INTERVAL));
                /* Create ServerSocket */
                ServerSocketFactory factory = ServerSocketFactory.getDefault();
                ServerSocket serverSocket = factory.createServerSocket(PORT);
                logger.info("Bound to port " + PORT);
                logger.info("ServerSocket initialized.");
                logger.info("Waiting for client connection..");
                /* Start listening */
                Thread plainSocket = new Thread(() -> {
                    while (true) {
                        try {
                            Socket client = serverSocket.accept();
                            //encrypt msg with rsa@larry
                            /* Upper bound of simultaneous connections */
                            //set up connection within time period
                            String clientIP = client.getInetAddress().getHostAddress();
                            long currentTime = System.currentTimeMillis();
                            if (!intervalLimit.containsKey(clientIP) || (currentTime - intervalLimit.get(clientIP) > INTERVAL)) {
                                /* Update the time record */
                                intervalLimit.put(clientIP, currentTime);
                                /* Assign a worker thread for this socket. */
//                                System.out.println("begin test for threadpool");
                                try {
                                    Nodes.threadPool.submit(new WorkerThread(client, fileList, serverList, false,isUltraNode));
                                }catch (Exception e) {
                                    e.printStackTrace();
                                    logger.log(Level.WARNING, "{0} cannot create stream", client.getRemoteSocketAddress().toString());
                                    client.close();
                                }
//                                System.out.println("end test for threadpool");
                            } else {
                                /* Violation */
                                client.close();
                            }
                        } catch (IOException ex) {
                            logger.warning(ex.getMessage());
                        }
                    }
                });
                Thread listener = new Thread(() -> {
                    while (true){
                        for (Map.Entry<Host,Socket> entry: Nodes.unsecure_relay.entrySet()) {
                            forward(entry.getValue());
                        }
                    }
                });
                plainSocket.start();
                listener.start();
            }
            if (!optionsValidator(cmdLine)) {
                //check command for original client
                throw new ParseException("Multiple command options!");
            }
            //get destination host from commandline args
            Host host = getHost(cmdLine);

            //get resource template from command args
            ResourceTemplate resourceTemplate = getResourceTemplate(cmdLine);

            // get plain Socket
            Socket socket = new Socket();

            /* Connect! */
//            System.err.println(host.getHostname()+ String.valueOf(host.getPort()));
//            System.err.println(host);

            socket.connect(new InetSocketAddress(host.getHostname(), host.getPort()), TIME_OUT);

            //proceed commands
            String error_message = null;    //error message when command line options missing

            if (cmdLine.hasOption("query")) {
                queryCommand(socket, resourceTemplate);
            }//should be fine @larry

            if (cmdLine.hasOption("publish")) {
                if (!cmdLine.hasOption("uri")) {
                    error_message = "URI is missing.";
                } else {
                    publishCommand(socket, resourceTemplate);
                }
            }//should be fine @larry

            /*remove should be changed to only remove own resources @larry*/
            if (cmdLine.hasOption("remove")) {
                if (!cmdLine.hasOption("uri")) {
                    error_message = "URI is missing.";
                } else {
                    removeCommand(socket, resourceTemplate);
                }
            }
            /* share command has to change @larry*/
            if (cmdLine.hasOption("share")) {
                if (!cmdLine.hasOption("uri") || !cmdLine.hasOption("secret")) {
                    error_message = "URI or secret missing.";
                } else {
                    shareCommand(socket, resourceTemplate);
                }
            }
            /*@larry*/
            if (cmdLine.hasOption("exchange")){
                if (!cmdLine.hasOption("servers")) {
                    error_message = "servers missing!";
                } else {
                    //parse commandline args to host list
                    String[] s = cmdLine.getOptionValue("servers").split(",");
                    List<Host> serverList = new ArrayList<>();
                    for (String server : s) {
                        String[] address = server.split(":");
                        serverList.add(new Host(address[0], Integer.valueOf(address[1])));
                    }
                    exchangeCommand(socket, serverList);
                }
            }

            if (cmdLine.hasOption("fetch")) {
                if (!cmdLine.hasOption("uri")) {
                    error_message = "URI is missing.";
                } else {
                    fetchCommand(socket, resourceTemplate);
                }
            }

            if (cmdLine.hasOption("subscribe")) {
                // boolean relay = line.hasOption("relay");
                boolean relay = true;
                //set local IP address as default ID.
                String id = cmdLine.getOptionValue("id", socket.getLocalAddress().toString());
                subscribeCommand(socket, resourceTemplate, relay, id);
            }

            if (error_message != null) {
                logger.warning(error_message);
            }
            System.err.println("server list at the moment :"+serverList.getServerList());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        logger.info("Starting the EZShare F2F system and initialise node");
        /*parse arguments*/
        CommandLineParser parser = new DefaultParser();
        Options options = commandOptions();
        try {
            // parse command line arguments
            CommandLine line = parser.parse(options, args);
            commandParse(line);
        } catch (ParseException e) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("EZShare.Nodes", options);
        }
    }

}
