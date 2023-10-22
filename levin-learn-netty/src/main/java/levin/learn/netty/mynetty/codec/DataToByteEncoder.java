package levin.learn.netty.mynetty.codec;

import java.nio.ByteBuffer;

/**
 * 描述:
 *
 * @author dinglevin
 * @since 2023/10/12 23:25
 */
public interface DataToByteEncoder {
    void encode(Object object, ByteBuffer out);
}
