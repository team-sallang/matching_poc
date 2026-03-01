package com.salang.matching_poc.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.salang.matching_poc.model.entity.Room;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findFirstByUser1IdOrUser2Id(UUID user1Id, UUID user2Id);
}
