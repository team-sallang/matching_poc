package com.salang.matching_poc.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.salang.matching_poc.controller.dto.MatchRequest;
import com.salang.matching_poc.controller.dto.MatchResponse;
import com.salang.matching_poc.exception.AlreadyInQueueException;
import com.salang.matching_poc.exception.UserNotFoundException;
import com.salang.matching_poc.exception.UserNotInQueueException;
import com.salang.matching_poc.model.entity.MatchQueue;
import com.salang.matching_poc.model.entity.Room;
import com.salang.matching_poc.model.entity.User;
import com.salang.matching_poc.model.enums.Gender;
import com.salang.matching_poc.model.enums.MatchStatus;
import com.salang.matching_poc.model.enums.Region;
import com.salang.matching_poc.model.entity.Hobby;
import com.salang.matching_poc.model.entity.UserHobby;
import com.salang.matching_poc.repository.HobbyRepository;
import com.salang.matching_poc.repository.MatchQueueRepository;
import com.salang.matching_poc.repository.RoomRepository;
import com.salang.matching_poc.repository.UserHobbyRepository;
import com.salang.matching_poc.repository.UserRepository;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("MatchService 통합 테스트")
@SuppressWarnings("null")
class MatchServiceTest {

    @Autowired
    private MatchService matchService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchQueueRepository matchQueueRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HobbyRepository hobbyRepository;

    @Autowired
    private UserHobbyRepository userHobbyRepository;

    private User testUser;
    private UUID testUserId;
    private MatchRequest matchRequest;

    @BeforeEach
    @SuppressWarnings("unused") // IDE가 @BeforeEach의 런타임 실행을 인식하지 못함
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .nickname("test_user")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(1995, 1, 1))
                .region(Region.SEOUL)
                .build();
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();
        matchRequest = new MatchRequest(testUserId);
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 UserNotFoundException 발생")
    void requestMatch_UserNotFound_ThrowsException() {
        UUID nonExistentUserId = UUID.randomUUID();
        MatchRequest request = new MatchRequest(nonExistentUserId);

        assertThatThrownBy(() -> matchService.requestMatch(request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("이미 큐에 있으면 AlreadyInQueueException 발생")
    void requestMatch_AlreadyInQueue_ThrowsException() {
        MatchQueue existingQueue = MatchQueue.builder()
                .userId(testUserId)
                .tier(testUser.getTier())
                .region(testUser.getRegion())
                .birthYear(testUser.getBirthDate().getYear())
                .gender(testUser.getGender())
                .hobbyIds(new Integer[0])
                .build();
        matchQueueRepository.save(existingQueue);

        assertThatThrownBy(() -> matchService.requestMatch(matchRequest))
                .isInstanceOf(AlreadyInQueueException.class);
    }

    @Test
    @DisplayName("즉시 매칭 성공 시 matched 응답 반환")
    void requestMatch_InterceptSuccess_ReturnsMatchedResponse() {
        // 공통 취미 생성
        Hobby commonHobby = Hobby.builder()
                .category("스포츠")
                .name("축구")
                .build();
        commonHobby = hobbyRepository.save(commonHobby);

        // 테스트 사용자에 취미 추가
        UserHobby testUserHobby = UserHobby.builder()
                .user(testUser)
                .hobby(commonHobby)
                .build();
        userHobbyRepository.save(testUserHobby);

        // 파트너 사용자 생성
        User partnerUser = User.builder()
                .nickname("partner")
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(1996, 1, 1))
                .region(Region.SEOUL)
                .build();
        partnerUser = userRepository.save(partnerUser);

        // 파트너 사용자에 취미 추가
        UserHobby partnerUserHobby = UserHobby.builder()
                .user(partnerUser)
                .hobby(commonHobby)
                .build();
        userHobbyRepository.save(partnerUserHobby);

        // 파트너를 큐에 등록 (취미 ID 포함)
        MatchQueue partner = MatchQueue.builder()
                .userId(partnerUser.getId())
                .tier(partnerUser.getTier())
                .region(partnerUser.getRegion())
                .birthYear(partnerUser.getBirthDate().getYear())
                .gender(partnerUser.getGender())
                .hobbyIds(new Integer[] { commonHobby.getId() })
                .build();
        matchQueueRepository.save(partner);

        // 매칭 요청
        MatchResponse<?> response = matchService.requestMatch(matchRequest);

        // 검증
        assertThat(response.getStatus()).isEqualTo("MATCHED");
        assertThat(response.getData()).isNotNull();

        // 실제 DB 상태 검증
        Optional<MatchQueue> updatedPartner = matchQueueRepository.findByUserId(partnerUser.getId());
        assertThat(updatedPartner).isPresent();
        assertThat(updatedPartner.get().getStatus()).isEqualTo(MatchStatus.MATCHED);

        // Room 생성 확인
        List<Room> rooms = roomRepository.findAll();
        assertThat(rooms).hasSize(1);
        Room createdRoom = rooms.get(0);
        assertThat(createdRoom.getUser1().getId()).isIn(testUserId, partnerUser.getId());
        assertThat(createdRoom.getUser2().getId()).isIn(testUserId, partnerUser.getId());
    }

    @Test
    @DisplayName("즉시 매칭 실패 시 waiting 응답 반환 및 큐 등록")
    void requestMatch_InterceptFailed_ReturnsWaitingResponse() {
        // 매칭할 파트너가 없는 상황
        MatchResponse<?> response = matchService.requestMatch(matchRequest);

        // 검증
        assertThat(response.getStatus()).isEqualTo("WAITING");
        assertThat(response.getData()).isNotNull();

        // 실제 DB 상태 검증: 큐에 등록되었는지 확인
        Optional<MatchQueue> queued = matchQueueRepository.findByUserId(testUserId);
        assertThat(queued).isPresent();
        assertThat(queued.get().getStatus()).isEqualTo(MatchStatus.WAITING);

        // Room은 생성되지 않았는지 확인
        List<Room> rooms = roomRepository.findAll();
        assertThat(rooms).isEmpty();
    }

    @Test
    @DisplayName("confirmMatch: 두 사용자 모두 WAITING 상태면 매칭 성공")
    void confirmMatch_BothWaiting_Success() {
        // 두 번째 사용자 생성
        User user2 = User.builder()
                .nickname("user2")
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(1996, 1, 1))
                .region(Region.BUSAN)
                .build();
        user2 = userRepository.save(user2);

        // 두 사용자를 큐에 등록
        MatchQueue queue1 = MatchQueue.builder()
                .userId(testUserId)
                .tier(testUser.getTier())
                .region(testUser.getRegion())
                .birthYear(testUser.getBirthDate().getYear())
                .gender(testUser.getGender())
                .hobbyIds(new Integer[0])
                .build();
        matchQueueRepository.save(queue1);

        MatchQueue queue2 = MatchQueue.builder()
                .userId(user2.getId())
                .tier(user2.getTier())
                .region(user2.getRegion())
                .birthYear(user2.getBirthDate().getYear())
                .gender(user2.getGender())
                .hobbyIds(new Integer[0])
                .build();
        matchQueueRepository.save(queue2);

        // 매칭 확정
        matchService.confirmMatch(testUserId, user2.getId());

        // 실제 DB 상태 검증
        Optional<MatchQueue> updatedQueue1 = matchQueueRepository.findByUserId(testUserId);
        Optional<MatchQueue> updatedQueue2 = matchQueueRepository.findByUserId(user2.getId());
        assertThat(updatedQueue1).isPresent();
        assertThat(updatedQueue1.get().getStatus()).isEqualTo(MatchStatus.MATCHED);
        assertThat(updatedQueue2).isPresent();
        assertThat(updatedQueue2.get().getStatus()).isEqualTo(MatchStatus.MATCHED);

        // Room 생성 확인
        List<Room> rooms = roomRepository.findAll();
        assertThat(rooms).hasSize(1);
    }

    @Test
    @DisplayName("confirmMatch: updated != 2면 IllegalStateException 발생")
    void confirmMatch_UpdateFailed_ThrowsException() {
        // 두 번째 사용자 생성
        User user2 = User.builder()
                .nickname("user2")
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(1996, 1, 1))
                .region(Region.BUSAN)
                .build();
        user2 = userRepository.save(user2);
        final UUID user2Id = user2.getId();

        // 첫 번째 사용자만 큐에 등록 (WAITING 상태)
        MatchQueue queue1 = MatchQueue.builder()
                .userId(testUserId)
                .tier(testUser.getTier())
                .region(testUser.getRegion())
                .birthYear(testUser.getBirthDate().getYear())
                .gender(testUser.getGender())
                .hobbyIds(new Integer[0])
                .build();
        matchQueueRepository.save(queue1);

        // 두 번째 사용자는 큐에 없음 (또는 이미 MATCHED 상태)
        // updateStatusIf가 2를 반환하지 않을 것

        assertThatThrownBy(() -> matchService.confirmMatch(testUserId, user2Id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Match confirmation failed");
    }

    @Test
    @DisplayName("cancelMatch: 큐에 없으면 UserNotInQueueException 발생")
    void cancelMatch_NotInQueue_ThrowsException() {
        // 큐에 등록하지 않은 상태
        assertThatThrownBy(() -> matchService.cancelMatch(matchRequest))
                .isInstanceOf(UserNotInQueueException.class);
    }

    @Test
    @DisplayName("cancelMatch: 이미 MATCHED 상태면 예외 발생")
    void cancelMatch_AlreadyMatched_ThrowsException() {
        // MATCHED 상태로 큐에 등록
        MatchQueue queue = MatchQueue.builder()
                .userId(testUserId)
                .tier(testUser.getTier())
                .region(testUser.getRegion())
                .birthYear(testUser.getBirthDate().getYear())
                .gender(testUser.getGender())
                .hobbyIds(new Integer[0])
                .build();
        queue.setStatus(MatchStatus.MATCHED);
        matchQueueRepository.save(queue);

        assertThatThrownBy(() -> matchService.cancelMatch(matchRequest))
                .isInstanceOf(UserNotInQueueException.class)
                .hasMessageContaining("이미 매칭되어 취소할 수 없습니다");
    }

    @Test
    @DisplayName("cancelMatch: WAITING 상태면 정상 취소")
    void cancelMatch_Waiting_Success() {
        // WAITING 상태로 큐에 등록
        MatchQueue queue = MatchQueue.builder()
                .userId(testUserId)
                .tier(testUser.getTier())
                .region(testUser.getRegion())
                .birthYear(testUser.getBirthDate().getYear())
                .gender(testUser.getGender())
                .hobbyIds(new Integer[0])
                .build();
        matchQueueRepository.save(queue);

        // 취소 실행
        matchService.cancelMatch(matchRequest);

        // 실제 DB 상태 검증: 큐에서 삭제되었는지 확인
        Optional<MatchQueue> deleted = matchQueueRepository.findByUserId(testUserId);
        assertThat(deleted).isEmpty();
    }
}
