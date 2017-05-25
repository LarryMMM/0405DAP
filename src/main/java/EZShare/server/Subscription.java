package EZShare.server;

import EZShare.message.Host;
import EZShare.message.SubscribeMessage;

/**
 * Encapsulation of subscriptions
 *
 * @author zenanz
 */
public class Subscription {
    private int resultSize = 0;
    private SubscribeMessage subscribeMessage;
    private String origin;
    private Host target;
    private boolean secure;

    public Subscription(SubscribeMessage subscribeMessage, String origin, Host target, boolean secure) {
        this.origin = origin;
        this.target = target;
        this.subscribeMessage = subscribeMessage;
        this.secure = secure;
    }

    public Subscription(SubscribeMessage subscribeMessage, String origin, boolean secure) {
        this.origin = origin;
        this.subscribeMessage = subscribeMessage;
        this.secure = secure;
    }


    public SubscribeMessage getSubscribeMessage() {
        return this.subscribeMessage;
    }

    @Override
    public String toString() {
        return this.subscribeMessage.getId() + "|currentSize:" + resultSize;
    }

    public int getResultSize() {
        return resultSize;
    }

    public void addResult(int number) {
        this.resultSize += number;
    }

    public Host getTarget() {
        return target;
    }

    public String getOrigin() {
        return origin;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

}
