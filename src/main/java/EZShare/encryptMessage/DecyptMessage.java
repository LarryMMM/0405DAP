package EZShare.encryptMessage;

import com.google.gson.Gson;

public class DecyptMessage {
    private static final String[] valid_commands = {"MESSAGE","SIGNATURE","SENDER"};
    /**
     * Base Class of All EncryptedMessages
     * Created by jason on 13/5/18.
     */
    private final String decryptedMessage;
    private final String signature;
    private final String sender;
    public DecyptMessage(String encryptedMessage,String signature,String sender){
        this.decryptedMessage = encryptedMessage;
        this.signature = signature;
        this.sender = sender;
    }

    public String getDecryptedMessage() {
        return decryptedMessage;
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
