package levin.learn.netty.nio;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * 描述:
 *
 * @author dinglevin
 * @since 2023/10/22 10:35
 */
public class EchoServer {
    public static void main(String[] args) throws Exception {
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress("localhost", 9000));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("EchoServer started.");

        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        while (true) {
            int selected = selector.select();
            if (selected == 0) {
                continue;
            }

            Set<SelectionKey> selectionKeySet = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeySet.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                if (selectionKey.isAcceptable()) {
                    registerClient(selector, selectionKey);
                } else if (selectionKey.isReadable()) {
                    readAndEcho(byteBuffer, selectionKey);
                }

                iterator.remove();
            }
        }
    }

    private static void registerClient(Selector selector, SelectionKey selectionKey) throws Exception {
        ServerSocketChannel serverChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel client = serverChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        selectionKey.interestOps(SelectionKey.OP_ACCEPT);
        System.out.println("Connected from client - [" + client.getRemoteAddress() + "]");
    }

    private static void readAndEcho(ByteBuffer byteBuffer, SelectionKey selectionKey) throws Exception {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        SocketAddress clientAddress = socketChannel.getRemoteAddress();

        byteBuffer.clear();
        int read = socketChannel.read(byteBuffer);
        if (read < 0) {
            socketChannel.close();
            System.out.println("NO data read, disconnect from " + clientAddress);
            return;
        }

        byte[] content = new byte[read];
        byteBuffer.get(0, content);
        String msg = new String(content, StandardCharsets.UTF_8).trim();
        System.out.println("Got msg from client: " + msg + " - [" + clientAddress + "]");

        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        byteBuffer.clear();

        if ("exit".equalsIgnoreCase(msg)) {
            socketChannel.close();
        }
    }
}
