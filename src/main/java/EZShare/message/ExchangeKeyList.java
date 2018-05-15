package EZShare.message;

import java.util.concurrent.ConcurrentHashMap;

public class ExchangeKeyList extends Message{
//    private final ConcurrentHashMap<String,PublicKey> keyList;
private final ConcurrentHashMap<String,String> keyList;
    public ExchangeKeyList(ConcurrentHashMap<String,String> keyList){
        super("EXCHANGEKEY");
        this.keyList = keyList;
    }
//    public ExchangeKeyList(ConcurrentHashMap<String,PublicKey> keyList){
//        super("EXCHANGEKEY");
//        this.keyList = keyList;
//    }

//    public ConcurrentHashMap<String,PublicKey> getKeyList() {return keyList;}
    public ConcurrentHashMap<String,String> getKeyList() {return keyList;}

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
