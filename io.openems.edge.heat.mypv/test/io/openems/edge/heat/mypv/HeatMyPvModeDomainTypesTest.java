package io.openems.edge.heat.mypv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HeatMyPvModeDomainTypesTest {

	@Test
	void testChannelModeFromModeMapping() {
		assertEquals(ChannelMode.OFF, ChannelMode.fromMode(Mode.OFF));
		assertEquals(ChannelMode.FAST_HEAT, ChannelMode.fromMode(Mode.FAST_HEAT));
		assertEquals(ChannelMode.SURPLUS, ChannelMode.fromMode(Mode.SURPLUS));
	}

	@Test
	void testChannelModeValues() {
		assertEquals(-1, ChannelMode.UNDEFINED.getValue());
		assertEquals("UNDEFINED", ChannelMode.UNDEFINED.getName());
		assertEquals(1, ChannelMode.OFF.getValue());
		assertEquals("OFF", ChannelMode.OFF.getName());
		assertEquals(2, ChannelMode.FAST_HEAT.getValue());
		assertEquals("FAST_HEAT", ChannelMode.FAST_HEAT.getName());
		assertEquals(3, ChannelMode.SURPLUS.getValue());
		assertEquals("SURPLUS", ChannelMode.SURPLUS.getName());
		assertEquals(ChannelMode.UNDEFINED, ChannelMode.OFF.getUndefined());
	}

	@Test
	void testHeatMyPvPayloadSerializerRoundTrip() {
		var serializer = HeatMyPvPayload.serializer();
		var sut = new HeatMyPvPayload(Mode.FAST_HEAT);
		var json = serializer.serialize(sut);

		assertEquals("{\"mode\":\"FAST_HEAT\"}", json.toString());
		assertEquals(sut, serializer.deserialize(json));
	}
}
