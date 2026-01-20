package com.salang.matching_poc.model.entity;

import com.salang.matching_poc.model.entity.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hobbies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hobby extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 50)
    private String subcategory;

    @Column(nullable = false, length = 50)
    private String name;

    @Builder
    public Hobby(String category, String subcategory, String name) {
        this.category = category;
        this.subcategory = subcategory;
        this.name = name;
    }
}
