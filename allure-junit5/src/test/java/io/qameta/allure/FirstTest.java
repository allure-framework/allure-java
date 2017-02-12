package io.qameta.allure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FirstTest {

	@Test
	@DisplayName("Test 1")
	void myFirstTest(TestInfo testInfo) {
		assertEquals(2, 2, "1 + 1 should equal 2");
		assertEquals("Test 1", testInfo.getDisplayName(), () -> "TestInfo is injected correctly");
	}

}
