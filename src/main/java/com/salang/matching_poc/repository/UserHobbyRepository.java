package com.salang.matching_poc.repository;

import com.salang.matching_poc.model.entity.UserHobby;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHobbyRepository extends JpaRepository<UserHobby, Long> {
}
