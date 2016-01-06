package util.encrypt;

import util.Keys;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
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
    public EncryptionUtilAuthRSA(Key outKey,Key inKey){
        // make sure to use the right ALGORITHM for what you want to do (see text)

        try {
            in = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
            out = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        initKeys(outKey,inKey);

    }

    public void initKeys(Key outKey,Key inKey){
        // MODE is the encryption/decryption mode
        // KEY is either a private, public or secret key
        try {
            if(in!=null){
                if(inKey!=null)
                    in.init(Cipher.DECRYPT_MODE,inKey);
            }
            if(out!=null){
                if(outKey!=null)
                    out.init(Cipher.ENCRYPT_MODE,outKey);
            }
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

    }

    @Override
    public byte[] encrypt(byte[] msg) {
        try {
            return out.doFinal(msg);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            //e.printStackTrace();
        }
        return msg;
    }

    @Override
    public byte[] decrypt(byte[] received) {
        try {
            return in.doFinal(received);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            //e.printStackTrace();
        }
        return received;
    }
}
