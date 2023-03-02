package io.openems.edge.controller.api.backend;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.Lists;

import io.openems.common.types.OpenemsType;

public class SendAggregatedChannelValuesWorkerTest {

	@Test
	public void testAggregateNaturalCumulated() {
		final var value = SendAggregatedChannelValuesWorker.aggregate(true, OpenemsType.LONG, //
				Lists.newArrayList(2, 4));
		assertEquals(4, value.getAsLong());
	}

	@Test
	public void testAggregateNaturalNotCumulated() {
		final var value = SendAggregatedChannelValuesWorker.aggregate(false, OpenemsType.LONG, //
				Lists.newArrayList(2, 7));
		assertEquals(5, value.getAsLong());
	}

	@Test
	public void testAggregateFloatingCumulated() {
		final var value = SendAggregatedChannelValuesWorker.aggregate(true, OpenemsType.DOUBLE, //
				Lists.newArrayList(2.23, 4.75));
		assertEquals(4.75, value.getAsDouble(), 0);
	}

	@Test
	public void testAggregateFloatingNotCumulated() {
		final var value = SendAggregatedChannelValuesWorker.aggregate(false, OpenemsType.DOUBLE, //
				Lists.newArrayList(2.9, 7.1));
		assertEquals(5, value.getAsDouble(), 0);
	}

	@Test
	public void testAggregateStringCumulated() {
		final var value = SendAggregatedChannelValuesWorker.aggregate(true, OpenemsType.STRING, //
				Lists.newArrayList("a", "b", "c", "d", "e"));
		assertEquals("a", value.getAsString());
	}

	@Test
	public void testAggregateStringNotCumulated() {
		final var value = SendAggregatedChannelValuesWorker.aggregate(false, OpenemsType.STRING, //
				Lists.newArrayList("a", "b", "c", "d", "e"));
		assertEquals("a", value.getAsString());
	}

}
