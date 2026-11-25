package io.openems.common.utils;

import static io.openems.common.utils.GathererUtils.filterIsInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class GathererUtilsTest {

	@Test
	void testFilterIsInstance() {
		final var result = Stream.<Object>of("not an integer", 1, null, 2.0, 3) //
				.gather(filterIsInstance(Integer.class)) //
				.toList();

		assertEquals(List.of(1, 3), result);
	}

}
