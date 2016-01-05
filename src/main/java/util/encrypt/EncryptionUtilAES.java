package util.encrypt;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by pqpies on 1/5/16.
 */
public class EncryptionUtilAES implements EncryptionUtil {
    Cipher in,out;
    public EncryptionUtilAES(){


    }

    @Override
    public byte[] encrypt(byte[] msg) {
        return new byte[0];
    }

    @Override
    public byte[] decrypt(byte[] received) {
        return new byte[0];
    }
}
