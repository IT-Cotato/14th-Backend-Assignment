package com.cotato.backend14th;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// JWT_SECRET이 없는 환경에서도 컨텍스트 로딩을 확인할 수 있도록 테스트 전용 값을 지정한다.
@SpringBootTest(properties = "jwt.secret=test-only-secret-key-for-context-loading-0123456789")
class Backend14thApplicationTests {

	@Test
	void contextLoads() {
	}

}
