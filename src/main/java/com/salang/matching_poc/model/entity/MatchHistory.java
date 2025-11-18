package com.salang.matching_poc.model.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class MatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_a_id", nullable = false)
    private UUID userAId;

    @Column(name = "user_b_id", nullable = false)
    private UUID userBId;

    @Column(name = "matched_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime matchedAt;

    @Column(name = "match_duration_ms")
    private Integer matchDurationMs;

    @Builder
    public MatchHistory(UUID userAId, UUID userBId, LocalDateTime matchedAt, Integer matchDurationMs) {
        this.userAId = userAId;
        this.userBId = userBId;
        this.matchedAt = matchedAt;
        this.matchDurationMs = matchDurationMs;
    }
}
