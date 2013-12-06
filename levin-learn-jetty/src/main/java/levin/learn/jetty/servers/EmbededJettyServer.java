package levin.learn.jetty.servers;

import org.eclipse.jetty.server.Server;

public abstract class EmbededJettyServer {
    
    public void start(int port) {
	Server server = createServer(port);
	
	try {
	    doStart(server, port);
		
	    server.start();
	    server.join();
	} catch(Exception ex) {
	    ex.printStackTrace();
	    forceStop(server);
	}
    }
    
    protected abstract void doStart(Server server, int port);
    
    protected Server createServer(int port) {
	return new Server(port);
    }
    
    protected static void forceStop(Server server) {
	try {
	    server.stop();
	} catch(Exception ex) {
	    ex.printStackTrace();
	}
    }
}
