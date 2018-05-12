package EZShare.message;

/**
 * Encapsulation of Fetch Command.
 * Created by jason on 11/4/17.
 */
public class FetchMessage extends Message{
    private final ResourceTemplate resourceTemplate;
    private boolean relay;
    public boolean isRelay() {
        return relay;
    }
    public void setRelay(boolean relay) {
        this.relay = relay;
    }
    private int mxHops;
    public int getMxHops(){return this.mxHops;}
    public void setMxHops(int mxHops){this.mxHops = mxHops;}

    public FetchMessage(ResourceTemplate resource,int mxHops){
        super("FETCH");
        this.resourceTemplate = resource;
        this.mxHops = mxHops;
        this.relay = true;
    }

    public ResourceTemplate getResource() {
        return resourceTemplate;
    }

    /**
     * Require uri in correct file schema to fetch
     * @return  Whether the fetch request is in correct format.
     */
    @Override
    public boolean isValid() {
        return resourceTemplate.isValid()&&resourceTemplate.isValidFile();
    }
}
