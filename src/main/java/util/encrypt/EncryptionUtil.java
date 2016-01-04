package util.encrypt;

/**
 * Created by pqpies on 1/4/16.
 */
public interface EncryptionUtil {
    public byte[] encrypt(byte[] msg);

    public byte[] decrypt(byte[] received);
}
