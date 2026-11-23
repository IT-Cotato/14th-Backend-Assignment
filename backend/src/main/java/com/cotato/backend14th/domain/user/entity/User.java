package com.cotato.backend14th.domain.user.entity;

import com.cotato.backend14th.global.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 60) // BCrypt 해시 길이
    private String password;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(nullable = false)
    private Integer generation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
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
