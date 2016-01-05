package util.encrypt;

import util.Keys;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by pqpies on 1/5/16.
 */
public class EncryptionUtilAuthRSA implements EncryptionUtil {
    private Cipher in,out;

    /**
     * Creates a EncryptionUtil that encrypt bytes with a given private key "out" and a given public key "in".
     * Can be used for Auth using RSA.
     * @param outKey
     * @param inKey
     */
    public EncryptionUtilAuthRSA(String outKey,String inKey){
        // make sure to use the right ALGORITHM for what you want to do (see text)

        try {
            in = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
            out = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        // MODE is the encryption/decryption mode
        // KEY is either a private, public or secret key
        File inKeyFile=new File(inKey);
        File outKeyFile=new File(outKey);
        try {
            in.init(Cipher.DECRYPT_MODE, Keys.readPublicPEM(inKeyFile) );
            out.init(Cipher.ENCRYPT_MODE, Keys.readPrivatePEM(outKeyFile));
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @Override
    public byte[] encrypt(byte[] msg) {
        try {
            return out.doFinal(msg);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }finally {
            return null;
        }
    }

    @Override
    public byte[] decrypt(byte[] received) {
        try {
            return in.doFinal(received);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } finally {
            return null;
        }
    }
}
