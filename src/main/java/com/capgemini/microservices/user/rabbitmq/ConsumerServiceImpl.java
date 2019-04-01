package com.capgemini.microservices.user.rabbitmq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.capgemini.microservices.user.mail.MailService;
import com.capgemini.microservices.user.model.MailDTO;
import com.capgemini.microservices.user.services.ConsumerService;

@Component
public class ConsumerServiceImpl implements ConsumerService {

	@Autowired
	private MailService mailService;

	@Override
	@RabbitListener(queues = "${bridgelabz.rabbitmq.queue}")
	public void receiver(MailDTO msg) {
		mailService.sendMail(msg);
	}
}
