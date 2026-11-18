package com.cotato.backend14th.global.jwt;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// 로그아웃한 토큰을 서버 메모리에 보관한다. (서버를 재시작하면 목록이 사라진다)
// 토큰은 어차피 만료 시각이 지나면 쓸 수 없으므로, 만료된 토큰은 목록에서 지운다.
@Component
public class TokenBlacklist {

    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    public void add(String token, Date expiration) {
        removeExpired();
        blacklist.put(token, expiration);
    }

    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    private void removeExpired() {
        Date now = new Date();
        // map에 values() 메서드를 사용하여 -> 값만 모은 목록으로 removeIF로 값 하나를 받아와서 expiration이라는 이름을 붙이고, expiration.before함수를 사용
        // before메서드는 expiration 은 발급시간 + 1시간 이므로 expiration.before는 만료시간 전인지를 확인하여 True이면 해당 값을 제거한다.
        blacklist.values().removeIf(expiration -> expiration.before(now));
    }
}
