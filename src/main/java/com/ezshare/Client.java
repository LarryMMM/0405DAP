package com.ezshare;

import java.io.*;

import com.ezshare.message.*;
import com.ezshare.log.*;
import com.ezshare.message.FileTemplate;
import com.google.gson.Gson;
import org.apache.commons.cli.*;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * Implementation of Client
 * Created by jason on 10/4/17.
 */
public class Client {

    private static final String download_path = "Downloads/";
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
        //make sure only one command option appears in commandline args
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
        //parse commandline args to Host object
        String hostname = line.hasOption("host")?line.getOptionValue("host",""):"localhost";
        Integer port = line.hasOption("port")?Integer.valueOf(line.getOptionValue("port","")):3780;
        return new Host(hostname,port);
    }

    /**
     * Construct ResourceTemplate object from command line args.
     * @param line command line args.
     * @return ResourceTemplate object.
     */
    private static ResourceTemplate getResourceTemplate(CommandLine line){
        //parse commandline args to ResourceTemplate object
        String channel = line.getOptionValue("channel","");
        String name = line.getOptionValue("name","");
        String[] tags = (line.hasOption("tags"))?line.getOptionValue("tags").split(","):null;
        String description = line.getOptionValue("description","");
        String uri = (line.hasOption("uri"))?line.getOptionValue("uri"):"";
        String owner = line.getOptionValue("owner","");
        String ezserver = line.getOptionValue("ezserver","");

        return new ResourceTemplate(channel,name,tags,description,uri,owner,ezserver);

    }

    /**
     * Send Messages to the server.
     * @param output the output stream of the socket.
     * @param JSON  the json string to be sent.
     */
    private static void sendMessage(DataOutputStream output, String JSON) throws IOException{
        //send message to server
        output.writeUTF(JSON);
        output.flush();

        //log
        if(debug) logger.fine("SENT:"+JSON);
    }

    /**
     * Read simple response.
     * @param input Input stream of the socket
     * @return  The exact response of server or timeout info.
     * @throws IOException  when connection failed or timeout.
     */
    private static String readResponse(DataInputStream input) throws IOException{
        //set timer
        long t1 = System.currentTimeMillis();

        while (true){

            long t2 = System.currentTimeMillis();
            if(input.available()>0){
                return input.readUTF();
            }
            //set timeout
            if (t2-t1>5000){throw new IOException("TIMEOUT");}
        }
    }

    /**
     * Handle IOException of Socket connection.
     * @param e IOException object.
     * @param socket    Socket that threw the exception.
     */
    private static void IOExceptionHandler(IOException e, Socket socket){
        if (e.getMessage().equals("TIMEOUT")){
            if (debug) logger.warning("Connection Timeout!"+socket.getRemoteSocketAddress());}
        else {
            if (debug) logger.warning("Connection Failure."+socket.getRemoteSocketAddress());}
    }

    /**
     * Process query command.
     * Can also be utilized in Server WorkerThread
     * @param socket    The socket connected to target server.
     * @param resourceTemplate  The encapsulation of the resource.
     */
    public static List<ResourceTemplate> queryCommand(Socket socket,ResourceTemplate resourceTemplate){

        List<ResourceTemplate> result = new ArrayList<>();

        try{

            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            if(debug) logger.fine("querying to "+socket.getRemoteSocketAddress());

            QueryMessage queryMessage = new QueryMessage(resourceTemplate,true);

            String JSON = gson.toJson(queryMessage);
            sendMessage(output,JSON);

            String response = readResponse(input);

            //receive response
            if(response.contains("success")){
                //if success print resources
                if(debug)   logger.fine("RECEIVE:"+response);
                response = readResponse(input); //discard success message
                while (!response.contains("resultSize")){
                    //print out resources
                    ResourceTemplate r = gson.fromJson(response,ResourceTemplate.class);
                    result.add(r);
                    System.out.println(response);
                    response = readResponse(input);
                }
                //receive result size for successful request
                if(debug)   logger.fine("RECEIVE_ALL:"+response);
            }else if(response.contains("error")){
                //when error occur
                if(debug) logger.warning("RECEIVED:"+response);
            }


        } catch (IOException e){
            IOExceptionHandler(e,socket);
        }finally {

            //try to terminate connection no matter what happened
            try {
                socket.close();
            }catch (IOException e){
                if (debug) logger.warning("Unable to disconnect!");
            }

        }
        return result;
    }

    /**
     * Process publish command.
     * @param socket    The socket connected to target server.
     * @param resourceTemplate  The encapsulation of the resource.
     */
    private static void publishCommand(Socket socket,ResourceTemplate resourceTemplate){
        try{

            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            if(debug) logger.fine("publishing to "+socket.getRemoteSocketAddress());

            PublishMessage publishMessage = new PublishMessage(resourceTemplate);

            String JSON = gson.toJson(publishMessage);
            sendMessage(output,JSON);

            String response = readResponse(input);

            if(response.contains("error")&&debug)
                logger.warning("RECEIVED:"+response);
            if(response.contains("success")&&debug)
                logger.fine("RECEIVED:"+response);



        } catch (IOException e){
            IOExceptionHandler(e,socket);
        } finally {
            try {
                socket.close();
            }catch (IOException e){
                if (debug) logger.warning("Unable to disconnect!");
            }
        }
    }

    /**
     * Proceed share command
     * @param secret    Secret of the server.
     * @param socket    The socket connected to target server.
     * @param resourceTemplate  The encapsulation of the resource.
     */
    private static void shareCommand(Socket socket,String secret,ResourceTemplate resourceTemplate){

        try{

            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            if(debug) logger.fine("sharing to "+socket.getRemoteSocketAddress());

            ShareMessage shareMessage = new ShareMessage(resourceTemplate,secret);

            String JSON = gson.toJson(shareMessage);
            sendMessage(output,JSON);

            String response = readResponse(input);
            if(response.contains("error")&&debug)
                logger.warning("RECEIVED:"+response);
            if(response.contains("success")&&debug)
                logger.fine("RECEIVED:"+response);


        } catch (IOException e){
            IOExceptionHandler(e,socket);
        }finally {
            try {
                socket.close();
            }catch (IOException e){
                if (debug) logger.warning("Unable to disconnect!");
            }
        }
    }

    /**
     *  Process remove command.
     * @param socket    The socket connected to target server.
     * @param resourceTemplate  The encapsulation of the resource.
     */
    private static void removeCommand(Socket socket,ResourceTemplate resourceTemplate){


        try{

            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            if(debug) logger.fine("removing to "+socket.getRemoteSocketAddress());

            RemoveMessage removeMessage = new RemoveMessage(resourceTemplate);

            String JSON = gson.toJson(removeMessage);
            sendMessage(output,JSON);

            String response = readResponse(input);
            if(response.contains("error")&&debug)
                logger.warning("RECEIVED:"+response);
            if(response.contains("success")&&debug)
                logger.fine("RECEIVED:"+response);


        } catch (IOException e){
            IOExceptionHandler(e,socket);
        }finally {
            try {
                socket.close();
            }catch (IOException e){
                if (debug) logger.warning("Unable to disconnect!");
            }
        }
    }

    /**
     * Process exchange command.
     * @param socket    The socket connected to target server.
     * @param serverList The servers in exchange request.
     */
    private static void exchangeCommand(Socket socket,List<Host> serverList){

        try{

            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            if(debug) logger.fine("exchanging to "+socket.getRemoteSocketAddress());

            ExchangeMessage exchangeMessage = new ExchangeMessage(serverList);

            String JSON = gson.toJson(exchangeMessage);
            sendMessage(output,JSON);

            String response = readResponse(input);
            if(response.contains("error")&&debug)
                logger.warning("RECEIVED:"+response);
            if(response.contains("success")&&debug)
                logger.fine("RECEIVED:"+response);

        } catch (IOException e){
            IOExceptionHandler(e,socket);
        } finally {
            try {
                socket.close();
            }catch (IOException e){
                if (debug) logger.warning("Unable to disconnect!");
            }
        }
    }

    /**
     * Process fetch command.
     * @param socket    The socket connected to target server.
     * @param resourceTemplate  The encapsulation of the resource.
     */
    private static void fetchCommand(Socket socket,ResourceTemplate resourceTemplate){


        try{
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            if(debug) logger.fine("fetching to "+socket.getRemoteSocketAddress());

            FetchMessage fetchMessage = new FetchMessage(resourceTemplate);

            String JSON = gson.toJson(fetchMessage);
            sendMessage(output,JSON);

            String response = readResponse(input);
            if(response.contains("success")){

                if(debug)logger.fine("RECEIVED:"+response);
                //try to read file template
                String file_template = readResponse(input);

                if(debug) logger.fine("RECEIVE:"+file_template);
                //if result size is 0
                if (response.contains("resultSize")){
                    if(debug)logger.warning("RECEIVED:"+response);
                }
                else {
                //result exist! parse resource template
                FileTemplate receivedFileTemplate = gson.fromJson(file_template,FileTemplate.class);

                int resource_size = receivedFileTemplate.getResourceSize();

                byte[] download = new byte[resource_size];


                String name = new File(receivedFileTemplate.getUri()).getName();
                //System.out.println(name);

                //check download directory
                File download_directory = new File(download_path);
                if (!download_directory.exists())
                    download_directory.mkdir();

                //create file
                FileOutputStream fileOutputStream = new FileOutputStream(download_path+name);

                //read exact all file bytes from server
                input.readFully(download);

                //write file
                fileOutputStream.write(download);

                fileOutputStream.close();

                //read resourceSize
                response = readResponse(input);
                logger.fine("RECEIVED:"+response);

                }

            }else if(response.contains("error")){
                if(debug)   logger.warning("RECEIVED:"+response);
            }

        } catch (IOException e){
            IOExceptionHandler(e,socket);
        } finally {
            try {
                socket.close();
            }catch (IOException e){
                if (debug) logger.warning("Unable to disconnect!");
            }
        }

    }


    public static void main(String[] args){

        //Initialize command line parser and options
        CommandLineParser parser = new DefaultParser();
        Options options = commandOptions();

        Socket socket;

        try{
            //parse command line arguments
            CommandLine line = parser.parse(options,args);

            //validate the command option is unique.
            if(!optionsValidator(line)){throw new ParseException("Multiple command options!");}

            //set debug on if toggled
            debug = line.hasOption("debug");
            if (debug) logger.info("setting debug on");

            //get destination host from commandline args
            Host host = getHost(line);

            //get resource template from command args
            ResourceTemplate resourceTemplate = getResourceTemplate(line);

            //connect to server
            socket = new Socket(host.getHostname(),host.getPort());

            //proceed commands

            String error_message = null;    //error message when command line options missing

            if(line.hasOption("query")){queryCommand(socket,resourceTemplate);}

            if(line.hasOption("publish")){
                if(!line.hasOption("uri")){
                    error_message = "URI is missing.";}
                else
                    publishCommand(socket,resourceTemplate);}

            if(line.hasOption("remove")){
                if(!line.hasOption("uri")){
                    error_message = "URI is missing.";}
                else
                    removeCommand(socket,resourceTemplate);}

            if(line.hasOption("share")){
                if(!line.hasOption("uri")||!line.hasOption("secret")){
                    error_message = "URI or secret missing.";}
                else
                    shareCommand(socket,line.getOptionValue("secret"),resourceTemplate);}

            if(line.hasOption("exchange")){
                if(!line.hasOption("servers")){
                    error_message = "servers missing.";}
                else {

                    //parse commandline args to host list
                    String[] s = line.getOptionValue("servers").split(",");
                    List<Host> serverList = new ArrayList<>();
                    for (String server:s) {
                        String[] address = server.split(":");
                        serverList.add(new Host(address[0],Integer.valueOf(address[1])));
                    }

                    exchangeCommand(socket,serverList);}
            }

            if(line.hasOption("fetch")){
                if (!line.hasOption("uri")){
                    error_message = "URI is missing.";
                }else fetchCommand(socket,resourceTemplate);
            }

            if(error_message!=null&&debug){
                logger.warning(error_message);
            }
            socket.close();


        }catch (ParseException e){
            //If commandline args invalid, show help info.
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("EZShare.Client",options);

        }catch (IOException e){
            if (debug) logger.warning("Unable to Create Socket!");
        }catch (ArrayIndexOutOfBoundsException | NumberFormatException e){
            //when value of -servers option invalid
            if (debug) logger.warning("Server address invalid.");
        }
    }




}
