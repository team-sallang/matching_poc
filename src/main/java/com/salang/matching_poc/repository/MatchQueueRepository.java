package com.salang.matching_poc.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.salang.matching_poc.model.entity.MatchQueue;
import com.salang.matching_poc.model.enums.MatchStatus;

import jakarta.persistence.LockModeType;

public interface MatchQueueRepository extends JpaRepository<MatchQueue, Long> {

        List<MatchQueue> findByStatus(MatchStatus status);

        Optional<MatchQueue> findByUserId(UUID userId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select mq from MatchQueue mq where mq.userId = :userId and mq.status = :status")
        Optional<MatchQueue> findByUserIdAndStatusForUpdate(@Param("userId") UUID userId,
                        @Param("status") MatchStatus status);

        @Query(value = """
                        select *
                        from match_queue
                        where status = :status
                          and user_id <> :userId
                          and gender <> :gender
                          and tier <> :excludedTier
                          and location = :region
                          and birth_year between :birthYearMin and :birthYearMax
                          and hobby_ids && :hobbyIds
                        order by created_at
                        limit 1
                        for update skip locked
                        """, nativeQuery = true)
        Optional<MatchQueue> findPhase1Match(
                        @Param("userId") UUID userId,
                        @Param("gender") String gender,
                        @Param("region") String region,
                        @Param("birthYearMin") Integer birthYearMin,
                        @Param("birthYearMax") Integer birthYearMax,
                        @Param("hobbyIds") Integer[] hobbyIds,
                        @Param("excludedTier") String excludedTier,
                        @Param("status") String status);

        @Query(value = """
                        select *
                        from match_queue
                        where status = :status
                          and user_id <> :userId
                          and gender <> :gender
                          and tier <> :excludedTier
                          and location = :region
                          and birth_year between :birthYearMin and :birthYearMax
                        order by created_at
                        limit 1
                        for update skip locked
                        """, nativeQuery = true)
        Optional<MatchQueue> findPhase2Match(
                        @Param("userId") UUID userId,
                        @Param("gender") String gender,
                        @Param("region") String region,
                        @Param("birthYearMin") Integer birthYearMin,
                        @Param("birthYearMax") Integer birthYearMax,
                        @Param("excludedTier") String excludedTier,
                        @Param("status") String status);

        @Query(value = """
                        select *
                        from match_queue
                        where status = :status
                          and user_id <> :userId
                          and gender <> :gender
                          and tier <> :excludedTier
                          and birth_year between :birthYearMin and :birthYearMax
                        order by created_at
                        limit 1
                        for update skip locked
                        """, nativeQuery = true)
        Optional<MatchQueue> findPhase3Match(
                        @Param("userId") UUID userId,
                        @Param("gender") String gender,
                        @Param("birthYearMin") Integer birthYearMin,
                        @Param("birthYearMax") Integer birthYearMax,
                        @Param("excludedTier") String excludedTier,
                        @Param("status") String status);

        @Query(value = """
                        select *
                        from match_queue
                        where status = :status
                          and user_id <> :userId
                          and gender <> :gender
                          and tier <> :excludedTier
                        order by created_at
                        limit 1
                        for update skip locked
                        """, nativeQuery = true)
        Optional<MatchQueue> findPhase4Match(
                        @Param("userId") UUID userId,
                        @Param("gender") String gender,
                        @Param("excludedTier") String excludedTier,
                        @Param("status") String status);

        @Query(value = """
                        select *
                        from match_queue
                        where status = :status
                          and user_id <> :userId
                        order by created_at
                        limit 1
                        for update skip locked
                        """, nativeQuery = true)
        Optional<MatchQueue> findPhase5Match(
                        @Param("userId") UUID userId,
                        @Param("status") String status);

        /**
         * status=oldStatus인 레코드를 newStatus로 원자적으로 변경.
         * @return 변경된 행 수 (2여야 매칭 확정 성공)
         */
        @Modifying(clearAutomatically = true)
        @Query("UPDATE MatchQueue mq SET mq.status = :newStatus WHERE mq.userId IN :userIds AND mq.status = :oldStatus")
        int updateStatusIf(@Param("userIds") List<UUID> userIds,
                        @Param("oldStatus") MatchStatus oldStatus,
                        @Param("newStatus") MatchStatus newStatus);
}
