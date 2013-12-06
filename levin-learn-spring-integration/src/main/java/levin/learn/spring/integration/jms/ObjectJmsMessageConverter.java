package levin.learn.spring.integration.jms;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;

public class ObjectJmsMessageConverter implements MessageConverter {

	public Object fromMessage(Message msg) throws JMSException, MessageConversionException {
		if(!(msg instanceof ObjectMessage)) {
			throw new MessageConversionException("msg instance is not an ObjectMessage: " + msg.getClass());
		}
		ObjectMessage objMsg = (ObjectMessage)msg;
		return objMsg.getObject();
	}

	public Message toMessage(Object obj, Session session) throws JMSException, MessageConversionException {
		if(!(obj instanceof Serializable)) {
			throw new MessageConversionException("obj instance is not serializable: " + obj.getClass());
		}
		return session.createObjectMessage((Serializable)obj);
	}
	
}
