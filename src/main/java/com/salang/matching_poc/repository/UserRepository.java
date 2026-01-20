package com.salang.matching_poc.repository;

import com.salang.matching_poc.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
