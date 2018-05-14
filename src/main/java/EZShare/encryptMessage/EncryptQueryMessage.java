package EZShare.encryptMessage;

public class EncryptQueryMessage  {
    private static byte[] encryptMessage;
    private static String signatureMessage;
    public EncryptQueryMessage(byte[]encryptMessage,String signatureMessage){
        this.encryptMessage = encryptMessage;
        this.signatureMessage = signatureMessage;
    }

}
