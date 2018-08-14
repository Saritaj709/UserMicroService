package com.bridgelabz.microservices.user.services;

import com.bridgelabz.microservices.user.exception.RegistrationException;

public interface SocialLoginService {

	public String createFacebookAuthorizationURL();
	public void createFacebookAccessToken(String code);
	public Object getName() throws RegistrationException;
}
