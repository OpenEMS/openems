package io.openems.edge.evse.electricvehicle.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities;

public class EvseElectricVehicleGenericImplTest {

	@Test
	public void test() throws Exception {
		{
			var eva = getElectricVehicleAbilities(64000, 1000, 3000, 2000, 4000);
			assertEquals(64000, eva.capacity());
			assertEquals(1000, eva.singlePhaseLimit().min());
			assertEquals(3000, eva.singlePhaseLimit().max());
			assertEquals(2000, eva.threePhaseLimit().min());
			assertEquals(4000, eva.threePhaseLimit().max());
		}
		{
			var eva = getElectricVehicleAbilities(64000, 1000, 3000, 0, 0);
			assertEquals(1000, eva.singlePhaseLimit().min());
			assertEquals(3000, eva.singlePhaseLimit().max());
			assertEquals(ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY, eva.threePhaseLimit());
		}
		{
			var eva = getElectricVehicleAbilities(0, 0, 0, 2000, 4000);
			assertEquals(ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY, eva.singlePhaseLimit());
			assertEquals(2000, eva.threePhaseLimit().min());
			assertEquals(4000, eva.threePhaseLimit().max());
		}
		{
			var eva = getElectricVehicleAbilities(0, 1, 0, 1, 0);
			assertEquals(ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY, eva.singlePhaseLimit());
			assertEquals(ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY, eva.threePhaseLimit());
		}
	}

	private static ElectricVehicleAbilities getElectricVehicleAbilities(int capacity, int minPowerSinglePhase,
			int maxPowerSinglePhase, int minPowerThreePhase, int maxPowerThreePhase)
			throws OpenemsException, Exception {
		final var sut = new EvseElectricVehicleGenericImpl();
		new ComponentTest(sut) //
				.activate(MyConfig.create() //
						.setId("evseElectricVehicle0") //
						.setCapacity(capacity) //
						.setMinPowerSinglePhase(minPowerSinglePhase) //
						.setMaxPowerSinglePhase(maxPowerSinglePhase) //
						.setMinPowerThreePhase(minPowerThreePhase) //
						.setMaxPowerThreePhase(maxPowerThreePhase) //
						.build());
		return sut.getElectricVehicleAbilities();
	}

}
