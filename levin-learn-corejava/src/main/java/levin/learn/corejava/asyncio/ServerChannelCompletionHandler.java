package levin.learn.corejava.asyncio;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.CompletionHandler;

public class ServerChannelCompletionHandler implements CompletionHandler<AsynchronousServerSocketChannel, Proactor> {

    @Override
    public void completed(AsynchronousServerSocketChannel result, Proactor proactor) {
        
    }

    @Override
    public void failed(Throwable exc, Proactor proactor) {
        exc.printStackTrace();
    }

}
