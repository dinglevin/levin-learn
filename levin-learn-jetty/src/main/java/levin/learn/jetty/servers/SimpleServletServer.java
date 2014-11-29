package levin.learn.jetty.servers;

import levin.learn.jetty.servlets.HelloWorldServlet;

import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;

public class SimpleServletServer extends EmbededJettyServer {

	public static void main(String[] args) {
		new SimpleServletServer().start(8002);
	}

	@Override
	protected void doStart(Server server, int port) {
		Connector connector = new SelectChannelConnector();
		connector.setServer(server);
		
		connector.setPort(port);
		connector.setHost("localhost");
		server.addConnector(connector);

		connector = new SocketConnector();
		connector.setServer(server);
		connector.setPort(port + 1);
		connector.setHost("localhost");
		((AbstractConnector) connector).setAcceptors(3);
		server.addBean(connector);

		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath("/hello");
		handler.addServlet(HelloWorldServlet.class, "/");

		HandlerCollection handlers = new HandlerCollection();
		handlers.addHandler(handler);
		server.setHandler(handlers);
	}

	@Override
	protected Server createServer(int port) {
		Server server = new Server();
		server.setThreadPool(new ExecutorThreadPool());
		return server;
	}

}
