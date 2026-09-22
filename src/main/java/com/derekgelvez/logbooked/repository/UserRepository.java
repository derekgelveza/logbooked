package com.derekgelvez.logbooked.repository;

import com.derekgelvez.logbooked.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    //these are for login
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    //these are for registration
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);



}
