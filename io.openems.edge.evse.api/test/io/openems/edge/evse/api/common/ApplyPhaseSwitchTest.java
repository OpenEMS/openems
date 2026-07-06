package io.openems.edge.evse.api.common;

import static io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection.TO_SINGLE_PHASE;
import static io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection.TO_THREE_PHASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonNull;

class ApplyPhaseSwitchTest {

	@Test
	void testManualSerializeDeserialize() {
		var sut = new ApplyPhaseSwitch(TO_SINGLE_PHASE, new ApplyPhaseSwitch.PhaseSwitchAbility.Manual());
		var json = ApplyPhaseSwitch.serializer().serialize(sut).getAsJsonObject();

		assertEquals("TO_SINGLE_PHASE", json.get("direction").getAsString());
		assertEquals("Manual", json.getAsJsonObject("phaseSwitchAbility").get("class").getAsString());

		assertEquals(sut, ApplyPhaseSwitch.serializer().deserialize(json));
	}

	@Test
	void testInternalSerializeDeserialize() {
		var sut = new ApplyPhaseSwitch(TO_THREE_PHASE, new ApplyPhaseSwitch.PhaseSwitchAbility.Internal());
		var json = ApplyPhaseSwitch.serializer().serialize(sut).getAsJsonObject();

		assertEquals("TO_THREE_PHASE", json.get("direction").getAsString());
		assertEquals("Internal", json.getAsJsonObject("phaseSwitchAbility").get("class").getAsString());

		var deserialized = ApplyPhaseSwitch.serializer().deserialize(json);
		assertEquals(sut, deserialized);
		assertInstanceOf(ApplyPhaseSwitch.PhaseSwitchAbility.Internal.class, deserialized.ability());
	}

	@Test
	void testSerializeNull() {
		assertEquals(JsonNull.INSTANCE, ApplyPhaseSwitch.serializer().serialize(null));
	}
}
