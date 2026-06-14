package fr.freetech4u;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SchoolManagementServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
