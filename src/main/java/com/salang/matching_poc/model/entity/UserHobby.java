package com.salang.matching_poc.model.entity;

import com.salang.matching_poc.model.entity.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_hobbies", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "hobby_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserHobby extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userHobbyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @Builder
    public UserHobby(User user, Hobby hobby) {
        this.user = user;
        this.hobby = hobby;
    }
}
