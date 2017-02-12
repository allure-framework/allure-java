package io.qameta.allure;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecondTest {

	@Test
	@Disabled
	void test2() {
		assertEquals(2, 1, "2 is not equal to 1");
	}

	@Test
	void test3() throws InterruptedException {
	}

	@Test
	void failingTest() {
		throw new RuntimeException("Failure");
	}
}
