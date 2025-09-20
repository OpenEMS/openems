package io.openems.backend.timedata.aggregatedinflux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.Lists;

import io.openems.backend.timedata.aggregatedinflux.AllowedChannels.DataType;

public class AllowedChannelsTest {

	@Test
	public void testInitialization() {
		// to avoid initialization errors for duplicated channels
		final var type = AllowedChannels.getChannelType("_sum/ConsumptionActiveEnergy");
		assertEquals(AllowedChannels.ChannelType.MAX, type);
	}

	@Test
	public void testMultiChannels() {
		final var mutliChannels = AllowedChannels.multiChannels("component", 0, 5, "channel", DataType.DOUBLE);

		final var expectedChannels = Lists.newArrayList(//
				"component0/channel", //
				"component1/channel", //
				"component2/channel", //
				"component3/channel", //
				"component4/channel" //
		);
		for (var entry : mutliChannels) {
			assertTrue(expectedChannels.remove(entry.getKey()));
		}
		assertTrue(expectedChannels.isEmpty());
	}

	@Test
	public void testMultiChannelsWithChannelNumber() {
		final var mutliChannels = AllowedChannels.multiChannels("component", 0, 5, "channel", 1, 3, DataType.DOUBLE);

		final var expectedChannels = Lists.newArrayList(//
				"component0/channel1", //
				"component1/channel1", //
				"component2/channel1", //
				"component3/channel1", //
				"component4/channel1", //
				"component0/channel2", //
				"component1/channel2", //
				"component2/channel2", //
				"component3/channel2", //
				"component4/channel2" //
		);
		for (var entry : mutliChannels) {
			assertTrue(entry.getKey(), expectedChannels.remove(entry.getKey()));
		}
		assertTrue(expectedChannels.isEmpty());
	}

}
