package EZShare.encryptMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class EncryptMessage {
    private static final String[] valid_commands = {"MESSAGE","SIGNATURE"};
    /**
     * Base Class of All EncryptedMessages
     * Created by jason on 13/5/18.
     */
    private final JsonObject encryptedMessage;
    private final String signatureMessage;
    public EncryptMessage(JsonObject encryptedMessage,String signatureMessage){
            this.encryptedMessage = encryptedMessage;
            this.signatureMessage = signatureMessage;
        }

    public JsonObject getEncryptedMessage() {
            return encryptedMessage;
        }

        /**
         * Validate command name. IMPORTANT:CASE SENSITIVE!
         * @return  Whether the command name is valid.
         */
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}

