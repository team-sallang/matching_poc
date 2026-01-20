package com.salang.matching_poc.repository;

import com.salang.matching_poc.model.entity.Hobby;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HobbyRepository extends JpaRepository<Hobby, Integer> {
}
