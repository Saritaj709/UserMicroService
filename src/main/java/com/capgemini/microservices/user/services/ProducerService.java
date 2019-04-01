package com.capgemini.microservices.user.services;

import com.capgemini.microservices.user.model.MailDTO;

public interface ProducerService {
public void sender(MailDTO dto);
}
