package io.openems.edge.evse.api.chargepoint;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.common.ApplySetPoint.convertAmpereToWatt;
import static io.openems.edge.evse.api.common.ApplySetPoint.convertMilliAmpereToWatt;
import static io.openems.edge.evse.api.common.ApplySetPoint.convertWattToAmpere;
import static io.openems.edge.evse.api.common.ApplySetPoint.convertWattToMilliAmpere;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonNull;

import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.evse.api.common.ApplySetPoint;

class ProfileTest {

	@Test
	void testConvert() {
		assertEquals(1380, convertAmpereToWatt(SINGLE_PHASE, 6));
		assertEquals(1380, convertMilliAmpereToWatt(SINGLE_PHASE, 6000));

		assertEquals(4140, convertAmpereToWatt(THREE_PHASE, 6));
		assertEquals(4140, convertMilliAmpereToWatt(THREE_PHASE, 6000));

		assertEquals(3680, convertAmpereToWatt(SINGLE_PHASE, 16));
		assertEquals(3680, convertMilliAmpereToWatt(SINGLE_PHASE, 16000));

		assertEquals(11040, convertAmpereToWatt(THREE_PHASE, 16));
		assertEquals(11040, convertMilliAmpereToWatt(THREE_PHASE, 16000));

		assertEquals(7360, convertAmpereToWatt(SINGLE_PHASE, 32));
		assertEquals(7360, convertMilliAmpereToWatt(SINGLE_PHASE, 32000));

		assertEquals(22080, convertAmpereToWatt(THREE_PHASE, 32));
		assertEquals(22080, convertMilliAmpereToWatt(THREE_PHASE, 32000));

		assertEquals(17891, convertWattToMilliAmpere(THREE_PHASE, 12345));
		assertEquals(17, convertWattToAmpere(THREE_PHASE, 12345));

		assertEquals(5365, convertWattToMilliAmpere(SINGLE_PHASE, 1234));
		assertEquals(5, convertWattToAmpere(SINGLE_PHASE, 1234));
	}

	@Test
	void testChargePointAbilitiesSerializerWithPhaseSwitch() {
		var original = ChargePointAbilities.create()
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(THREE_PHASE, 6, 32))
				.setIsEvConnected(true)
				.setIsReadyForCharging(true)
				.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE)
				.build();
		var serializer = ChargePointAbilities.serializer();

		var deserialized = serializer.deserialize(serializer.serialize(original).getAsJsonObject());

		assertEquals(original.isEvConnected(), deserialized.isEvConnected());
		assertEquals(original.isReadyForCharging(), deserialized.isReadyForCharging());
		assertEquals(original.phaseSwitch().direction(), deserialized.phaseSwitch().direction());
		assertEquals(original.applySetPoint(), deserialized.applySetPoint());
	}

	@Test
	void testChargePointAbilitiesSerializerWithoutPhaseSwitch() {
		var original = ChargePointAbilities.create()
				.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 3680))
				.setIsEvConnected(false)
				.setIsReadyForCharging(false)
				.build();
		var serializer = ChargePointAbilities.serializer();
		var serialized = serializer.serialize(original).getAsJsonObject();
		var deserialized = serializer.deserialize(serialized);

		assertEquals(original, deserialized);
		assertNull(deserialized.phaseSwitch());
	}

	@Test
	void chargePointAbilities_serializer_null_returnsJsonNull() {
		assertEquals(JsonNull.INSTANCE, ChargePointAbilities.serializer().serialize(null));
	}

}
