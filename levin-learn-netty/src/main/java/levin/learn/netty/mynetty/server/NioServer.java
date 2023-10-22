package levin.learn.netty.mynetty.server;

import com.google.common.collect.Lists;
import levin.learn.netty.mynetty.codec.ByteToDataDecoder;
import levin.learn.netty.mynetty.codec.DataToByteEncoder;
import levin.learn.netty.mynetty.codec.impl.ByteToStringDecoder;
import levin.learn.netty.mynetty.codec.impl.StringToByteEncoder;
import levin.learn.netty.mynetty.handler.DataHandler;
import levin.learn.netty.mynetty.handler.impl.EchoDataHandler;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 描述:
 *
 * @author dinglevin
 * @since 2023/10/11 23:12
 */
public class NioServer {
    private static final int SERVER_PORT = 9000;
    private static final int BUF_SIZE = 2048;
    private static final ByteBuffer READ_BUF = ByteBuffer.allocate(BUF_SIZE);
    private static final ByteBuffer WRITE_BUF = ByteBuffer.allocate(BUF_SIZE);

    public static void main(String[] args) throws Exception {
        NioServer server = new NioServer(SERVER_PORT, new ByteToStringDecoder(),
                new StringToByteEncoder(), new EchoDataHandler());

        server.start();
        System.out.println("Server started...");
        server.listen();
    }

    private final int port;
    private final ByteToDataDecoder decoder;
    private final DataToByteEncoder encoder;
    private final DataHandler dataHandler;

    private volatile boolean started = false;
    private Selector selector;
    private ServerSocketChannel channel;

    public NioServer(int port,
                     ByteToDataDecoder decoder,
                     DataToByteEncoder encoder,
                     DataHandler dataHandler) {
        this.port = port;
        this.decoder = decoder;
        this.encoder = encoder;
        this.dataHandler = dataHandler;
    }

    public void start() throws Exception {
        if (started) {
            throw new IllegalStateException("NioServer already started");
        }

        Selector selector = Selector.open();
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.bind(new InetSocketAddress("localhost", port));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);

        this.selector = selector;
        this.channel = channel;

        this.started = true;
    }

    public void listen() throws Exception {
        while (true) {
            int selected = this.selector.select();
            if (selected == 0) {
                continue;
            }

            Set<SelectionKey> selectionKeySet = this.selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeySet.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                processSelectionKey(selectionKey);
                iterator.remove();
            }
        }
    }

    private void processSelectionKey(SelectionKey selectionKey) throws Exception {
        if (selectionKey.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(this.selector, SelectionKey.OP_READ);
            selectionKey.interestOps(SelectionKey.OP_ACCEPT);
        } else if (selectionKey.isReadable()) {
            SocketChannel client = (SocketChannel) selectionKey.channel();

            List<Object> inDataList = Lists.newArrayList();
            READ_BUF.clear();
            while (true) {
                int readLen = client.read(READ_BUF);
                if (readLen == 0) {
                    break;
                }
                if (readLen < 0) {
                    client.close();
                    return;
                }

                byte[] content = new byte[readLen];
                READ_BUF.get(0, content);
                decoder.decode(content, inDataList);
                READ_BUF.flip();
            }

            for (Object inData : inDataList) {
                Object outData = dataHandler.handle(inData);
                WRITE_BUF.clear();
                encoder.encode(outData, WRITE_BUF);
                WRITE_BUF.flip();
                client.write(WRITE_BUF);
            }
        } else if (selectionKey.isWritable()) {
            System.out.println("writable");
        }
    }
}
