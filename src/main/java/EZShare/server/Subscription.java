package EZShare.server;

import EZShare.Server;
import EZShare.message.Host;
import EZShare.message.SubscribeMessage;
import EZShare.message.UnsubscribeMessage;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;

/**
 * Encapsulation of subscriptions
 *
 * @author zenanz
 */
public class Subscription {
    private int reusltsize = 0;
    private SubscribeMessage subscribeMessage;
    private String orgin;
    private Host target;

    public Subscription(SubscribeMessage subscribeMessage,String orgin, Host target){
        this.orgin = orgin;
        this.target = target;
        this.subscribeMessage = subscribeMessage;
    }

    public Subscription(SubscribeMessage subscribeMessage,String orgin){
        this.orgin = orgin;
        this.subscribeMessage = subscribeMessage;
    }



    public SubscribeMessage getSubscribeMessage(){
        return this.subscribeMessage;
    }

    @Override
    public String toString() {
        return this.subscribeMessage.getId()+"|currentsize:"+reusltsize;
    }

    public int getReusltsize(){
        return reusltsize;
    }

    public void addResult(int number){
        this.reusltsize+=number;
    }

    public Host getTarget() {
        return target;
    }

    public String getOrgin() {
        return orgin;
    }

}
