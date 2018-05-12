package EZShare.message;

import EZShare.Nodes;

import javax.xml.soap.Node;
import java.util.logging.Level;

/**
 * Encapsulation of Share Command.
 * Created by jason on 11/4/17.
 */
public class ShareMessage extends Message{

    private final ResourceTemplate resource;


    public ShareMessage(ResourceTemplate resource){
        super("SHARE");
        this.resource = resource;
    }

    public ResourceTemplate getResource() {
        return resource;
    }


    /**
     * URI in share request can only be in file schema.
     * @return  Whether the request is valid to query.
     */
    @Override
    public boolean isValid() {
//        Nodes.logger.log(Level.INFO, "{0} : valid resource?", resource.isValid());
//        Nodes.logger.log(Level.INFO, "{0} : valid File?", resource.isValidFile());

        return resource.isValid()&&resource.isValidFile();
    }
}
