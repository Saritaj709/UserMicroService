package com.capgemini.microservices.user.utility;

import java.util.UUID;

import com.capgemini.microservices.user.exception.LoginException;
import com.capgemini.microservices.user.exception.RegistrationException;
import com.capgemini.microservices.user.model.LoginDTO;
import com.capgemini.microservices.user.model.PasswordDTO;
import com.capgemini.microservices.user.model.RegistrationDTO;

public class UserUtility {

	private final static String EMAIL = "^([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})$";

	public static void validateUser(RegistrationDTO user) throws RegistrationException {
		if (!user.getEmail().matches(EMAIL)) {
			throw new RegistrationException("User email is not valid ,follow abc@gmail.com,abc.100@yahoo.com");
		} else if (user.getFirstname().length() < 3) {
			throw new RegistrationException("User Firstname should have atleast 3 characters");
		} else if (user.getLastname().length() < 3) {
			throw new RegistrationException("User Lastname should have atleast 3 characters ");
		}

		else if (user.getPhoneNo().length() != 10) {
			throw new RegistrationException("contact no. is invalid");
		} else if (user.getPassword().length() < 4) {
			throw new RegistrationException("Password is invalid,should have atleast 4 characters");
		} else if (!user.getConfirmPassword().equals(user.getPassword())) {
			throw new RegistrationException("Password is invalid ,both password should be same ");
		}
	}

	public static void validateLogin(LoginDTO loginDto) throws LoginException {
		if (!loginDto.getEmail().matches(EMAIL)) {
			throw new LoginException("Email format is not valid");
		}
	}

	public static void validateReset(PasswordDTO passwordDto) throws RegistrationException {
		if (!passwordDto.getPassword().equals(passwordDto.getConfirmPassword())) {
			throw new RegistrationException("Passwords should be same");
		}
	}

	public static void validateEmail(String email) throws LoginException {
		if (!email.matches(EMAIL)) {
			throw new LoginException("Invalid email format");
		}
	}

	public static String generateUUId(){
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}
}
