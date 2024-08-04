package levin.learn.netty.nio.ticker;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * 描述:
 *
 * @author dinglevin
 * @since 2023/10/24 16:56
 */
public class TickerServer {
    public static void main(String[] args) throws Exception {
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress("localhost", 9000));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("TickerServer started.");

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
                iterator.remove();

                if (selectionKey.isWritable()) {
                    processWrite(selector, selectionKey);
                }
                if (selectionKey.isReadable()) {
                    processRead(byteBuffer, selector, selectionKey);
                }
                if (selectionKey.isAcceptable()) {
                    registerClient(selector, selectionKey);
                }
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

    private static void processWrite(Selector selector, SelectionKey selectionKey) throws Exception {
        int write = ((SocketChannel) selectionKey.channel()).write((ByteBuffer) selectionKey.attachment());
        if (write == 0) {
            System.out.println("Main thread write failed");
            selectionKey.channel().register(selector, SelectionKey.OP_WRITE, selectionKey.attachment());
        }
    }

    private static void processRead(ByteBuffer byteBuffer, Selector selector, SelectionKey selectionKey) throws Exception {
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

        for (int i = 0; i < 50; i++) {
            new Thread(new TickRunner("ticker-" + i, selector, socketChannel), "ticker-" + i).start();
        }
    }

    private static class TickRunner implements Runnable {
        private static final Random RANDOM = new Random();
        private String name;
        private Selector selector;
        private SocketChannel socketChannel;

        public TickRunner(String name,
                          Selector selector,
                          SocketChannel socketChannel) {
            this.name = name;
            this.selector = selector;
            this.socketChannel = socketChannel;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < 50; i++) {
                    Thread.sleep(RANDOM.nextLong(0L, 10L));
                    String msg = name + " - " + i;
                    ByteBuffer byteBuffer = ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8));
                    int write = socketChannel.write(byteBuffer);
                    if (write == 0) {
                        System.out.println("Write failed: " + msg);
                        socketChannel.register(selector, SelectionKey.OP_WRITE, byteBuffer);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
