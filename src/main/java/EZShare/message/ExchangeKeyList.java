package EZShare.message;

import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

public class ExchangeKeyList{
    private final ConcurrentHashMap<String,PublicKey> keyList;
    private final String command ;
    public ExchangeKeyList(ConcurrentHashMap<String,PublicKey> keyList){
        this.keyList = keyList;
        this.command = "EXCHANGEKEY";
    }
    public String getCommand() {
        return command;
    }
    public ConcurrentHashMap<String,PublicKey> getKeyList() {
        return keyList;
    }

    public boolean isValid() {
        for (String s: keyList.keySet()) {
            String[] address = s.split(":");
            Host h = new Host(address[0], Integer.valueOf(address[1]));
            if(!h.isValid()){
                return false;
            }
        }
        return true;
    }
}
