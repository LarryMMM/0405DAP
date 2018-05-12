package EZShare.message;

/**
 * Encapsulation of subscribe message
 *
 * @author jason
 */
public class SubscribeMessage extends Message {

    private boolean relay;
    private String id;
    private ResourceTemplate resourceTemplate;
    private int mxHops;
    public SubscribeMessage(boolean relay, String id, ResourceTemplate resourceTemplate, int mxHops ) {
        super("SUBSCRIBE");
        this.relay = relay;
        this.id = id;
        this.resourceTemplate = resourceTemplate;
        this.mxHops = mxHops;
    }
    public int getMxHops(){return this.mxHops;}
    public void setMxHops(int mxHops){this.mxHops = mxHops;}
    public boolean isRelay() {
        return relay;
    }
    public void setRelay(boolean relay) {
        this.relay = relay;
    }
    public ResourceTemplate getResourceTemplate() {
        return resourceTemplate;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean isValid() {
        return resourceTemplate.isValid() && id != null;
    }
}
