package util;

import java.io.IOException;

/**
 * Created by pqpies on 1/4/16.
 */
public interface TCPConnection {

    void setDecorator(TCPConnectionDecorator decorator);

    TCPConnectionDecorator getDecorator();

    void send(byte[] msg) throws Exception;

    void send(String msg) throws Exception;

    String receive() throws IOException;

    byte[] receiveBytes() throws IOException;


    void close();

    boolean isClosed();
}
