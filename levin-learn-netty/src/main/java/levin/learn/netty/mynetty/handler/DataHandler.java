package levin.learn.netty.mynetty.handler;

/**
 * 描述:
 *
 * @author dinglevin
 * @since 2023/10/12 23:53
 */
public interface DataHandler {
    Object handle(Object data);
}
