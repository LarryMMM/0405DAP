package EZShare.encryptMessage;
import com.google.gson.Gson;

public class EncryptMessage extends EncryptIsValidatable{
    private static final String[] valid_commands = {"MESSAGE","SIGNATURE"};
    /**
     * Base Class of All EncryptedMessages
     * Created by jason on 13/5/18.
     */
    private final String command;

    public EncryptMessage(String command){
            this.command = command;
        }

    public String getCommand() {
            return command;
        }

        /**
         * Validate command name. IMPORTANT:CASE SENSITIVE!
         * @return  Whether the command name is valid.
         */
    @Override
    public boolean isValid() {
        for (String c : valid_commands) {
            if(c.equals(this.command))
                return true;
        }
        return false;
    }
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}

