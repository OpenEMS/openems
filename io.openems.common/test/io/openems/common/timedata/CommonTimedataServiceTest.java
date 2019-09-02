package io.openems.common.timedata;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.SortedMap;

import org.junit.Test;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;

public class CommonTimedataServiceTest {

	private static CommonTimedataService getDummyInstance() {
		return new CommonTimedataService() {

			@Override
			public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
					ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
				return null;
			}

			@Override
			public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
					ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, int resolution)
					throws OpenemsNamedException {
				return null;
			}
		};
	}

	@Test
	public void testCalculateResolution() {
		CommonTimedataService s = CommonTimedataServiceTest.getDummyInstance();

		// 1 Month
		int seconds = s.calculateResolution(//
				ZonedDateTime.of(2019, 7, 1, 0, 0, 0, 0, ZoneId.of("UTC")), //
				ZonedDateTime.of(2019, 7, 31, 0, 0, 0, 0, ZoneId.of("UTC")));
		assertEquals(4 * 60 * 60/* 4 Hours */, seconds);
	}

}
