package levin.learn.spring.integration.jms.gateway;

public class SpringJmsSimpleClient {
	public static void main(String[] args) throws Exception {
//		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
//				"classpath:META-INF/spring/jms/spring-jms.xml");
//
//		context.registerShutdownHook();
//
//		JmsTemplate jmsTemplate = context.getBean("jmsTemplate", JmsTemplate.class);
//		Queue requestQueue = context.getBean("findBooksByNameRequestQueue", Queue.class);
//		Queue responseQueue = context.getBean("findBooksByNameResponseQueue", Queue.class);
//
//		jmsTemplate.send(requestQueue, new MessageCreator() {
//			public Message createMessage(Session session) throws JMSException {
//				return session.createObjectMessage("UMD Distilled");
//			}
//		});
//
//		Message responseMsg = jmsTemplate.receive(responseQueue);
//
//		System.out.println(((ObjectMessage)responseMsg).getObject());
//
//		System.exit(0);
	}
}
