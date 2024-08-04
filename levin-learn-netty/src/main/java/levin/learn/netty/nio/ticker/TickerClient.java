package levin.learn.netty.nio.ticker;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * 描述:
 *
 * @author dinglevin
 * @since 2023/10/24 17:47
 */
public class TickerClient {
    public static void main(String[] args) throws Exception {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 9000));

        socketChannel.write(StandardCharsets.UTF_8.encode("start"));

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        while (true) {
            byteBuffer.clear();
            int read = socketChannel.read(byteBuffer);
            byte[] respBytes = new byte[read];
            byteBuffer.get(0, respBytes);
            String response = new String(respBytes, StandardCharsets.UTF_8).trim();
            System.out.println("Response from server: " + response + " [" + socketChannel.getRemoteAddress() + "]");
            byteBuffer.clear();
        }
    }
}
