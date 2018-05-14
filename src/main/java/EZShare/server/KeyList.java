package EZShare.server;
import EZShare.Nodes;
import EZShare.RSA;
import EZShare.message.Host;

import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;


public class KeyList {
    private static ConcurrentHashMap<String,PublicKey> keyList;
    public KeyList(ConcurrentHashMap<String,PublicKey> keyList){this.keyList=keyList;}
    public KeyList(){

    }
    public synchronized void updateKeyList(ConcurrentHashMap<String,PublicKey> inputKeyList) {
        for(String clientId : inputKeyList.keySet()){
          if (keyList.containsKey(clientId)){
                keyList.replace(clientId,inputKeyList.get(clientId));
          }else {
              keyList.put(clientId,inputKeyList.get(clientId));
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

//
//    private List<RSA> PublicKeyList = new ArrayList<>();
//
//    private ReadWriteLock lock;
//
//    public KeyList() {
//        //PublicKeyList
//        //PrivateKey =
//    }
//
//
//    private Boolean KeyAdd(RSA rsa) {
//
//        lock.writeLock().lock();
//        try {
//            if (PublicKeyList.isEmpty()) {
//                PublicKeyList.add(rsa);
//                //sendNotification(resourceTemplate);
//                return true;
//            } else {
//                for (int i = 0; i < PublicKeyList.size(); i++) {
//                    RSA p = PublicKeyList.get(i);
//                    if (p.getID().equals(rsa.getID())) {
//                        return true;
//                    }
//                }
//            }
//            PublicKeyList.add(rsa);
//            //sendNotification(resourceTemplate);
//            return true;
//
//        } finally {
//            lock.writeLock().unlock();
//        }
//    }
//
//    private Boolean KeySearch(String id) {
//        lock.readLock().lock();
//        try {
//            if (PublicKeyList.isEmpty()) {
//                return false;
//            } else {
//                for (int i = 0; i < PublicKeyList.size(); i++) {
//                    RSA p = PublicKeyList.get(i);
//                    if (p.getID().equals(id)) {
//                        return true;
//                    }
//                }
//            }
//            return false;
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    private Key getPublicKey(String id) {
//        lock.writeLock().lock();
//        try {
//            if(KeySearch(id)) {
//                for (int i = 0; i < PublicKeyList.size(); i++) {
//                    RSA p = PublicKeyList.get(i);
//                    if (p.getID().equals(id)) {
//                        return p.getPublicKey();
//                    }
//                }
//            }
//        } finally {
//            lock.writeLock().unlock();
//        }
//        return null;
//    }
//
//    private Boolean KeyDelete(String id) {
//
//        lock.writeLock().lock();
//        try {
//            if (PublicKeyList.isEmpty()) {
//                //sendNotification(resourceTemplate);
//                return true;
//            } else {
//                for (int i = 0; i < PublicKeyList.size(); i++) {
//                    RSA p = PublicKeyList.get(i);
//                    if (p.getID().equals(id)) {
//                        PublicKeyList.remove(p);
//                        return true;
//                    }
//                }
//            }
//            //sendNotification(resourceTemplate);
//            return true;
//        } finally {
//            lock.writeLock().unlock();
//        }
//    }
}
