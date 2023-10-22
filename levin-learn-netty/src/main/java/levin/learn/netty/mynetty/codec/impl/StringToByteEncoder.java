package levin.learn.netty.mynetty.codec.impl;

import levin.learn.netty.mynetty.codec.DataToByteEncoder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 描述:
 *
 * @author dinglevin
 * @since 2023/10/20 23:15
 */
public class StringToByteEncoder implements DataToByteEncoder {
    @Override
    public void encode(Object object, ByteBuffer out) {
        out.put(object.toString().getBytes(StandardCharsets.UTF_8));
    }
}
