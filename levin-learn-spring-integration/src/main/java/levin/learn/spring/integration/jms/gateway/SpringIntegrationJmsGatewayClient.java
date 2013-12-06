package levin.learn.spring.integration.jms.gateway;

import java.util.List;

import levin.learn.commons.model.Book;
import levin.learn.commons.service.BookService;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringIntegrationJmsGatewayClient {
	public static void main(String[] args) throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:META-INF/spring/jms/spring-integration-jms-gateway.xml",
				"classpath:META-INF/spring/jms/spring-jms.xml");
		
		context.registerShutdownHook();
		
		BookService bookService = context.getBean("bookService", BookService.class);
		
		List<Book> findBooks = bookService.findBooksByName("UMD Distilled");
		System.out.println("Find Books: \n" + findBooks);
		
		Book findBook = bookService.findBookByIsbn("7-100-04343-3");
		System.out.println("Find Book: \n" + findBook);
		
		System.exit(0);
	}
}
