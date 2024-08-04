package levin.learn.netty.sample.echo.client;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * 描述:
 *
 * @author dinglevin
 * @since 2023/11/2 11:44
 */
public class EchoServer {
    public static void main(String[] args) throws Exception {
        EventLoopGroup mainEventLoopGroup = new NioEventLoopGroup(1);
        EventLoopGroup childEventLoopGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(mainEventLoopGroup, childEventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(9000))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new EchoServerHandler());
                        }
                    });

            ChannelFuture channelFuture = bootstrap.bind().sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            childEventLoopGroup.shutdownGracefully().sync();
            mainEventLoopGroup.shutdownGracefully().sync();
        }

    }
}
