package util.encrypt;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by pqpies on 1/5/16.
 */
public class EncryptionUtilAES implements EncryptionUtil {
    Cipher aesIN,aesOUT;
    public EncryptionUtilAES(byte[] key,byte[] iv){
        // KEYSIZE is in bits
        SecretKeySpec keySpec = new SecretKeySpec(key,"AES");
        IvParameterSpec ivSpec=new IvParameterSpec(iv);
        try {
            aesIN=Cipher.getInstance("AES/CTR/NoPadding");
            aesOUT=Cipher.getInstance("AES/CTR/NoPadding");
            aesIN.init(Cipher.DECRYPT_MODE,keySpec,ivSpec);
            aesOUT.init(Cipher.ENCRYPT_MODE,keySpec,ivSpec);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }


    }

    @Override
    public byte[] encrypt(byte[] msg) {
        try {
            return aesOUT.doFinal(msg);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } finally {
            return null;
        }
    }

    @Override
    public byte[] decrypt(byte[] received)  {
        try {
            return aesIN.doFinal(received);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } finally {
            return null;
        }
    }
}
