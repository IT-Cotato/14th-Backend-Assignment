package com.cotato.backend14th.domain.user.entity;

import com.cotato.backend14th.global.common.entity.BaseEntity;
import jakarta.persistence.*;
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

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(nullable = false)
    private int generation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Part part;

    // 생성자 함수, 빌더 어노테이션을 통해서 객체 생성시 값을 이름과 함께 삽입 가능
    @Builder
    public User(String email, String password, String name, String nickname, int generation, Part part){
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.generation = generation;
        this.part = part;
    }
}
