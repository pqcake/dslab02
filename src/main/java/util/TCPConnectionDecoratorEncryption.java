package util;

import util.encrypt.EncryptionUtil;

import java.io.IOException;

/**
 * Created by pqpies on 1/4/16.
 */
public class TCPConnectionDecoratorEncryption extends TCPConnectionDecoratorBasic {
    private EncryptionUtil encryptionUtil;

    public TCPConnectionDecoratorEncryption(EncryptionUtil encryptionUtil){
        this.encryptionUtil=encryptionUtil;
    }

    @Override
    public String prepare(String msg) throws Exception{
        // first use self then next
        msg=new String(encryptionUtil.encrypt(msg.getBytes()));
        if(decorator!=null)
            decorator.prepare(msg);
        return msg;
    }

    @Override
    public byte[] prepare(byte[] msg) throws Exception{
        // first use self then next
        msg=encryptionUtil.encrypt(msg);
        if(decorator!=null)
            msg=decorator.prepare(msg);
        return msg;
    }

    @Override
    public String receive(String received) throws IOException{
        return new String(receive(received.getBytes()));
    }

    @Override
    public byte[] receive(byte[] received) throws IOException{
        // first remove previous then this
        if(decorator!=null)
            received=decorator.receive(received);
        received=encryptionUtil.decrypt(received);
        return received;
    }



}
