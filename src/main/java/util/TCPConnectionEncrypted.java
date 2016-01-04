package util;

import util.encrypt.EncryptionUtil;

import java.io.IOException;

/**
 * Created by pqpies on 1/4/16.
 */
public class TCPConnectionEncrypted extends TCPConnectionDecoratorBasic {
    private EncryptionUtil encryptionUtil;

    @Override
    public String prepare(String msg) throws Exception{
        if(decorator!=null)
            decorator.prepare(msg);
        return new String(encryptionUtil.encrypt(msg.getBytes()));
    }

    @Override
    public byte[] prepare(byte[] msg) throws Exception{
        if(decorator!=null)
            msg=decorator.prepare(msg);
        return encryptionUtil.encrypt(msg);
    }

    @Override
    public String receive(String received) throws IOException{
        return new String(receive(received.getBytes()));
    }

    @Override
    public byte[] receive(byte[] received) throws IOException{
        received=encryptionUtil.decrypt(received);
        if(decorator!=null)
            received=decorator.receive(received);
        return received;
    }



}
