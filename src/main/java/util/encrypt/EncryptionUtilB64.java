package util.encrypt;

import java.util.Base64;

/**
 * Created by pqpies on 1/5/16.
 */
public class EncryptionUtilB64 implements EncryptionUtil{
    @Override
    public byte[] encrypt(byte[] msg) {
        return Base64.getEncoder().encode(msg);
    }

    @Override
    public byte[] decrypt(byte[] received) {
        return Base64.getDecoder().decode(received);
    }
}
