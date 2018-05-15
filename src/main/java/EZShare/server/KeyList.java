package EZShare.server;
import EZShare.Nodes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;


public class KeyList {
    private ConcurrentHashMap<String,String> keyList = new ConcurrentHashMap<>();
//    public KeyList(ConcurrentHashMap<String,String> keyList){this.keyList=keyList;}
    public KeyList(){

    }
    public synchronized void updateKeyList(ConcurrentHashMap<String,String> inputKeyList) {
        for(String clientId : inputKeyList.keySet()){
            if (!keyList.containsKey(clientId)){
                keyList.put(clientId,inputKeyList.get(clientId));
            }else {
                keyList.replace(clientId,inputKeyList.get(clientId));
            }
        }
    }
    public synchronized void removeKeyList(String clientId){
        try{
            keyList.remove(clientId);
        }catch (Exception e){
            Nodes.logger.log(Level.WARNING,"{0} : key cannot be removed!",clientId);
        }
    }
}
