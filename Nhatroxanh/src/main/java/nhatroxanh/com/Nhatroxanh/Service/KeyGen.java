package nhatroxanh.com.Nhatroxanh.Service;

import javax.crypto.KeyGenerator;
import java.util.Base64;

public class KeyGen {
    public static void main(String[] args) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128); // AES-128 (16 byte)
        byte[] key = keyGen.generateKey().getEncoded();
        String base64Key = Base64.getEncoder().encodeToString(key);
        System.out.println("Generated Key: " + base64Key);
        System.out.println("Key length: " + key.length + " bytes");
    }
}