package levin.learn.netty.mynetty.codec.impl;

import levin.learn.netty.mynetty.codec.ByteToDataDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 描述:
 *
 * @author dinglevin
 * @since 2023/10/20 23:11
 */
public class ByteToStringDecoder implements ByteToDataDecoder {
    @Override
    public void decode(byte[] bytes, List<Object> dataList) {
        dataList.add(new String(bytes, StandardCharsets.UTF_8));
    }
}
