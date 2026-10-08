package com.app.med_support;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Disabled("Application context test requires database configuration")
@SpringBootTest
@ActiveProfiles("test")
class MedSupportApplicationTests {

	@Test
	void contextLoads() {
	}

}
