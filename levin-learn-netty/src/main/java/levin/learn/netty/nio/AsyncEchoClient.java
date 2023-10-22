package levin.learn.netty.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * 描述:
 *
 * @author dinglevin
 * @since 2023/10/22 14:15
 */
public class AsyncEchoClient {
    public static void main(String[] args) throws Exception {
        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        boolean connected = socketChannel.connect(new InetSocketAddress("localhost", 9000));
        if (connected) {
            socketChannel.register(selector, SelectionKey.OP_READ);
            new Thread(new Writer(socketChannel)).start();
        } else {
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        }

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
                processSelection(selector, byteBuffer, selectionKey);
                iterator.remove();
            }
        }
    }

    private static void processSelection(Selector selector,
                                         ByteBuffer byteBuffer,
                                         SelectionKey selectionKey) throws Exception {
        if (!selectionKey.isValid()) {
            return;
        }

        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        if (selectionKey.isConnectable()) {
            if (socketChannel.finishConnect()) {
                System.out.println("Client connected to [" + socketChannel.getRemoteAddress() + "]");
                socketChannel.register(selector, SelectionKey.OP_READ);
                new Thread(new Writer(socketChannel)).start();
            } else {
                System.out.println("Client connect failed");
                System.exit(2);
            }
        } else if (selectionKey.isReadable()) {
            byteBuffer.clear();
            int read = socketChannel.read(byteBuffer);
            if (read < 0) {
                selectionKey.channel();
                System.out.println("Exit");
                System.exit(0);
            }

            byte[] content = new byte[read];
            byteBuffer.get(0, content);
            String response = new String(content, StandardCharsets.UTF_8);
            System.out.println("Response: " + response + " - [" + socketChannel.getRemoteAddress() + "]");
            selectionKey.interestOps(SelectionKey.OP_READ);
        }
    }

    private static class Writer implements Runnable {
        private SocketChannel socketChannel;

        public Writer(SocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }

        @Override
        public void run() {
            try {
                Scanner scanner = new Scanner(System.in);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if ("".equals(line)) {
                        continue;
                    }

                    socketChannel.write(StandardCharsets.UTF_8.encode(line));
                }
                scanner.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
