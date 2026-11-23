package com.cotato.backend14th.domain.user.entity;

import com.cotato.backend14th.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(nullable = false, length = 60)
    private String password;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 10)
    private String nickname;
    @Column(nullable = false)
    private int generation;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 2)
    private Part part;

    public User(String email, String password, String name, String nickname, int generation, Part part) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.generation = generation;
        this.part = part;
    }
}
