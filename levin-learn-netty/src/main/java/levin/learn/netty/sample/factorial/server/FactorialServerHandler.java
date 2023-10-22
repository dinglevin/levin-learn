package levin.learn.netty.sample.factorial.server;

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;


public class FactorialServerHandler extends SimpleChannelInboundHandler<BigInteger> {
    private static final Logger logger = LoggerFactory.getLogger(FactorialServerHandler.class);
    
    private BigInteger lastMultiplier = new BigInteger("1");
    private BigInteger factorial = new BigInteger("1");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                BigInteger msg) throws Exception {
        lastMultiplier = msg;
        factorial = factorial.multiply(msg);
        
        logger.info("Factorial of {} is: {}", lastMultiplier, factorial);
        
        ctx.writeAndFlush(factorial);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.err.printf("Factorial of %,d is: %,d%n", lastMultiplier, factorial);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
