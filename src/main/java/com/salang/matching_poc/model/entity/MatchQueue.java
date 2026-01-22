package com.salang.matching_poc.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.salang.matching_poc.model.enums.Gender;
import com.salang.matching_poc.model.enums.MatchStatus;
import com.salang.matching_poc.model.enums.Region;
import com.salang.matching_poc.model.enums.Tier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "match_queue", indexes = {
        @Index(name = "idx_match_queue_status", columnList = "status"),
        @Index(name = "idx_match_queue_created_at", columnList = "created_at")
}) // 해당 쿼리문은 복합 인덱스 개선 여지가 있음. 고려 필요
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "queue_id")
    private Long queueId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Setter
    private MatchStatus status = MatchStatus.WAITING;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "hobby_ids")
    private Integer[] hobbyIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", length = 20)
    private Tier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "location", length = 50)
    private Region region;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public MatchQueue(UUID userId, Integer[] hobbyIds, Tier tier, Region region, Integer birthYear, Gender gender) {
        this.userId = userId;
        this.hobbyIds = hobbyIds != null ? hobbyIds : new Integer[0];
        this.tier = tier;
        this.region = region;
        this.birthYear = birthYear;
        this.gender = gender;
        this.status = MatchStatus.WAITING;
    }
}
