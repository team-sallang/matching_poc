package com.salang.matching_poc.model.entity;

import com.salang.matching_poc.model.entity.base.BaseTimeEntity;
import com.salang.matching_poc.model.enums.Gender;
import com.salang.matching_poc.model.enums.Region;
import com.salang.matching_poc.model.enums.Tier;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  @Column(length = 50, unique = true, nullable = false)
  private String nickname;

  @Enumerated(EnumType.STRING)
  @Column(length = 10, nullable = false)
  private Gender gender;

  @Column(nullable = false)
  private LocalDate birthDate;

  @Enumerated(EnumType.STRING)
  @Column(length = 50, nullable = false)
  private Region region;

  @Column(nullable = false)
  private Integer totalScore;

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  private Tier tier;

  @Builder
  public User(String nickname, Gender gender, LocalDate birthDate, Region region) {
    this.nickname = nickname;
    this.gender = gender;
    this.birthDate = birthDate;
    this.region = region;
    this.totalScore = 0;
    this.tier = Tier.SPROUT;
  }
}
