package io.openems.edge.simulator.evse.chargepoint;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.evse.api.chargepoint.Profile;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.simulator.evse.chargepoint.enums.PhaseSwitchState;

class SimulatorEvseChargepointImplTest {

	@Test
	void testReadOnlySinglePhasePresets() throws Exception {
		final var sut = new SimulatorEvseChargepointImpl();

		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setReadOnly(true) //
						.setVehicleConnected(true) //
						.setWiring(SINGLE_PHASE) //
						.build()) //
				.next(new TestCase("Applies read-only single-phase presets on activation") //
						.onAfterProcessImage(() -> {
							assertTrue(sut.isReadOnly());
							assertTrue(sut.getIsReadyForCharging());
							assertEquals(PhaseSwitchState.SINGLE, sut.getPhaseSwitchState());
							assertEquals(Integer.valueOf(1380), sut.getActivePowerL1().get());
							assertEquals(Integer.valueOf(0), sut.getActivePowerL2().get());
							assertEquals(Integer.valueOf(0), sut.getActivePowerL3().get());
							assertEquals(Integer.valueOf(6000), sut.getCurrentL1().get());
							assertEquals(Integer.valueOf(0), sut.getCurrentL2().get());
							assertEquals(Integer.valueOf(0), sut.getCurrentL3().get());
							assertEquals(Integer.valueOf(230000), sut.getVoltageL1().get());
							assertEquals(Integer.valueOf(0), sut.getVoltageL2().get());
							assertEquals(Integer.valueOf(0), sut.getVoltageL3().get());
							assertNull(sut.getChargePointAbilities());
						})) //
				.deactivate();
	}

	@Test
	void testReadOnlyMultiPhasePresets() throws Exception {
		final var sut = new SimulatorEvseChargepointImpl();

		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setReadOnly(true) //
						.setVehicleConnected(true) //
						.setWiring(THREE_PHASE) //
						.build()) //
				.next(new TestCase("Applies read-only three-phase presets on activation") //
						.onAfterProcessImage(() -> {
							assertTrue(sut.isReadOnly());
							assertTrue(sut.getIsReadyForCharging());
							assertEquals(PhaseSwitchState.THREE, sut.getPhaseSwitchState());
							assertEquals(Integer.valueOf(1380), sut.getActivePowerL1().get());
							assertEquals(Integer.valueOf(1380), sut.getActivePowerL2().get());
							assertEquals(Integer.valueOf(1380), sut.getActivePowerL3().get());
							assertEquals(Integer.valueOf(6000), sut.getCurrentL1().get());
							assertEquals(Integer.valueOf(6000), sut.getCurrentL2().get());
							assertEquals(Integer.valueOf(6000), sut.getCurrentL3().get());
							assertEquals(Integer.valueOf(230000), sut.getVoltageL1().get());
							assertEquals(Integer.valueOf(230000), sut.getVoltageL2().get());
							assertEquals(Integer.valueOf(230000), sut.getVoltageL3().get());
							assertNull(sut.getChargePointAbilities());
						}));
	}

	@Test
	void testApplyPhaseSwitchAndModifiedConfig() throws Exception {
		final int milliAmpere = 6000;

		final var sut = new SimulatorEvseChargepointImpl();
		final var componentTest = new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setVehicleConnected(true) //
						.setWiring(THREE_PHASE) //
						.setSupportsPhaseSwitching(true) //
						.setMinCurrent(milliAmpere) //
						.build());

		componentTest.next(new TestCase("Initial writable abilities") //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
				.output(ElectricityMeter.ChannelId.VOLTAGE, 0) //
				.output(SimulatorEvseChargepoint.ChannelId.PHASE_SWITCH_STATE, PhaseSwitchState.THREE) //
				.onAfterProcessImage(() -> {
					assertFalse(sut.isReadOnly());
					assertEquals(PhaseSwitchState.THREE, sut.getPhaseSwitchState());

					var abilities = sut.getChargePointAbilities();
					assertTrue(abilities.isEvConnected());
					assertTrue(abilities.isReadyForCharging());
					assertEquals(PhaseSwitchDirection.TO_SINGLE_PHASE, abilities.phaseSwitch().direction());
					assertEquals(THREE_PHASE, abilities.applySetPoint().phase());
					assertNotNull(abilities.phaseSwitch().oppositePhaseApplySetPoint());
					assertEquals(SINGLE_PHASE, abilities.phaseSwitch().oppositePhaseApplySetPoint().phase());
					assertEquals(1380, abilities.phaseSwitch().oppositePhaseApplySetPoint().min());
					assertEquals(3680, abilities.phaseSwitch().oppositePhaseApplySetPoint().max());
				}));

		componentTest.next(new TestCase("Apply current") //
				.onBeforeProcessImage(() -> {
					sut.apply(Profile.ChargePointActions.from(sut.getChargePointAbilities()) //
							.setApplyMinSetPoint() //
							.build());
				}) //
				.output(ElectricityMeter.ChannelId.CURRENT_L1, milliAmpere) //
				.output(ElectricityMeter.ChannelId.CURRENT_L2, milliAmpere) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, milliAmpere) //
				.output(ElectricityMeter.ChannelId.CURRENT, milliAmpere * 3));

		componentTest.next(new TestCase("Test phase switch") //
				.onBeforeProcessImage(() -> {
					sut.apply(Profile.ChargePointActions.from(sut.getChargePointAbilities()) //
							.setApplyMinSetPoint() //
							.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE) //
							.build());

					assertEquals(PhaseSwitchState.SINGLE, sut.getPhaseSwitchState());
					var abilities = sut.getChargePointAbilities();
					assertEquals(SINGLE_PHASE, abilities.applySetPoint().phase());
					assertEquals(PhaseSwitchDirection.TO_THREE_PHASE, abilities.phaseSwitch().direction());
					assertNotNull(abilities.phaseSwitch().oppositePhaseApplySetPoint());
					assertEquals(THREE_PHASE, abilities.phaseSwitch().oppositePhaseApplySetPoint().phase());
					assertEquals(4140, abilities.phaseSwitch().oppositePhaseApplySetPoint().min());
					assertEquals(11040, abilities.phaseSwitch().oppositePhaseApplySetPoint().max());
				}) //
				.output(SimulatorEvseChargepoint.ChannelId.PHASE_SWITCH_STATE, PhaseSwitchState.SINGLE) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, milliAmpere * 230 / 1000) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, milliAmpere * 230 / 1000) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
				.output(ElectricityMeter.ChannelId.CURRENT, milliAmpere) //
				.output(ElectricityMeter.ChannelId.CURRENT_L1, milliAmpere) //
				.output(ElectricityMeter.ChannelId.CURRENT_L2, 0) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, 0) //
				.output(ElectricityMeter.ChannelId.VOLTAGE, 230000) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230000) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 0) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 0));
	}
}
