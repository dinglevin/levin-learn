package levin.learn.netty.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 描述:
 *
 * @author dinglevin
 * @since 2023/10/22 10:58
 */
public class SyncEchoClient {
    public static void main(String[] args) throws Exception {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 9000));

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if ("".equals(line)) {
                continue;
            }

            socketChannel.write(StandardCharsets.UTF_8.encode(line));

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
