package com.cotato.backend14th.domain.user.repository;

import com.cotato.backend14th.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // emali을 기준으로 user를 찾을때 없을 수도 있기에 Optional 타입으로 빈 상자를 반환할 수 있도록 한다.
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}
