package test;
import EZShare.Nodes;
import EZShare.RSA;
import EZShare.server.KeyList;

import java.util.*;

public class KeyListTest {
    private List<RSA> keyList = new ArrayList<>();
    public synchronized List<RSA> getKeyList(){return keyList;}
    public synchronized void updateKeyList(List<RSA> inputKeyList) {
        for(RSA inputRSA : inputKeyList){
            if(!inputKeyList.contains(inputRSA.getID()) && !(inputRSA.getID().equals("LARRY"))){
                keyList.add(inputRSA);
            }
        }
    }
    public static void main(String[] args){
        KeyList keyList=null;
        RSA testrsa= new RSA("LARRY");
        List<RSA> list = new ArrayList<>();
        list.add(testrsa);
        list.add(new RSA("May"));
        System.out.println(testrsa.getID());
        System.out.println(testrsa.getPublicKey());
        System.out.println(testrsa.getPrivateKey());


        System.out.println("list"+list.get(0).getID());
        keyList.updateKeyList(list);
        System.out.println(keyList);
    }
}
