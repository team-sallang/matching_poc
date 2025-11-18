package com.salang.matching_poc.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_history")
@Getter
@Builder
@NoArgsConstructor // 코드 수정 필요.
@AllArgsConstructor // 코드 수정 필요.
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
}
