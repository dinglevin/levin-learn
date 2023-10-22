package levin.learn.netty.mynetty.codec;

import java.util.List;

/**
 * 描述:
 *
 * @author dinglevin
 * @since 2023/10/12 23:25
 */
public interface ByteToDataDecoder {
    void decode(byte[] bytes, List<Object> dataList);
}
