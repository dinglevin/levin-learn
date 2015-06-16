package levin.learn.netty.sample.factorial;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;


public class FactorialServer {
    private static final int PORT = 8001;
    
    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workGroup)
                     .channel(NioServerSocketChannel.class)
                     .handler(new LoggingHandler())
                     .childHandler(new FactorialServerInitializer());
            
            Channel channel = bootstrap.bind(PORT).sync().channel();
            
            System.out.println("Factorial Server Started.");
            
            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
