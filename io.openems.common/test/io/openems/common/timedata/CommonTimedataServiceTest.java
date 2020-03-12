package io.openems.common.timedata;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

public class CommonTimedataServiceTest {

	@Test
	public void testCalculateResolution() {

		// 1 Month
		int seconds = CommonTimedataService.calculateResolution(//
				ZonedDateTime.of(2019, 7, 1, 0, 0, 0, 0, ZoneId.of("UTC")), //
				ZonedDateTime.of(2019, 7, 31, 0, 0, 0, 0, ZoneId.of("UTC")));
		assertEquals(4 * 60 * 60/* 4 Hours */, seconds);
	}

}
