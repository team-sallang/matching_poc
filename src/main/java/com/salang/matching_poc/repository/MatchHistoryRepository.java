package com.salang.matching_poc.repository;

import com.salang.matching_poc.model.entity.MatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MatchHistoryRepository extends JpaRepository<MatchHistory, UUID> {
}

