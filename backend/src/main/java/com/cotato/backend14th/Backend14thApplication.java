// Backend14thApplication.java
package com.cotato.backend14th;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling // 만료된 로그아웃 토큰 정리
@SpringBootApplication
public class Backend14thApplication {

    public static void main(String[] args) {
        SpringApplication.run(Backend14thApplication.class, args);
    }
}
