package com.bridgelabz.microservices.user.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.spi.RegisterableService;
import javax.xml.bind.DatatypeConverter;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.bridgelabz.microservices.user.exception.ActivationException;
import com.bridgelabz.microservices.user.exception.LoginException;
import com.bridgelabz.microservices.user.exception.RegistrationException;
import com.bridgelabz.microservices.user.exception.RestHighLevelClientException;
import com.bridgelabz.microservices.user.exception.UserNotFoundException;
import com.bridgelabz.microservices.user.model.LoginDTO;
import com.bridgelabz.microservices.user.model.MailDTO;
import com.bridgelabz.microservices.user.model.PasswordDTO;
import com.bridgelabz.microservices.user.model.RegistrationDTO;
import com.bridgelabz.microservices.user.model.User;
import com.bridgelabz.microservices.user.repository.ElasticRepositoryForUser;
import com.bridgelabz.microservices.user.repository.RedisRepository;
import com.bridgelabz.microservices.user.repository.UserRepository;
import com.bridgelabz.microservices.user.utility.UserUtility;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Repository
public class UserDao {
	  private final String INDEX = "userdb";
	  private final String TYPE = "user";

	    private RestHighLevelClient restHighLevelClient;

	    private ObjectMapper objectMapper;
	    
	    @Autowired
		private ModelMapper modelMapper;
	    
	    @Autowired
		private UserRepository userRepository;
	    
	    @Autowired
		private ElasticRepositoryForUser userElasticRepository;
	    
	    @Autowired
		private PasswordEncoder passwordEncoder;
	    
	    @Autowired
		private ProducerService producer;
	    
	    @Autowired
		private Environment environment;
		
		@Autowired
		private JwtToken jwtToken;
		
		@Autowired
		private RedisRepository redisRepository; 
		
		@Autowired
		private AwsS3Service awsS3Service;

	    public UserDao( ObjectMapper objectMapper, RestHighLevelClient restHighLevelClient) {
	        this.objectMapper = objectMapper;
	        this.restHighLevelClient = restHighLevelClient;
	    }

	    
		public User registerUser(RegistrationDTO dto) throws RegistrationException{
	      /*  user.setId(UUID.randomUUID().toString());*/
	        /*@SuppressWarnings("unchecked")
			Map<String, Object> dataMap = objectMapper.convertValue(user, Map.class);*/
	    	UserUtility.validateUser(dto);

			Optional<User> checkUser = userRepository.findByEmail(dto.getEmail());

			if (checkUser.isPresent()) {
				throw new RegistrationException("User email already exists,unable to register");

			}
	    	User user = modelMapper.map(dto, User.class);
	    	
	    	@SuppressWarnings("unchecked")
	    	Map<String, Object> dataMap = objectMapper.convertValue(user, Map.class);
	        IndexRequest indexRequest = new IndexRequest(INDEX, TYPE, user.getId())
	                .source(dataMap);
	        try {
	            @SuppressWarnings("unused")
				IndexResponse response = restHighLevelClient.index(indexRequest);
	        } catch(ElasticsearchException e) {
	            e.getDetailedMessage();
	        } catch (java.io.IOException ex){
	            ex.getLocalizedMessage();
	        }
	        
	        userRepository.insert(user);
			userElasticRepository.save(user);
			
	        String jwt = jwtToken.tokenGenerator(user.getId());

			jwtToken.parseJwtToken(jwt);

			MailDTO mail = new MailDTO();
			mail.setTo(dto.getEmail());
			mail.setSubject("Account activation mail");
			mail.setText(environment.getProperty("accountActivationLink") + jwt);
			producer.sender(mail);
	        return user;
	    }
		
		public void passwordReset(String uuid, PasswordDTO dto) throws UserNotFoundException, RegistrationException {
			
			UserUtility.validateReset(dto);
			
			String email= redisRepository.get(uuid);

			//Optional<User> user = userRepository.findById(userId);
			Optional<User> user = userElasticRepository.findByEmail(email);

			if (!user.isPresent()) {
				throw new UserNotFoundException("User not found");
			}

			user.get().setPassword(passwordEncoder.encode(dto.getPassword()));
			userRepository.save(user.get());
			userElasticRepository.save(user.get());
			redisRepository.delete(uuid);

		}
		
		public String uploadPic(String token, String pic) throws UserNotFoundException {
			Claims claims = Jwts.parser().setSigningKey(DatatypeConverter.parseBase64Binary(environment.getProperty("Key")))
					.parseClaimsJws(token).getBody();

			// Optional<User> user = userRepository.findById(claims.getSubject());
			Optional<User> user = userElasticRepository.findById(claims.getSubject());

	        User mainUser=user.get();

			if (!claims.getSubject().equals(mainUser.getId())) {
				throw new UserNotFoundException(environment.getProperty("UserNotFoundException"));
			}
			
			mainUser.setProfilePic(environment.getProperty("imageLinkForUser") + pic);
			
			awsS3Service.uploadFile(environment.getProperty("bucketName"), environment.getProperty("folderNameForUser"), pic);

			userRepository.save(mainUser);
			userElasticRepository.save(mainUser);
			return pic;
		}

		public String removePic(String token) throws UserNotFoundException {
			Claims claims = Jwts.parser().setSigningKey(DatatypeConverter.parseBase64Binary(environment.getProperty("Key")))
					.parseClaimsJws(token).getBody();
	        Optional<User> user=userElasticRepository.findById(claims.getSubject());
	        
	        User mainUser=user.get();
	        if(!claims.getSubject().equals(mainUser.getId())) {
	        	throw new UserNotFoundException(environment.getProperty("UserNotFoundException"));
	        }
	        
	        mainUser.setProfilePic(null);

	        userRepository.save(mainUser);
	        userElasticRepository.save(mainUser);
			return "1";
		}
	   /* public Map<String, Object> getBookById(String id){
	        GetRequest getRequest = new GetRequest(INDEX, TYPE, id);
	        GetResponse getResponse = null;
	        try {
	            getResponse = restHighLevelClient.get(getRequest);
	        } catch (java.io.IOException e){
	            e.getLocalizedMessage();
	        }
	        Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
	        return sourceAsMap;
	    }*/
	    
	    public String loginUser(LoginDTO loginDto) throws LoginException, UserNotFoundException, ActivationException, IOException, RestHighLevelClientException {

			UserUtility.validateLogin(loginDto);

			//Optional<User> checkUser = userRepository.findByEmail(loginDto.getEmail());
			Optional<User> checkUser = userElasticRepository.findByEmail(loginDto.getEmail());

			if (!checkUser.isPresent()) {
				throw new UserNotFoundException("This Email id does not exist");
			}
			
			GetRequest getRequest = new GetRequest(INDEX, TYPE, checkUser.get().getId());
		        GetResponse getResponse = null;
		        getResponse = restHighLevelClient.get(getRequest);
			if(getResponse.equals(null)) {
				throw new RestHighLevelClientException("Some exception related to rest high level client occured");
			}
			if (!checkUser.get().isActivate()) {
				throw new ActivationException("User account is not activated yet");
			}

			if (!passwordEncoder.matches(loginDto.getPassword(), checkUser.get().getPassword())) {
				throw new LoginException("Password unmatched");
			}

			String jwt = jwtToken.tokenGenerator(checkUser.get().getId());
			return jwt;

		}
	    
	    public void forgetPassword(String email) throws UserNotFoundException {

	    	//	Optional<User> user = userRepository.findById(id);
	    		Optional<User> user = userElasticRepository.findByEmail(email);


	    		if (!user.isPresent()) {
	    			throw new UserNotFoundException("User is not present");
	    		}

	    		String uuid=UserUtility.generateUUId();
	    		redisRepository.save(uuid,email);

	    		MailDTO mail = new MailDTO();
	    		mail.setTo(email);
	    		mail.setSubject("Password reset mail");
	    		mail.setText(environment.getProperty("passwordResetLink")  + uuid);

	    		producer.sender(mail);
	    		//mailService.sendMail(mail);
	    	}

	   /* public Map<String, Object> updateBookById(String id, Book book){
	        UpdateRequest updateRequest = new UpdateRequest(INDEX, TYPE, id)
	                .fetchSource(true);    // Fetch Object after its update
	        Map<String, Object> error = new HashMap<>();
	        error.put("Error", "Unable to update book");
	        try {
	            String bookJson = objectMapper.writeValueAsString(book);
	            updateRequest.doc(bookJson, XContentType.JSON);
	            UpdateResponse updateResponse = restHighLevelClient.update(updateRequest);
	            Map<String, Object> sourceAsMap = updateResponse.getGetResult().sourceAsMap();
	            return sourceAsMap;
	        }catch (JsonProcessingException e){
	            e.getMessage();
	        } catch (java.io.IOException e){
	            e.getLocalizedMessage();
	        }
	        return error;
	    }*/

	   /* public void deleteBookById(String id) {
	        DeleteRequest deleteRequest = new DeleteRequest(INDEX, TYPE, id);
	        try {
	            @SuppressWarnings("unused")
				DeleteResponse deleteResponse = restHighLevelClient.delete(deleteRequest);
	        } catch (java.io.IOException e){
	            e.getLocalizedMessage();
	        }
	    }*/

}
