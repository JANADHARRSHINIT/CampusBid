package com.campusbid.backend.repository;

import com.campusbid.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentId(String studentId);

    Optional<User> findByVerificationToken(String token);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);
}
