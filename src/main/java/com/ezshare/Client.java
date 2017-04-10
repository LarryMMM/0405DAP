package com.ezshare;

import com.ezshare.message.*;
import com.ezshare.log.*;
import com.google.gson.Gson;
import org.apache.commons.cli.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;


/**
 * Implementation of Client
 * Created by jason on 10/4/17.
 */
public class Client {

    private static final Logger logger = LogCustomFormatter.getLogger(Client.class.getName());
    private static final Gson gson = new Gson();
    private static boolean debug;

    /**
     * Construct command line options
     * @return  CommandLine options.
     */
    private static Options commandOptions() {
        //Build up command line options
        Options options = new Options();
        options.addOption("debug", false, "print debug information");
        options.addOption("fetch", false, "fetch resource from server");
        options.addOption("channel", true, "channel");
        options.addOption("description", true, "resource description");
        options.addOption("exchange", false, "exchange server list with server");
        options.addOption("host", true, "server host, a domain name or IP address");
        options.addOption("name", true, "resource name");
        options.addOption("owner", true, "owner");
        options.addOption("port", true, "server port, an integer");
        options.addOption("publish", false, "publish resources from server");
        options.addOption("query", false, "query for resources from server");
        options.addOption("remove", false, "remove resource from server");
        options.addOption("secret", true, "secret");
        options.addOption("servers", true, "secret");
        options.addOption("share", false, "share resource on server");
        options.addOption("tags", true, "resource tags, tag1,tag2,tag3,...");
        options.addOption("uri", true, "resource URI");
        options.addOption("help", false, "help");

        //parse command line arguments
        return options;
    }

    /**
     *  Validator of Command Line options.
     *  Prevent user typing multiple commands.
     * @param line command line args after parsing.
     * @return validation of the command line args.
     */
    private static boolean optionsValidator(CommandLine line){
        int count = 0;
        if(line.hasOption("query"))count++;
        if(line.hasOption("publish"))count++;
        if(line.hasOption("share"))count++;
        if(line.hasOption("exchange"))count++;
        if(line.hasOption("remove"))count++;
        if(line.hasOption("fetch"))count++;

        return count==1;
    }

    /**
     * Construct Host object via command line args
     * @param line command line args
     * @return Host object
     */
    private static Host getHost(CommandLine line){
        String hostname = line.hasOption("host")?line.getOptionValue("host",""):"localhost";
        Integer port = line.hasOption("port")?Integer.valueOf(line.getOptionValue("port","")):3780;
        return new Host(hostname,port);
    }

    /**
     * Construct ResourceTemplate object from command line args.
     * @param line command line args.
     * @return ResourceTemplate object.
     * @throws URISyntaxException When uri invalid.
     */
    private static ResourceTemplate getResourceTemplate(CommandLine line) throws URISyntaxException{
        String channel = line.getOptionValue("channel","");
        String name = line.getOptionValue("name","");
        String[] tags = (line.hasOption("tags"))?line.getOptionValue("tags").split(","):null;
        String description = line.getOptionValue("description","");
        //URI check
        String uri = (line.hasOption("uri"))?new URI(line.getOptionValue("uri")).toString():"";
        String owner = line.getOptionValue("owner","");
        String ezserver = line.getOptionValue("ezserver","");

        return new ResourceTemplate(channel,name,tags,description,uri,owner,ezserver);

    }

    /**
     * Send Messages to the server.
     * @param output the output stream of the socket.
     * @param JSON  the json string to be sent.
     */
    private static void sendMessage(DataOutputStream output, String JSON) throws IOException {
        //send message to server
        output.writeUTF(JSON);
        output.flush();

        //log
        if(debug) logger.fine("SENT:"+JSON);
    }

    /**
     * Process query command
     * @param host host to query
     * @param resourceTemplate The description of resource.
     */
    public static void queryCommand(Host host,ResourceTemplate resourceTemplate){

        String hostname = host.getHostname();
        Integer port = host.getPort();

        try(Socket socket = new Socket(hostname,port)){

            if(debug) logger.fine("querying to "+hostname+":"+port.toString());

            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            QueryMessage queryMessage = new QueryMessage(resourceTemplate,true);

            String JSON = gson.toJson(queryMessage);
            sendMessage(output,JSON);

            long t1 = System.currentTimeMillis();
            while (true){

                long t2 = System.currentTimeMillis();
                if(input.available()>0){
                    String response = input.readUTF();

                    if(response.contains("success")){
                        if (debug)logger.fine("RECEIVED:"+response);
                        continue;
                    }
                    if(response.contains("error")){
                        if (debug)logger.warning("RECEIVED"+response);
                        break;
                    }
                    if (response.contains("resultSize")){
                        if (debug)logger.fine("RECEIVED_ALL:"+response);
                        break;}

                    logger.info("RESOURCE:"+response);

                }


                if (t2-t1>5000)
                    break;
            }
            socket.close();

        } catch (IOException e){
            if (debug) logger.warning("Unable to connect to "+host.toString());
        }

    }

    public static void main(String[] args){

        CommandLineParser parser = new DefaultParser();
        Options options = commandOptions();

        try{
            CommandLine line = parser.parse(options,args);

            if(!optionsValidator(line)){throw new ParseException("Multiple command options!");}

            debug = line.hasOption("debug");
            if (debug) logger.info("setting debug on");

            Host host = getHost(line);

            ResourceTemplate resourceTemplate = getResourceTemplate(line);

            if(line.hasOption("query")){
                queryCommand(host,resourceTemplate);
            }


        }catch (ParseException e){

            if (debug) logger.warning(e.getMessage());
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("EZShare.Client",options);

        }catch (URISyntaxException e){

            if (debug) logger.warning("Invalid URI");

        }
    }




}
