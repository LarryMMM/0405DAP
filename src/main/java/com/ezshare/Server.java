package com.ezshare;

import com.ezshare.log.LogCustomFormatter;
import com.ezshare.server.FileList;
import com.ezshare.server.ServerList;
import com.ezshare.server.WorkerThread;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.net.ServerSocketFactory;

/**
 *
 * @author Wenhao Zhao, Ying Li
 */
public class Server {

    /* Configuration */
    public static String HOST = "localhost";
    public static int PORT = 3000;
    public static final int MAX_THREAD_COUNT = 10;
    public static long EXCHANGE_PERIOD = 600000;
    public static long INTERVAL = 1000;
    public static String SECRET = random(26);
    public static boolean DEBUG=false;
    
    public static final Logger logger = LogCustomFormatter.getLogger(Server.class.getName());
    
    private static final FileList fileList = new FileList();
    private static final ServerList serverList = new ServerList();
    
    /*
        Currently it is a simple fixed-volume thread pool.
        If no thread resource is available at the moment, it would be blocked until it could get one.
    */
    private static ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREAD_COUNT);
    
    /*
        A HashMap to record the mapping from a specified client to the starting time of its last connection
    */
    private static ConcurrentHashMap<String, Long> intervalLimit = new ConcurrentHashMap<>();

    /**
     * Construct command line options
     * @return  CommandLine options.
     *
     */
    private static Options commandOptions() {
        //Build up command line options
        Options options = new Options();
        options.addOption("advertisedhostname", true, "advertised hostname");
        options.addOption("connectionintervallimit", true, "connection interval limit in seconds");
        options.addOption("exchangeinterval", true, "exchange interval in seconds");
        options.addOption("port", true, "server port, an integer");
        options.addOption("secret", true, "secret");
        options.addOption("debug", false, "print debug information");

        //parse command line arguments
        return options;
    }

    public static String random(int length) {
        String str = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int num = random.nextInt(36);
            buf.append(str.charAt(num));
        }
        return buf.toString();
    }

    public static void main(String[] args) {
        logger.info("Starting the EZShare Server");
        /* Timer running as a daemon thread schedules the regular EXCHANGE command. */
        Timer timer = new Timer(true);
        TimerTask regularExchangeTask = new TimerTask() {   
            @Override
            public void run() {
                serverList.regularExchange();
            }   
        };   
        timer.schedule(regularExchangeTask, 0, EXCHANGE_PERIOD);
        
        /* Receive requests. */
        ServerSocketFactory factory = ServerSocketFactory.getDefault();

        CommandLineParser parser = new DefaultParser();
        Options options = commandOptions();

        try {
            //parse command line arguments
            CommandLine line = parser.parse(options,args);

            if(line.hasOption("advertisedhostname")){
                HOST = line.getOptionValue("advertisedhostname");

            }
            if(line.hasOption("connectionintervallimit")){
                INTERVAL = Integer.parseInt(line.getOptionValue("connectionintervallimit"));

            }
            if(line.hasOption("exchangeinterval")){
                EXCHANGE_PERIOD = Integer.parseInt(line.getOptionValue("exchangeinterval"));

            }
            if(line.hasOption("port")){
                PORT = Integer.parseInt(line.getOptionValue("port"));
            }
            if(line.hasOption("secret")){
                SECRET = line.getOptionValue("secret");

            }
            //if debug not toggle, cancel all logs.
            if(!line.hasOption("debug")) {
                logger.setFilter((LogRecord record)->(false));}
            else {
                logger.info("setting debug on");
            }

            logger.info("using advertised hostname: "+HOST);
            logger.info(String.valueOf("using connection interval limit: "+INTERVAL));
            logger.info(String.valueOf("using exchange interval period: "+EXCHANGE_PERIOD));
            logger.info("using secret: "+SECRET);
            logger.info("bound to port "+PORT);
            
            /* Start listening */
            ServerSocket server = factory.createServerSocket(PORT);

            System.out.println("ServerSocket initialized.");
            System.out.println("Waiting for client connection..");
            logger.info("started");

            /* Wait for connections. */
            while (true) {
                Socket client = server.accept();

                String clientIP = client.getInetAddress().getHostAddress();
                long currentTime = System.currentTimeMillis();
                if (!intervalLimit.containsKey(clientIP) || (currentTime - intervalLimit.get(clientIP) > INTERVAL)) {
                    /* Update the time record */
                    intervalLimit.put(clientIP, currentTime);
                    
                    /* Assign a worker thread for this socket. */
                    try {
                        threadPool.submit(new WorkerThread(client, fileList, serverList));
                    } catch (IOException e){
                        logger.log(Level.WARNING, "{0} cannot create stream", client.getRemoteSocketAddress().toString());
                        client.close();
                    }

                } else {
                    /* Violation */
                    client.close();
                }
            }
        } catch (IOException ex) {
            logger.warning(ex.getMessage());
        } catch (ParseException ex) {
            //If commandline args invalid, show help info.
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("EZShare.Server",options);
        }
    }
}