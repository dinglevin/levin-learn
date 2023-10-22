package levin.learn.netty.mynetty.handler.impl;

import levin.learn.netty.mynetty.handler.DataHandler;

/**
 * 描述:
 *
 * @author dinglevin
 * @since 2023/10/20 23:08
 */
public class EchoDataHandler implements DataHandler {
    @Override
    public Object handle(Object data) {
        System.out.println("Got data from client: " + data);
        return data;
    }
}
