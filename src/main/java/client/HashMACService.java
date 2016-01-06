package client;

import org.bouncycastle.util.encoders.Base64;

import java.io.File;
import java.security.Key;
import javax.crypto.Mac;
import java.security.MessageDigest;
import util.Keys;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

public class HashMACService {

    /**
     * Creates a Hash Message Authentication Codes (MACs) from a key and a message.
     * @param key secrect shared keys between the clients (provided with the template)
     * @param msg message to use for mac generation
     * @return base64 encoded hash mac
     */
    public static String createHMAC(Key key, String msg) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(key);
            hmac.update(msg.getBytes());
            byte[] hash = hmac.doFinal();
            return new String(Base64.encode(hash));
        } catch(NoSuchAlgorithmException e) {
        } catch(InvalidKeyException e) {}
        return null;
    }

    /**
     *
     * @param key secrect shared keys between the clients (provided with the template)
     * @param receivedHMAC
     * @param receivedMessage
     * @return true if verification was successful
     */
    public static boolean verifyHMAC(Key key, String receivedHMAC, String receivedMessage) {
        byte[] computedHMAC = decodeBase64(createHMAC(key, receivedMessage));
        byte[] encodedReceivedHMAC = decodeBase64(receivedHMAC);
        return MessageDigest.isEqual(computedHMAC, encodedReceivedHMAC);
    }

    private static byte[] encodeBase64(String text) {
        return Base64.encode(text.getBytes());
    }

    private static byte[] decodeBase64(String text) {
        return Base64.decode(text.getBytes());
    }
}