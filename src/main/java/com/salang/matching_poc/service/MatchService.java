package com.salang.matching_poc.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salang.matching_poc.constants.MatchingConstants;
import com.salang.matching_poc.controller.dto.MatchRequest;
import com.salang.matching_poc.controller.dto.MatchResponse;
import com.salang.matching_poc.exception.AlreadyInQueueException;
import com.salang.matching_poc.exception.UserNotFoundException;
import com.salang.matching_poc.exception.UserNotInQueueException;
import com.salang.matching_poc.model.entity.MatchQueue;
import com.salang.matching_poc.model.entity.Room;
import com.salang.matching_poc.model.entity.User;
import com.salang.matching_poc.model.entity.UserHobby;
import com.salang.matching_poc.model.enums.MatchStatus;
import com.salang.matching_poc.repository.MatchQueueRepository;
import com.salang.matching_poc.repository.RoomRepository;
import com.salang.matching_poc.repository.UserHobbyRepository;
import com.salang.matching_poc.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final UserRepository userRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final MatchQueueRepository matchQueueRepository;
    private final RoomRepository roomRepository;
    private final MatchQueueMatchFinder matchQueueMatchFinder;

    @Transactional
    public MatchResponse<?> requestMatch(MatchRequest request) {
        UUID userId = request.userId();
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (matchQueueRepository.findByUserId(userId).isPresent()) {
            throw new AlreadyInQueueException();
        }

        Optional<MatchQueue> partner = findInterceptPartner(user);
        return partner
                .<MatchResponse<?>>map(p -> doInterceptAndReturn(user, p))
                .orElseGet(() -> doEnqueueAndReturn(user));
    }

    /** 즉시 매칭: room 생성, 파트너 MATCHED 처리 후 MatchResponse.matched 반환. */
    private MatchResponse<?> doInterceptAndReturn(User user, MatchQueue partner) {
        Room room = createRoom(user, partner.getUserId());
        partner.setStatus(MatchStatus.MATCHED);
        matchQueueRepository.save(partner);
        return MatchResponse.matched(room.getRoomId(),
                OffsetDateTime.now(MatchingConstants.ZONE_ASIA_SEOUL));
    }

    /** 대기열 등록 후 MatchResponse.waiting 반환. */
    private MatchResponse<?> doEnqueueAndReturn(User user) {
        MatchQueue queued = buildQueueEntry(user);
        matchQueueRepository.save(queued);
        return MatchResponse.waiting("매칭 대기열에 등록되었습니다. Supabase Realtime을 통해 매칭 결과를 기다려주세요.",
                OffsetDateTime.now(MatchingConstants.ZONE_ASIA_SEOUL));
    }

    /**
     * 스케줄러/인터셉트에서 파트너 확정 후 호출. WAITING→MATCHED 조건부 업데이트로 원자성 보장.
     * updated != 2 이면 이미 타 스레드/인스턴스에서 매칭된 경우 → 롤백.
     */
    @Transactional
    public void confirmMatch(UUID user1Id, UUID user2Id) {
        int updated = matchQueueRepository.updateStatusIf(
                List.of(user1Id, user2Id), MatchStatus.WAITING, MatchStatus.MATCHED);
        if (updated != 2) {
            throw new IllegalStateException(
                    "Match confirmation failed: one or both users already matched. updated=" + updated);
        }
        User user1 = userRepository.findById(user1Id).orElseThrow(UserNotFoundException::new);
        User user2 = userRepository.findById(user2Id).orElseThrow(UserNotFoundException::new);
        Room room = Room.builder().user1(user1).user2(user2).build();
        roomRepository.save(room);
        log.info("매칭 성공! 사용자1: {}, 사용자2: {}", user1Id, user2Id);
        log.info("채팅방 생성 완료. Room ID: {}", room.getRoomId());
    }

    @Transactional
    public void cancelMatch(MatchRequest request) {
        UUID userId = request.userId();
        MatchQueue queue = matchQueueRepository.findByUserId(userId)
                .orElseThrow(UserNotInQueueException::new);
        if (queue.getStatus() != MatchStatus.WAITING) {
            throw new UserNotInQueueException("이미 매칭되어 취소할 수 없습니다.");
        }
        matchQueueRepository.delete(queue);
    }

    private Optional<MatchQueue> findInterceptPartner(User requester) {
        Integer[] hobbyIds = loadHobbyIds(requester);
        int birthYear = requester.getBirthDate().getYear();
        return matchQueueMatchFinder.findPhase1Match(
                requester.getId(),
                requester.getGender().name(),
                requester.getRegion().name(),
                birthYear - MatchingConstants.AGE_TOLERANCE_YEARS,
                birthYear + MatchingConstants.AGE_TOLERANCE_YEARS,
                hobbyIds,
                MatchingConstants.EXCLUDED_TIER,
                MatchingConstants.WAITING_STATUS);
    }

    private MatchQueue buildQueueEntry(User user) {
        return MatchQueue.builder()
                .userId(user.getId())
                .hobbyIds(loadHobbyIds(user))
                .tier(user.getTier())
                .region(user.getRegion())
                .birthYear(user.getBirthDate().getYear())
                .gender(user.getGender())
                .build();
    }

    private Integer[] loadHobbyIds(User user) {
        List<UserHobby> hobbies = userHobbyRepository.findAllByUser(user);
        return hobbies.stream()
                .map(UserHobby::getHobby)
                .map(hobby -> hobby.getId())
                .toArray(Integer[]::new);
    }

    private Room createRoom(User user1, UUID user2Id) {
        User user2 = userRepository.findById(user2Id).orElseThrow(UserNotFoundException::new);
        Room room = Room.builder()
                .user1(user1)
                .user2(user2)
                .build();
        return roomRepository.save(room);
    }
}
