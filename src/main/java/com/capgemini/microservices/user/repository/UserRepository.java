package com.capgemini.microservices.user.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.capgemini.microservices.user.model.User;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
public Optional<User> findByEmail(String email);

public void deleteByEmail(String email);

}
