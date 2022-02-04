package io.openems.common.timedata;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Assert;
import org.junit.Test;

public class CommonTimedataServiceTest {

	@Test
	public void testCalculateResolution() {

		// 1 Month
		var seconds = CommonTimedataService.calculateResolution(//
				ZonedDateTime.of(2019, 7, 1, 0, 0, 0, 0, ZoneId.of("UTC")), //
				ZonedDateTime.of(2019, 7, 31, 0, 0, 0, 0, ZoneId.of("UTC")));
		Assert.assertEquals(4 * 60 * 60/* 4 Hours */, seconds);
	}

}
