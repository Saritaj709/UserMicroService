package com.capgemini.microservices.user.services;

import com.capgemini.microservices.user.model.MailDTO;

public interface ConsumerService {
public void receiver(MailDTO dto);
}
