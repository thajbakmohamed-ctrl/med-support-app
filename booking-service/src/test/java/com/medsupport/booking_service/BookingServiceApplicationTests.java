
package com.medsupport.booking_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.EnabledIf;

@SpringBootTest
@EnabledIf(
		expression = "#{systemEnvironment['RUN_INTEGRATION_TESTS'] == 'true'}",
		loadContext = false
)
class BookingServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}
