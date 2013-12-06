package levin.learn.jetty.servlets;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HelloWorldServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
	resp.setContentType("text/html;charset=utf-8");
	resp.setStatus(HttpServletResponse.SC_OK);
	
	resp.getWriter().println("<h1>Hello World</h1>");
	resp.getWriter().println("Current Date: " + new Date());
    }
}
