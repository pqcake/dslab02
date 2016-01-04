package util;

import java.io.IOException;

/**
 * Created by pqpies on 1/4/16.
 */
public interface TCPConnectionDecorator {

    void setDecorator(TCPConnectionDecorator decorator);

    TCPConnectionDecorator getDecorator();

    byte[] prepare(byte[] msg) throws Exception;

    String prepare(String msg) throws Exception;

    String receive(String received) throws IOException;

    byte[] receive(byte[] received) throws IOException;

}
