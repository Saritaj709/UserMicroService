package com.capgemini.microservices.user.mail;

import com.capgemini.microservices.user.model.MailDTO;

public interface MailService {
	public boolean sendMail(MailDTO mail);	
}
