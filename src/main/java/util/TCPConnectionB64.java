package util;

import java.io.IOException;
import java.util.Base64;

/**
 * Created by pqpies on 1/4/16.
 */
public class TCPConnectionB64 extends TCPConnectionDecoratorBasic {

    @Override
    public String prepare(String msg) throws Exception{
        if(decorator!=null)
            decorator.prepare(msg);
        return Base64.getEncoder().encodeToString(msg.getBytes());
    }

    @Override
    public byte[] prepare(byte[] msg) throws Exception{
        if(decorator!=null)
            msg=decorator.prepare(msg);
        return Base64.getEncoder().encode(msg);
    }

    @Override
    public String receive(String received) throws IOException{
        return new String(receive(received.getBytes()));
    }

    @Override
    public byte[] receive(byte[] received) throws IOException{
        received=Base64.getDecoder().decode(received);
        if(decorator!=null)
            received=decorator.receive(received);
        return received;
    }



}
