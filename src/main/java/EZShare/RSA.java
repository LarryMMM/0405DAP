package EZShare;
import EZShare.message.Host;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;


public class RSA {
//    public static void main(String[] args) {
//        //RSA KEY GENERATION
//        ArrayList<Key> keyPair= getKeyPair("RSA");
//        System.err.println("Public key generated   :"+ keyPair.get(0));//public key
//        System.err.println("Private key generated   :"+ keyPair.get(1));//private key
//
//        //RSA KEY SAVE
//        saveKeyPair(keyPair,"LarryKeyPairTest");
//
//        //LOAD RSA KEY
//        ArrayList<Key> keyPairLoad= loadKeyPair("LarryKeyPairTest","RSA");
//        System.err.println("Public Key Loaded: " + keyPairLoad.get(0));
//        System.err.println("Private Key Loaded: " + keyPairLoad.get(1));
//
//        //ENCRYPT MESSAGE
//        byte[] cipherMsg = new byte[0];
//        try {
//            cipherMsg = encryptMessage((PublicKey) keyPairLoad.get(0),"RSA/ECB/PKCS1Padding","larry is genius");
//        } catch (NoSuchPaddingException e) {
//            e.printStackTrace();
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (InvalidKeyException e) {
//            e.printStackTrace();
//        } catch (BadPaddingException e) {
//            e.printStackTrace();
//        } catch (IllegalBlockSizeException e) {
//            e.printStackTrace();
//        }
//        //DECRYPT MESSAGE
//        String Msg = null;
//        try {
//            Msg = decryptMessage((PrivateKey)keyPairLoad.get(1),"RSA/ECB/PKCS1Padding",cipherMsg);
//        } catch (NoSuchPaddingException e) {
//            e.printStackTrace();
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (InvalidKeyException e) {
//            e.printStackTrace();
//        } catch (BadPaddingException e) {
//            e.printStackTrace();
//        } catch (IllegalBlockSizeException e) {
//            e.printStackTrace();
//        }
//        System.err.println("cipherMessage :" + Base64.getEncoder().encodeToString(cipherMsg));
//        System.err.println("plainMessage  :"+ Msg);
//        //RSA KEY SAVE AS TEXT
//        saveKeyPairText(keyPair,"LarryKeyPairTextTest");
//
//        //GENERATE RSA SIGNATURE
//        getSignatureFile((PrivateKey) keyPairLoad.get(1),"SHA256withRSA", "LarrySignatureTest.txt");
//
//        //VERIFY RSA SIGNATURE
//        boolean verify = verifySignatureFile((PublicKey)keyPairLoad.get(0),"SHA256withRSA",
//                "LarrySignatureTest.txt","signed"+ "LarrySignatureTest.txt");
//        System.err.println(verify);
//
//        //GENERATE RSA SIGNATURE FOR MESSAGE
//        String inputMessage = "Larry is genius!";
//        String outputMessage = getSignatureMessage((PrivateKey) keyPairLoad.get(1),"SHA256withRSA",inputMessage);
//        System.out.println(outputMessage);
//    }


    private PublicKey pubKey;
    private PrivateKey pvtKey;
    private String id;

    public String getID(){
        return this.id;
    }
    public PublicKey getPublicKey(){ return this.pubKey; }
    public PrivateKey getPrivateKey(){return this.pvtKey;}
    public RSA(){
        this.pubKey = (PublicKey) getKeyPair("RSA").get(0);
        this.pvtKey = (PrivateKey)getKeyPair("RSA").get(1);
        saveKeyPair(getKeyPair("RSA"),id.toString().replace(":",","));
    }

    public RSA(String clientId){
        String keyName = clientId.replace(":", ",");
        Path keyFilePath = Paths.get(keyName+"pub.txt");
        if(Files.exists(keyFilePath)){
            this.pubKey = loadPublicKey(keyName,"RSA");
            Path keyFilePath2 = Paths.get(keyName+"pvt.txt");
            if (Files.exists(keyFilePath2)){this.pvtKey = loadPrivateKey(keyName,"RSA");}
            System.out.println(this.pubKey);
            this.id = clientId;
        }else {
            //RSA KEY GENERATION
            this.pubKey = (PublicKey) getKeyPair("RSA").get(0);
            this.pvtKey = (PrivateKey)getKeyPair("RSA").get(1);
            saveKeyPair(getKeyPair("RSA"),keyName);
        }
        this.id = clientId;
    }

//    public RSA(Host clientId){
//        String keyName = clientId.toString().replace(":", ",");
//        Path keyFilePath = Paths.get(keyName+"pub.txt");
//        if(Files.exists(keyFilePath)){
//            this.pubKey = loadPublicKey(keyName,"RSA");
//            System.out.println(this.pubKey);
//            Path keyFilePath2 = Paths.get(keyName+"pvt.txt");
//            if (Files.exists(keyFilePath2)){this.pvtKey = loadPrivateKey(keyName,"RSA");
//        }else {
//            //RSA KEY GENERATION
//            this.pubKey = (PublicKey) getKeyPair("RSA").get(0);
//            this.pvtKey = (PrivateKey)getKeyPair("RSA").get(1);
//            saveKeyPairText(getKeyPair("RSA"),keyName);
//        }
//        this.id = clientId;
//        }
//    }

    private static ArrayList<Key> getKeyPair(String algorithm){
        try {
            ArrayList<Key> keyPair = new ArrayList<>();
            //RSA KEY GENERATION
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
            kpg.initialize(2048);//initialize with key size
//            kpg.initialize(512);//initialize with key size
            KeyPair kp = kpg.generateKeyPair();
            Key pub = kp.getPublic();
            Key pvt = kp.getPrivate();
//            System.out.println(pub);
//            System.out.println(pvt);
            keyPair.add(pub);
            keyPair.add(pvt);
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
//create RSA KEY
    private static void saveKeyPair(ArrayList<Key> keyPair,String keyName){
        try{
            Key pub = keyPair.get(0);
            Key pvt = keyPair.get(1);
            OutputStream saveKey = new FileOutputStream(keyName + "pvt.txt");
            saveKey.write(pvt.getEncoded());
            saveKey.close();

            saveKey = new FileOutputStream(keyName + "pub.txt");
            saveKey.write(pub.getEncoded());
            saveKey.close();
            //RSA FORMAT CHECK
            System.err.println("private key format: "+ pvt.getFormat());
            System.err.println("public key format: "+ pub.getFormat());
            System.err.println("save key pair succeeded!");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//save RSA KEY
    private static PrivateKey loadPrivateKey(String keyName,String algorithm){
        try {
            Path keyFilePath = Paths.get(keyName + "pvt.txt");
            byte[] loadKeyByte = Files.readAllBytes(keyFilePath);
            /* Generate private key. */
            PKCS8EncodedKeySpec ksPvt = new PKCS8EncodedKeySpec(loadKeyByte);
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            PrivateKey pvtKey = kf.generatePrivate(ksPvt);
            return pvtKey;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }return null;
    }
    private static PublicKey loadPublicKey(String keyName,String algorithm){
        try {
            /* Read all the public key bytes */
            Path keyFilePath = Paths.get(keyName + "pub.txt");
            byte[] loadKeyByte = Files.readAllBytes(keyFilePath);
            /* Generate public key. */
            X509EncodedKeySpec ksPub = new X509EncodedKeySpec(loadKeyByte);
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            PublicKey pubKey = kf.generatePublic(ksPub);
            return pubKey;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }return null;
    }
    private static ArrayList<Key> loadKeyPair(String keyName,String algorithm){
        try{
            ArrayList<Key> keyPair = new ArrayList<>();
            Path keyFilePath = Paths.get(keyName + ".pvt");
            byte[] loadKeyByte = Files.readAllBytes(keyFilePath);
            /* Generate private key. */
            PKCS8EncodedKeySpec ksPvt= new PKCS8EncodedKeySpec(loadKeyByte);
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            PrivateKey pvtKey = kf.generatePrivate(ksPvt);
            /* Read all the public key bytes */
            keyFilePath = Paths.get(keyName + ".pub");
            loadKeyByte = Files.readAllBytes(keyFilePath);
            /* Generate public key. */
            X509EncodedKeySpec ksPub = new X509EncodedKeySpec(loadKeyByte);
//            kf = KeyFactory.getInstance("RSA");
            PublicKey pubKey = kf.generatePublic(ksPub);
            keyPair.add(pubKey);
            keyPair.add(pvtKey);
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }
//load RSA KEY
    public static byte[] encryptMessage(PublicKey pubKey,String algorithm, String message) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher encryptCipher = Cipher.getInstance(algorithm);
        encryptCipher.init(Cipher.ENCRYPT_MODE,pubKey);
        return encryptCipher.doFinal(message.getBytes());
//            System.err.println(cipherText);
    }
//encrypt message
    public static String decryptMessage(PrivateKey pvtKey,String algorithm,byte[] cipherMessage) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher decryptCipher = Cipher.getInstance(algorithm);
        decryptCipher.init(Cipher.PRIVATE_KEY, pvtKey);
        return new String(decryptCipher.doFinal(cipherMessage));
    }
//decrypt message
    private static void saveKeyPairText(ArrayList<Key> keyPair,String keyNameText){
        try {
            PublicKey pubKey = (PublicKey) keyPair.get(0);
            PrivateKey pvtKey = (PrivateKey) keyPair.get(1);
            Base64.Encoder encoder = Base64.getEncoder();//java provided
            Writer saveKeyText = new FileWriter(keyNameText + "pvt.txt");
            saveKeyText.write("-----BEGIN RSA PRIVATE KEY-----\\n");
            saveKeyText.write(encoder.encodeToString(pvtKey.getEncoded()));
            saveKeyText.write("-----END RSA PRIVATE KEY-----\\n");
            saveKeyText.close();
            System.err.println("save private key text succeeded!");
            saveKeyText = new FileWriter(keyNameText + "pub.txt");
            saveKeyText.write("-----BEGIN RSA PUBLIC KEY-----\\n");
            saveKeyText.write(encoder.encodeToString(pubKey.getEncoded()));
            saveKeyText.write("-----END RSA PUBLIC KEY-----\\n");
            saveKeyText.close();
            System.err.println("save public key text succeeded!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//save RSA KEY as txt
    public static String getSignatureMessage(PrivateKey pvtKey,String algorithm,String message){
        try {
            Signature sign = Signature.getInstance(algorithm);
            sign.initSign(pvtKey);
            if(!message.isEmpty()){
                sign.update(message.getBytes());
            }
            System.err.println("generate RSA signature succeed!");
            return (message+ sign.sign());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        System.err.println("generate RSA signature failed!");
        return null;
    }
    public static boolean verifySignatureMessage(PublicKey pubKey,String algorithm,String unsignedMessage,String signedMessage){
        try {
            Signature sign = Signature.getInstance(algorithm);
            sign.initVerify(pubKey);
            if(!unsignedMessage.isEmpty()){
                sign.update(signedMessage.getBytes());
            }
            return(sign.verify(unsignedMessage.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static void getSignatureFile(PrivateKey pvtKey,String algorithm,String fileName){
        try {
            Signature sign = Signature.getInstance(algorithm);
            sign.initSign(pvtKey);
            InputStream input4Signature = new FileInputStream(fileName);
            byte[] buf = new byte[2048];
            int len = input4Signature.read(buf);
            if(len!=-1){
                sign.update(buf);
            }
            input4Signature.close();
            OutputStream output4Signature = new FileOutputStream("signed" + fileName);
            output4Signature.write(sign.sign());
            output4Signature.close();
            System.err.println("generate RSA signature succeed!");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
    }
//create signature
    public static boolean verifySignatureFile(PublicKey pubKey,String algorithm,String unsignedFileName,String signedFileName){
        try {
            Signature sign = Signature.getInstance(algorithm);
            sign.initVerify(pubKey);
            InputStream input4Signature = new FileInputStream(unsignedFileName);
            byte[] buf = new byte[2048];
            int len = input4Signature.read(buf);
            if(len!=-1){
                sign.update(buf);
            }
            Path signedFile = Paths.get(signedFileName);
            byte[] verifyBuf =  Files.readAllBytes(signedFile);
            return (sign.verify(verifyBuf));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return false;
    }
//verify signature
//    public static String encryptCommand(String keyName,String algorithm,String command){
//        return encryptMessage();
//    }//encrypt command message and sign signature
//    public static String decryptResponse(){
//
//    }
//    public static String encryptJson(String keyName, String algorithm, String Json){
//        try {
//            ArrayList<Key> KeyPair = RSA.loadKeyPair(keyName, algorithm);
//            PublicKey pubKey = (PublicKey)KeyPair.get(0);
//            PrivateKey pvtKey = (PrivateKey)KeyPair.get(1);
//            String encryptJson = encryptMessage(pubKey,algorithm, Json).toString();
//            String signature = getSignatureMessage(pvtKey,"SHA256withRSA",encryptJson.getBytes()).toString();
//
//            return encryptJson+",,,,"+signature;
//
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (BadPaddingException e) {
//            e.printStackTrace();
//        } catch (IllegalBlockSizeException e) {
//            e.printStackTrace();
//        } catch (NoSuchPaddingException e) {
//            e.printStackTrace();
//        } catch (InvalidKeyException e) {
//            e.printStackTrace();
//        }
//        return null;
//}
//
//    public static String decryptJson(String keyName, String algorithm, byte[] cipherMessage){
//        try{
//            ArrayList<Key> KeyPair = RSA.loadKeyPair(keyName, algorithm);
//            PrivateKey pvtKey = (PrivateKey) KeyPair.get(1);
//            PublicKey pubKey = (PublicKey)KeyPair.get(0);
//            String decryptJson = decryptMessage(pvtKey,algorithm,cipherMessage);
//            String[] parts= decryptJson.split(",,,,");
//            String Json = parts[0];
//            String signature = parts[1];
//            boolean verifysign = verifySignatureMessage(pubKey,"SHA256withRSA",Json,signature.getBytes());
//            if(verifysign){
//                return Json;
//            }
//            else
//                return null;
//
//        } catch (NoSuchPaddingException e) {
//            e.printStackTrace();
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (InvalidKeyException e) {
//            e.printStackTrace();
//        } catch (IllegalBlockSizeException e) {
//            e.printStackTrace();
//        } catch (BadPaddingException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
}