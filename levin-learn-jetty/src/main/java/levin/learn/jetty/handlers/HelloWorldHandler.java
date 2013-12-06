package levin.learn.jetty.handlers;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class HelloWorldHandler extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest,
	    HttpServletRequest request, HttpServletResponse response)
	    throws IOException, ServletException {
	response.setContentType("text/html;charset=utf-8");
	response.setStatus(HttpServletResponse.SC_OK);
	
	response.getWriter().println("<h1>Hello World</h1>");
	response.getWriter().println("Current Date: " + new Date());
	
	baseRequest.setHandled(true);
    }

}
