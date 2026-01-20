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

@Service
@RequiredArgsConstructor
public class MatchService {

    /** 1단계 Intercept·스케줄러 1~3단계: 나이 ±N세 */
    private static final int AGE_TOLERANCE_YEARS = 5;

    private final UserRepository userRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final MatchQueueRepository matchQueueRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public MatchResponse<?> requestMatch(MatchRequest request) {
        UUID userId = request.userId();
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (matchQueueRepository.findByUserId(userId).isPresent()) {
            throw new AlreadyInQueueException();
        }

        Optional<MatchQueue> partner = findInterceptPartner(user);
        if (partner.isPresent()) {
            MatchQueue p = partner.get();
            Room room = createRoom(user, p.getUserId());
            p.setStatus(MatchStatus.MATCHED);
            matchQueueRepository.save(p);
            return MatchResponse.matched(room.getRoomId(), OffsetDateTime.now(MatchingConstants.ZONE_ASIA_SEOUL));
        }

        MatchQueue queued = buildQueueEntry(user);
        matchQueueRepository.save(queued);

        return MatchResponse.waiting("매칭 대기열에 등록되었습니다. Supabase Realtime을 통해 매칭 결과를 기다려주세요.",
                OffsetDateTime.now(MatchingConstants.ZONE_ASIA_SEOUL));
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
        return matchQueueRepository.findPhase1Match(
                requester.getId(),
                requester.getGender().name(),
                requester.getRegion().name(),
                birthYear - AGE_TOLERANCE_YEARS,
                birthYear + AGE_TOLERANCE_YEARS,
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
