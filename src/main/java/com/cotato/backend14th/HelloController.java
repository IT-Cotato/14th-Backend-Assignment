package com.cotato.backend14th;

import org.springframework.web.bind.annotation.GetMapping;

public class HelloController {

    @GetMapping("/hello")
    public String hello(){
        return "안녕하세요, 윤현진입니다!";
    }
}
