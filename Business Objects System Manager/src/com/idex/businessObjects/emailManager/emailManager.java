package com.idex.businessObjects.emailManager;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class emailManager {
	public static void sendmessage(String recipient, String messageSubject, String messageText) {
		String host = "relay.idexcorpnet.com";
		//host = "mail.idexcorp.com";
		
		String from = "dkirk@idexcorp.com";
		String to = recipient;

		try {
			
			// Get system properties
			Properties props = System.getProperties();

			// Setup mail server
			props.put("mail.smtp.host", host);

			// Get session
			Session session = Session.getDefaultInstance(props, null);

			// Define message
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			message.setSubject(messageSubject);
			message.setText(messageText);
			message.setContent(messageText, "text/html");

			// Send message
			Transport.send(message);
		} catch (Exception ex) {
			
		}

	}
}
