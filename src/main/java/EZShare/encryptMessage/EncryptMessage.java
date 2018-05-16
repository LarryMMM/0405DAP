package EZShare.encryptMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class EncryptMessage {
    private static final String[] valid_commands = {"MESSAGE","SIGNATURE","SENDER"};
    /**
     * Base Class of All EncryptedMessages
     * Created by jason on 13/5/18.
     */
    private final String encryptedMessage;
    private final String signature;
    private final String sender;
    public EncryptMessage(String encryptedMessage,String signature,String sender){
            this.encryptedMessage = encryptedMessage;
            this.signature = signature;
            this.sender = sender;
    }

    public String getEncryptedMessage() {
            return encryptedMessage;
        }
    public String getSignature(){return  signature;}
    public String getSender(){return sender;}
        /**
         * Validate command name. IMPORTANT:CASE SENSITIVE!
         * @return  Whether the command name is valid.
         */
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}

