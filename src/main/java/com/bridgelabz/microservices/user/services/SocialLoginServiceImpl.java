package com.bridgelabz.microservices.user.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.social.facebook.connect.FacebookConnectionFactory;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.OAuth2Operations;
import org.springframework.social.oauth2.OAuth2Parameters;
import org.springframework.stereotype.Service;

import com.bridgelabz.microservices.user.exception.RegistrationException;
import com.bridgelabz.microservices.user.model.FacebookUser;
import com.bridgelabz.microservices.user.model.User;
import com.bridgelabz.microservices.user.repository.ElasticRepositoryForUser;
import com.bridgelabz.microservices.user.repository.UserRepository;

@Service
public class SocialLoginServiceImpl implements SocialLoginService {

	@Value("${spring.social.facebook.appId}")
	String facebookAppId;
	@Value("${spring.social.facebook.appSecret}")
	String facebookSecret;

	String accessToken;

	@Autowired
	private Environment environment;
	
	@Autowired
	private ElasticRepositoryForUser userElasticRepository;
	
	@Autowired
	private UserRepository userRepository;

	@Override
	public String createFacebookAuthorizationURL() {
		FacebookConnectionFactory connectionFactory = new FacebookConnectionFactory(facebookAppId, facebookSecret);
		OAuth2Operations oauthOperations = connectionFactory.getOAuthOperations();
		OAuth2Parameters params = new OAuth2Parameters();
		params.setRedirectUri(environment.getProperty("RedirectUri"));
		params.setScope("public_profile,email,user_birthday");
		return oauthOperations.buildAuthorizeUrl(params);
	}

	@Override
	public void createFacebookAccessToken(String code) {
		FacebookConnectionFactory connectionFactory = new FacebookConnectionFactory(facebookAppId, facebookSecret);
		AccessGrant accessGrant = connectionFactory.getOAuthOperations().exchangeForAccess(code,
				environment.getProperty("RedirectUri"), null);
		accessToken = accessGrant.getAccessToken();
	}

	@Override
	public Object getName() throws RegistrationException {
		Facebook facebook = new FacebookTemplate(accessToken);
		// String[] fields = {"id", "name"};
		FacebookUser userFb= facebook.fetchObject("me", FacebookUser.class, "email");
		 
		userFb.getEmail();
		Optional<User> checkUser=userElasticRepository.findByEmail(userFb.getEmail());
		if(checkUser.isPresent()) {
			throw new RegistrationException(environment.getProperty("RegistrationException"));
		}
		
		User user1=new User();
		user1.setEmail(userFb.getEmail());
		user1.setId(userFb.getId());
		user1.setFirstname(userFb.getName());
		userElasticRepository.save(user1);
		userRepository.save(user1);
		
		return user1;
	}
}