package levin.learn.netty.sample.factorial;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;


public class FactorialClient {
    public static final int COUNT = 1000;
    public static final String HOST = "localhost";
    public static final int PORT = 8001;
    
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                     .channel(NioSocketChannel.class)
                     .handler(new FactorialClientInitializer());
            
            ChannelFuture future = bootstrap.connect(HOST, PORT).sync();
            FactorialClientHandler handler = (FactorialClientHandler) future.channel().pipeline().last();
            
            System.out.format("Factorial of %,d is: %,d", COUNT, handler.getFactorial());
        } finally {
            group.shutdownGracefully();
        }
    }
}
