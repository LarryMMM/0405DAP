package EZShare.server;

import EZShare.message.SubscribeMessage;

/**
 * Encapsulation of subscriptions
 *
 * @author zenanz
 */
public class Subscription {
    private int reusltsize = 0;
    private SubscribeMessage subscribeMessage;

    public Subscription(SubscribeMessage subscribeMessage){
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

}
