package levin.learn.corejava.asyncio;

import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ChannelCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, Proactor> {

    @Override
    public void completed(AsynchronousSocketChannel result, Proactor proactor) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void failed(Throwable exc, Proactor proactor) {
        // TODO Auto-generated method stub
        
    }
}
