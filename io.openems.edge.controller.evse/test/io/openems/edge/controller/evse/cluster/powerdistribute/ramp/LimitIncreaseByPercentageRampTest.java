package io.openems.edge.controller.evse.cluster.powerdistribute.ramp;

import static io.openems.common.test.TestUtils.createDummyClock;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.type.Phase;
import io.openems.edge.controller.evse.cluster.powerdistribute.PowerDistributionEntry;
import io.openems.edge.controller.evse.cluster.powerdistribute.PowerDistributionKey;
import io.openems.edge.controller.evse.single.Mode;
import io.openems.edge.evse.api.common.ApplySetPoint;

class LimitIncreaseByPercentageRampTest {

	private static final ApplySetPoint.Ability.Watt SET_POINT_ABILITY = new ApplySetPoint.Ability.Watt(
			Phase.SingleOrThreePhase.THREE_PHASE, 6 * 230 * 3, 16 * 230 * 3);

	private static final float MAX_PERCENTAGE_CHANGE_PER_SECOND = 0.03f;

	@Test
	void testConsumptionLimit() {
		final var clock = createDummyClock();
		final var testKey = new PowerDistributionKey.Evse("test");
		final var testEntry = new PowerDistributionEntry(testKey, PowerDistributionEntry.Status.ACTIVE, Mode.SURPLUS, 0,
				SET_POINT_ABILITY);
		testEntry.setSetPointInWatt(SET_POINT_ABILITY.max());

		var ramp = new LimitIncreaseByPercentageRamp(MAX_PERCENTAGE_CHANGE_PER_SECOND, clock);
		var expectedPower = SET_POINT_ABILITY.min();

		assertEquals(expectedPower, ramp.getConsumptionLimitInWatt(testEntry));
		ramp.setLastSetPoint(testKey, expectedPower);

		clock.leap(1000, MILLIS);
		expectedPower += (int) Math.ceil(expectedPower * MAX_PERCENTAGE_CHANGE_PER_SECOND);
		assertEquals(expectedPower, ramp.getConsumptionLimitInWatt(testEntry));
		ramp.setLastSetPoint(testKey, expectedPower);

		clock.leap(1500, MILLIS);
		expectedPower += (int) Math.ceil(expectedPower * MAX_PERCENTAGE_CHANGE_PER_SECOND * 1.5);
		assertEquals(expectedPower, ramp.getConsumptionLimitInWatt(testEntry));

		// Lowering should be possible without ramp limitations
		testEntry.setSetPointInWatt(4200);
		assertEquals(4200, ramp.getConsumptionLimitInWatt(testEntry));

		testEntry.setSetPointInWatt(0);
		assertEquals(0, ramp.getConsumptionLimitInWatt(testEntry));
	}

	@Test
	void testStore() {
		final var clock = createDummyClock();
		final var ramp = new LimitIncreaseByPercentageRamp(MAX_PERCENTAGE_CHANGE_PER_SECOND, clock);
		final var testKey = new PowerDistributionKey.Evse("test");

		assertNull(ramp.getLastSetPointInWatt(testKey));
		ramp.setLastSetPoint(testKey, 5000);
		assertEquals(5000, ramp.getLastSetPointInWatt(testKey).setPointInWatt());
		assertEquals(clock.instant(), ramp.getLastSetPointInWatt(testKey).time());

		ramp.clearLastSetPoints();
		assertNull(ramp.getLastSetPointInWatt(testKey));
	}
}
