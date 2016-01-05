package util.encrypt;

import org.bouncycastle.util.encoders.Base64;

/**
 * Created by pqpies on 1/5/16.
 */
public class EncryptionUtilB64 implements EncryptionUtil{
    @Override
    public byte[] encrypt(byte[] msg) {
        return Base64.encode(msg);
    }

    @Override
    public byte[] decrypt(byte[] received) {
        return Base64.decode(received);
    }
}
