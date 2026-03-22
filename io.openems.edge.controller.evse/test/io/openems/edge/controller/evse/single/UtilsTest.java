package io.openems.edge.controller.evse.single;

import static io.openems.edge.controller.evse.TestUtils.createSingleCtrl;
import static io.openems.edge.evse.api.common.ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.api.common.ApplySetPoint;

public class UtilsTest {

	private static final String CTRL = "ctrlEvseSingle0";

	@Test
	public void testChargePointThreeVehicleSingle() {
		var ctrl = createSingleCtrl() //
				.setId(CTRL) //
				.setChargePointAbilities(b -> b//
						.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SingleOrThreePhase.THREE_PHASE, 6, 16)) //
						.setPhaseSwitch(null) //
						.setIsEvConnected(false) //
						.setIsReadyForCharging(false)) //
				.setElectricVehicleAbilities(b -> b//
						.setSinglePhaseLimitInWatt(1380, 7360) //
						.setThreePhaseLimit(EMPTY_APPLY_SET_POINT_ABILITY)) //
				.build();
		var sp = ctrl.getParams().combinedAbilities().applySetPoint();
		assertEquals(SingleOrThreePhase.SINGLE_PHASE, sp.phase());
		assertEquals(1380, sp.min());
		assertEquals(3680, sp.max());
		assertEquals(230, sp.step());
	}

	@Test
	public void testChargePointSingleVehicleSingle() {
		var ctrl = createSingleCtrl() //
				.setId(CTRL) //
				.setChargePointAbilities(b -> b//
						.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SingleOrThreePhase.SINGLE_PHASE, 6, 16)) //
						.setPhaseSwitch(null) //
						.setIsEvConnected(false) //
						.setIsReadyForCharging(false)) //
				.setElectricVehicleAbilities(b -> b//
						.setSinglePhaseLimitInWatt(1380, 7360)) //
				.build();
		var sp = ctrl.getParams().combinedAbilities().applySetPoint();
		assertEquals(SingleOrThreePhase.SINGLE_PHASE, sp.phase());
		assertEquals(1380, sp.min());
		assertEquals(3680, sp.max());
		assertEquals(230, sp.step());
	}

	@Test
	public void testChargePointThreeVehicleThree() {
		var ctrl = createSingleCtrl() //
				.setId(CTRL) //
				.setChargePointAbilities(b -> b//
						.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SingleOrThreePhase.THREE_PHASE, 6, 32)) //
						.setPhaseSwitch(null) //
						.setIsEvConnected(false) //
						.setIsReadyForCharging(false)) //
				.setElectricVehicleAbilities(b -> b//
						.setSinglePhaseLimitInWatt(1380, 7360) //
						.setThreePhaseLimitInWatt(4140, 11040)) //
				.build();
		var sp = ctrl.getParams().combinedAbilities().applySetPoint();
		assertEquals(SingleOrThreePhase.THREE_PHASE, sp.phase());
		assertEquals(4140, sp.min());
		assertEquals(11040, sp.max());
		assertEquals(690, sp.step());
	}

	@Test
	public void testChargePointSingleVehicleThree() {
		var ctrl = createSingleCtrl() //
				.setId(CTRL) //
				.setChargePointAbilities(b -> b//
						.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SingleOrThreePhase.SINGLE_PHASE, 6, 32)) //
						.setPhaseSwitch(null) //
						.setIsEvConnected(false) //
						.setIsReadyForCharging(false)) //
				.setElectricVehicleAbilities(b -> b//
						.setSinglePhaseLimit(EMPTY_APPLY_SET_POINT_ABILITY) //
						.setThreePhaseLimitInWatt(4140, 11040)) //
				.build();
		var sp = ctrl.getParams().combinedAbilities().applySetPoint();
		assertEquals(SingleOrThreePhase.SINGLE_PHASE, sp.phase());
		assertEquals(1380, sp.min());
		assertEquals(3680, sp.max());
		assertEquals(230, sp.step());
	}
}
