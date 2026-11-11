package io.openems.shared.influxdb.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;

/**
 * Security regression tests: {@link QueryChannels#of(Set)} is the single
 * validation gate. A channel that could break out of an InfluxQL identifier
 * must never appear in the resulting {@link QueryChannels} instance.
 */
public class QueryChannelsTest {

	@Test
	public void testAcceptsValidChannels() {
		var result = QueryChannels.of(Set.of(//
				new ChannelAddress("_sum", "EssSoc"), //
				new ChannelAddress("ess0", "ActivePowerL1"), //
				new ChannelAddress("ctrlBalancing0", "State"), //
				new ChannelAddress("_meta", "Version")));
		assertEquals(4, result.stream().count());
	}

	@Test
	public void testDropsQuoteInjection() {
		var safe = new ChannelAddress("_sum", "EssSoc");
		var result = QueryChannels.of(Set.of(safe,
				new ChannelAddress("_sum", "EssActivePower\") AS x FROM data WHERE edge='2';--")));
		assertTrue(result.stream().toList().contains(safe));
		assertEquals(1, result.stream().count());
	}

	@Test
	public void testDropsSemicolon() {
		var safe = new ChannelAddress("_sum", "EssSoc");
		var result = QueryChannels.of(Set.of(safe, new ChannelAddress("_sum", "Ess;DELETE")));
		assertTrue(result.stream().toList().contains(safe));
		assertEquals(1, result.stream().count());
	}

	@Test
	public void testDropsParenthesis() {
		var safe = new ChannelAddress("_sum", "EssSoc");
		var result = QueryChannels.of(Set.of(safe, new ChannelAddress("_sum", "Ess)Power")));
		assertTrue(result.stream().toList().contains(safe));
		assertEquals(1, result.stream().count());
	}

	@Test
	public void testDropsSpace() {
		var safe = new ChannelAddress("_sum", "EssSoc");
		var result = QueryChannels.of(Set.of(safe, new ChannelAddress("_sum", "Ess Power")));
		assertTrue(result.stream().toList().contains(safe));
		assertEquals(1, result.stream().count());
	}

	@Test
	public void testDropsNul() {
		var safe = new ChannelAddress("_sum", "EssSoc");
		var result = QueryChannels.of(Set.of(safe, new ChannelAddress("_sum", "Ess" + ((char) 0) + "Power")));
		assertTrue(result.stream().toList().contains(safe));
		assertEquals(1, result.stream().count());
	}

	@Test
	public void testDropsWildcards() {
		var safe = new ChannelAddress("_sum", "EssSoc");
		var result = QueryChannels.of(Set.of(safe,
				new ChannelAddress("meter*", "ActivePower"),
				new ChannelAddress("*", "*Power")));
		assertTrue(result.stream().toList().contains(safe));
		assertEquals(1, result.stream().count());
	}

	@Test
	public void testDropsToStringDivergence() throws OpenemsNamedException {
		// fromString keeps the raw input as toString(); the extra segment must be
		// rejected even though the parsed component-/channel-id look clean
		var safe = new ChannelAddress("_sum", "EssSoc");
		final var divergent = ChannelAddress.fromString("_sum/EssSoc/x\"inject");
		var result = QueryChannels.of(Set.of(safe, divergent));
		assertTrue(result.stream().toList().contains(safe));
		assertEquals(1, result.stream().count());
	}

	@Test
	public void testAllUnsafeReturnsEmpty() {
		var result = QueryChannels.of(Set.of(new ChannelAddress("_sum", "Bad;Name")));
		assertTrue(result.isEmpty());
	}

	@Test
	public void testAcceptsFromStringBuiltAddresses() throws OpenemsNamedException {
		// Exercise the accept path of clause 3 (toString == componentId + "/" + channelId)
		// with fromString-built addresses, which is the construction path every real
		// JSON-RPC request uses.
		var fromString = ChannelAddress.fromString("_sum/EssActivePower");
		var result = QueryChannels.of(Set.of(fromString));
		assertEquals(1, result.stream().count());
		assertTrue(result.stream().toList().contains(fromString));
	}
}
