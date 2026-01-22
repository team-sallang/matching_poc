package com.salang.matching_poc.repository;

import com.salang.matching_poc.model.entity.User;
import com.salang.matching_poc.model.entity.UserHobby;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserHobbyRepository extends JpaRepository<UserHobby, Long> {
    List<UserHobby> findAllByUser(User user);
}
