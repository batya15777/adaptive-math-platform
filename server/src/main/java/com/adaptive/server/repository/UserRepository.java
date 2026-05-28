package com.adaptive.server.repository;

import com.adaptive.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {

    boolean existsByEmail(String email);
}
