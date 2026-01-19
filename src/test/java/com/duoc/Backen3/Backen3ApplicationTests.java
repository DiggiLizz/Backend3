package com.duoc.Backen3;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.batch.job.enabled=false")
class Backen3ApplicationTests {

	@Test
	void contextLoads() {
	}

}
