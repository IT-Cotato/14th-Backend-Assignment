package com.cotato.backend14th.domain.user.entity;

import com.cotato.backend14th.global.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // BCrypt로 암호화한 값만 저장해요
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private Integer generation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Part part;

    @Builder
    private User(String email, String password, String name, String nickname, Integer generation, Part part) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.generation = generation;
        this.part = part;
    }
}
