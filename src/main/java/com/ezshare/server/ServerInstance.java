package com.ezshare.server;

import com.ezshare.log.LogCustomFormatter;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import javax.net.ServerSocketFactory;

/**
 *
 * @author Wenhao Zhao
 */
public class ServerInstance {

    /* Configuration */
    public static String HOST ;
    public static int PORT = 3000;
    public static final int MAX_THREAD_COUNT = 10;
    public static long EXCHANGE_PERIOD = 600000;
    public static long INTERVAL = 1000;
    public static String SECRET = random(26);
    public static boolean DEBUG=false;
    
    public static final Logger logger = LogCustomFormatter.getLogger(ServerInstance.class.getName());
    
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
    private static ConcurrentHashMap<String, Long> intervalLimit = new ConcurrentHashMap<String, Long>();

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
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int num = random.nextInt(36);
            buf.append(str.charAt(num));
        }
        return buf.toString();
    }

    public static void main(String[] args) {
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

        try (ServerSocket server = factory.createServerSocket(PORT)) {
            //parse command line arguments
            CommandLine line = parser.parse(options,args);



            if(line.hasOption("advertisedhostname")){
                HOST = line.getOptionValue("advertisedhostname");
                logger.info("using advertised hostname: "+HOST);
            }
            if(line.hasOption("connectionintervallimit")){
                INTERVAL = Integer.parseInt(line.getOptionValue("connectionintervallimit"));
                logger.info(String.valueOf("using connection interval limit: "+INTERVAL));
            }
            if(line.hasOption("exchangeinterval")){
                EXCHANGE_PERIOD = Integer.parseInt(line.getOptionValue("exchangeinterval"));
                logger.info(String.valueOf("using exchange interval period: "+EXCHANGE_PERIOD));
            }
            if(line.hasOption("port")){
                PORT = Integer.parseInt(line.getOptionValue("port"));
                logger.info(String.valueOf("using port: "+PORT));
            }
            if(line.hasOption("secret")){
                SECRET = line.getOptionValue("secret");
                logger.info("using secret: "+SECRET);
            }
            DEBUG = line.hasOption("debug");
           

            System.out.println("ServerSocket initialized.");
            System.out.println("Waiting for client connection..");

            /* Wait for connections. */
            while (true) {
                Socket client = server.accept();
                
                String clientIP = client.getInetAddress().getHostAddress();
                long currentTime = System.currentTimeMillis();
                if (!intervalLimit.containsKey(clientIP) || (currentTime - intervalLimit.get(clientIP) > INTERVAL)) {
                    /* Update the time record */
                    intervalLimit.put(clientIP, currentTime);
                    
                    /* Assign a worker thread for this socket. */
                    threadPool.submit(new WorkerThread(client, fileList, serverList));
                } else {
                    /* Violation */
                    client.close();
                }
            }
        } catch (IOException ex) {
            logger.warning(ex.getMessage());
        } catch (ParseException e) {
            //If commandline args invalid, show help info.
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("EZShare.Server",options);
        }
    }
}