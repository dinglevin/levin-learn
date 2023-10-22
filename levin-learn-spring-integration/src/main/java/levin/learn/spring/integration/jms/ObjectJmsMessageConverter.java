package levin.learn.spring.integration.jms;

//public class ObjectJmsMessageConverter implements MessageConverter {
//
//	public Object fromMessage(Message msg) throws JMSException, MessageConversionException {
//		if(!(msg instanceof ObjectMessage)) {
//			throw new MessageConversionException("msg instance is not an ObjectMessage: " + msg.getClass());
//		}
//		ObjectMessage objMsg = (ObjectMessage)msg;
//		return objMsg.getObject();
//	}
//
//	public Message toMessage(Object obj, Session session) throws JMSException, MessageConversionException {
//		if(!(obj instanceof Serializable)) {
//			throw new MessageConversionException("obj instance is not serializable: " + obj.getClass());
//		}
//		return session.createObjectMessage((Serializable)obj);
//	}
//
//}
