package levin.learn.corejava.asyncio;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AsyncIOServer {
    public static void main(String[] args) throws Exception {
        AsynchronousServerSocketChannel svrChannel = AsynchronousServerSocketChannel.open();
        svrChannel.accept(10, new CompletionHandler<AsynchronousSocketChannel, Integer>() {
            @Override
            public void completed(AsynchronousSocketChannel result, Integer attachment) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void failed(Throwable exc, Integer attachment) {
                // TODO Auto-generated method stub
                
            }
        });
    }
}
