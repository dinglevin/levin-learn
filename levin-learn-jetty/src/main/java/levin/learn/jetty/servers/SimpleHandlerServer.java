package levin.learn.jetty.servers;

import levin.learn.jetty.handlers.HelloWorldHandler;

import org.eclipse.jetty.server.Server;

public class SimpleHandlerServer extends EmbededJettyServer {

    public static void main(String[] args) {
	new SimpleHandlerServer().start(8001);
    }

    @Override
    protected void doStart(Server server, int port) {
	server.setHandler(new HelloWorldHandler());
    }
    
}
