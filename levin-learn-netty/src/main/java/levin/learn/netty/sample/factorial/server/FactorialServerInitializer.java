package levin.learn.netty.sample.factorial.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import levin.learn.netty.sample.factorial.codec.BigIntegerDecoder;
import levin.learn.netty.sample.factorial.codec.NumberEncoder;


public class FactorialServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        
        pipeline.addLast(ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
        pipeline.addLast(ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        
        pipeline.addLast(new BigIntegerDecoder());
        pipeline.addLast(new NumberEncoder());
        
        pipeline.addLast(new FactorialServerHandler());
    }

}
