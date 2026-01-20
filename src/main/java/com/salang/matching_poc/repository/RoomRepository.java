package com.salang.matching_poc.repository;

import com.salang.matching_poc.model.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
}
