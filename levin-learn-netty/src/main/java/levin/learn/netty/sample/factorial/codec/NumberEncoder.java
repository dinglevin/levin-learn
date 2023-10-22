package levin.learn.netty.sample.factorial.codec;

import java.math.BigInteger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


public class NumberEncoder extends MessageToByteEncoder<Number> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Number msg, ByteBuf out) throws Exception {
        BigInteger value;
        if (msg instanceof BigInteger) {
            value = (BigInteger) msg;
        } else {
            value = new BigInteger(String.valueOf(msg));
        }
        
        byte[] data = value.toByteArray();
        int dataLength = data.length;
        
        out.writeByte((byte) 'F');
        out.writeInt(dataLength);
        out.writeBytes(data);
    }

}
