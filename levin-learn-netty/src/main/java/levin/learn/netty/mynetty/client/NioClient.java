package levin.learn.netty.mynetty.client;

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
 * @since 2023/10/15 17:18
 */
public class NioClient {
    private static final int SERVER_PORT = 9000;

    public static void main(String[] args) throws Exception {
        NioClient client = new NioClient(SERVER_PORT);
        client.start();
        client.listen();
    }

    private final int serverPort;
    private Selector selector;
    private SocketChannel socketChannel;

    public NioClient(int serverPort) {
        this.serverPort = serverPort;
    }

    public void start() throws Exception {
        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        boolean connected = socketChannel.connect(new InetSocketAddress("localhost", serverPort));
        if (connected) {
            System.out.println("Client connected to [" + socketChannel.getRemoteAddress() + "]");
            socketChannel.register(selector, SelectionKey.OP_READ);
        } else {
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        }

        this.selector = selector;
        this.socketChannel = socketChannel;
    }

    public void listen() {
        new Reader().start();
        new Writer().start();
    }

    private class Reader extends Thread {
        public void run() {
            try {
                while (true) {
                    int selected = selector.select();
                    if (selected == 0) {
                        continue;
                    }

                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        process(key);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void process(SelectionKey key) throws Exception {
            if (!key.isValid()) {
                return;
            }

            if (key.isConnectable()) {
                SocketChannel channel = (SocketChannel) key.channel();
                if (channel.finishConnect()) {
                    System.out.println("Client connected to [" + socketChannel.getRemoteAddress() + "]");
                    channel.register(selector, SelectionKey.OP_READ);
                } else {
                    System.out.println("Client connect failed");
                    System.exit(2);
                }
            } else if (key.isReadable()) {
                SocketChannel channel = (SocketChannel) key.channel();
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                channel.read(buffer);
                buffer.flip();
                String content = StandardCharsets.UTF_8.decode(buffer).toString();
                System.out.println("Got response from server: " + content);
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private class Writer extends Thread {
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
