package com.schoolsaas;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("Requires full DB setup - skipped for Phase 6 unit test coverage")
@SpringBootTest
@ActiveProfiles("test")
class SchoolSaasApplicationTests {

	@Test
	void contextLoads() {
	}

}
