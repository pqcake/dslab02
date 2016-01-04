package util;

import java.io.IOException;

/**
 * Created by pqpies on 1/4/16.
 */
public abstract class TCPConnectionDecoratorBasic implements TCPConnectionDecorator{
    protected TCPConnectionDecorator decorator;
    @Override
    public void setDecorator(TCPConnectionDecorator decorator) {
        this.decorator=decorator;
    }

    @Override
    public TCPConnectionDecorator getDecorator() {
        return decorator;
    }
}
