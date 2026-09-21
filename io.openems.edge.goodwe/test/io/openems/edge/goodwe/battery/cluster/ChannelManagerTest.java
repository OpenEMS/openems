package io.openems.edge.goodwe.battery.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.openems.common.types.Tuple2;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.common.channel.value.Value;

class ChannelManagerTest {

	@Test
	void testCalculateSocSameCapacity() {
		final var battery0 = new DummyBattery("battery0");
		final var battery1 = new DummyBattery("battery1");
		final var soc = ChannelManager.calculateSoc(List.of(battery0, battery1), Map.of(//
				battery0,
				Tuple2.of(new Value<>(battery0.getSocChannel(), 30),
						new Value<>(battery0.getCapacityChannel(), 10_000)), //
				battery1,
				Tuple2.of(new Value<>(battery0.getSocChannel(), 70), new Value<>(battery0.getCapacityChannel(), 10_000)) //
		));

		assertEquals(50, soc);
	}

	@Test
	void testCalculateSocDifferentCapacity() {
		final var battery0 = new DummyBattery("battery0");
		final var battery1 = new DummyBattery("battery1");
		final var soc = ChannelManager.calculateSoc(List.of(battery0, battery1), Map.of(//
				battery0,
				Tuple2.of(new Value<>(battery0.getSocChannel(), 30),
						new Value<>(battery0.getCapacityChannel(), 10_000)), //
				battery1,
				Tuple2.of(new Value<>(battery0.getSocChannel(), 70), new Value<>(battery0.getCapacityChannel(), 20_000)) //
		));

		assertEquals(56, soc);
	}

	@Test
	void testCalculateSocMissingSoc() {
		final var battery0 = new DummyBattery("battery0");
		final var battery1 = new DummyBattery("battery1");
		final var soc = ChannelManager.calculateSoc(List.of(battery0, battery1), Map.of(//
				battery0,
				Tuple2.of(new Value<>(battery0.getSocChannel(), null),
						new Value<>(battery0.getCapacityChannel(), 10_000)), //
				battery1,
				Tuple2.of(new Value<>(battery0.getSocChannel(), 70), new Value<>(battery0.getCapacityChannel(), 20_000)) //
		));

		assertNull(soc);
	}

	@Test
	void testCalculateSocMissingCapacity() {
		final var battery0 = new DummyBattery("battery0");
		final var battery1 = new DummyBattery("battery1");
		final var soc = ChannelManager.calculateSoc(List.of(battery0, battery1), Map.of(//
				battery0,
				Tuple2.of(new Value<>(battery0.getSocChannel(), 30),
						new Value<>(battery0.getCapacityChannel(), 10_000)), //
				battery1,
				Tuple2.of(new Value<>(battery0.getSocChannel(), 70), new Value<>(battery0.getCapacityChannel(), null)) //
		));

		assertNull(soc);
	}

	@Test
	void testCalculateSocMissingBattery() {
		final var battery0 = new DummyBattery("battery0");
		final var battery1 = new DummyBattery("battery1");
		final var soc = ChannelManager.calculateSoc(List.of(battery0, battery1), Map.of(//
				battery0,
				Tuple2.of(new Value<>(battery0.getSocChannel(), 30), new Value<>(battery0.getCapacityChannel(), 10_000)) //
		));

		assertNull(soc);
	}

}