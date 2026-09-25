package com.cotato.backend14th;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class Backend14thApplication {

  public static void main(String[] args) {
    SpringApplication.run(Backend14thApplication.class, args);
  }

}
