package io.openems.edge.evse.chargepoint.mennekes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.channel.Level;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.bender.EvseChargePointBender;
import io.openems.edge.evse.chargepoint.bender.OcppState;
import io.openems.edge.evse.chargepoint.bender.VehicleState;
import io.openems.edge.evse.chargepoint.mennekes.common.DeviceID;
import io.openems.edge.evse.chargepoint.mennekes.common.LogVerbosity;
import io.openems.edge.evse.chargepoint.mennekes.common.Mennekes;
import io.openems.edge.evse.chargepoint.mennekes.common.MennekesTestFixtures;
import io.openems.edge.evse.chargepoint.mennekes.enums.PhaseSwitchMode;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;

class EvseChargePointMennekesTest {

	private EvseMennekesImpl mennekes;
	private ComponentTest test;

	@BeforeEach
	void setup() throws Exception {
		this.mennekes = new EvseMennekesImpl();
		this.test = new ComponentTest(this.mennekes) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", MennekesTestFixtures.createMennekesModbusBridge()) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.setReadOnly(false) //
						.setWiring(SingleOrThreePhase.THREE_PHASE) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build())
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase());
	}

	@Test
	void test() throws Exception {
		this.test.next(new TestCase()//
				.activateStrictMode()//
				.output(OpenemsComponent.ChannelId.STATE, Level.WARNING)//
				.output(Mennekes.ChannelId.SET_CURRENT_LIMIT, null) // WRITE_ONLY Channel
				.output(Mennekes.ChannelId.SET_POWER_LIMIT, null)//
				.output(Mennekes.ChannelId.HEMS_MIN_POWER, 4140) //
				.output(Mennekes.ChannelId.HEMS_MAX_POWER, 11040) //
				.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false)//

				.output(EvseChargePointBender.ChannelId.VEHICLE_STATE, VehicleState.STATE_C) //
				.output(Mennekes.ChannelId.PHASE_SWITCH_MODE, PhaseSwitchMode.DYNAMIC_PHASE_SWITCH) //
				.output(Mennekes.ChannelId.PHASE_SWITCH_PAUSE, 30) //
				.output(Mennekes.ChannelId.PHASE_SWITCH_RUNNING, true) //
				.output(Mennekes.ChannelId.EMS_CURRENT_LIMIT, 16) //

				.output(EvseChargePointBender.ChannelId.FIRMWARE_VERSION, "1.5.22") //
				.output(EvseChargePointBender.ChannelId.FIRMWARE_OUTDATED, false) //
				.output(EvseChargePointBender.ChannelId.OCPP_CP_STATUS, OcppState.CHARGING) //

				.output(EvseChargePointBender.ChannelId.ERR_RCMB_TRIGGERED, false) //
				.output(EvseChargePointBender.ChannelId.ERR_VEHICLE_STATE_E, false) //
				.output(EvseChargePointBender.ChannelId.ERR_MODE3_DIODE_CHECK, false) //
				.output(EvseChargePointBender.ChannelId.ERR_MCB_TYPE2_TRIGGERED, false) //
				.output(EvseChargePointBender.ChannelId.ERR_MCB_SCHUKO_TRIGGERED, false) //
				.output(EvseChargePointBender.ChannelId.ERR_RCD_TRIGGERED, false) //
				.output(EvseChargePointBender.ChannelId.ERR_CONTACTOR_WELD, false) //
				.output(EvseChargePointBender.ChannelId.ERR_BACKEND_DISCONNECTED, false) //
				.output(EvseChargePointBender.ChannelId.ERR_ACTUATOR_LOCKING_FAILED, false) //
				.output(EvseChargePointBender.ChannelId.ERR_ACTUATOR_LOCKING_WITHOUT_PLUG_FAILED, false) //
				.output(EvseChargePointBender.ChannelId.ERR_ACTUATOR_STUCK, false) //
				.output(EvseChargePointBender.ChannelId.ERR_ACTUATOR_DETECTION_FAILED, false) //
				.output(EvseChargePointBender.ChannelId.ERR_FW_UPDATE_RUNNING, false) //
				.output(EvseChargePointBender.ChannelId.ERR_TILT, false) //
				.output(EvseChargePointBender.ChannelId.ERR_WRONG_CP_PR_WIRING, false) //
				.output(EvseChargePointBender.ChannelId.ERR_TYPE2_OVERLOAD_THR_2, false) //
				.output(EvseChargePointBender.ChannelId.ERR_ACTUATOR_UNLOCKED_WHILE_CHARGING, true) //
				.output(EvseChargePointBender.ChannelId.ERR_TILT_PREVENT_CHARGING_UNTIL_REBOOT, false) //
				.output(EvseChargePointBender.ChannelId.ERR_PIC24, false) //
				.output(EvseChargePointBender.ChannelId.ERR_USB_STICK_HANDLING, false) //
				.output(EvseChargePointBender.ChannelId.ERR_INCORRECT_PHASE_INSTALLATION, false) //
				.output(EvseChargePointBender.ChannelId.ERR_NO_POWER, false) //

				.output(EvseChargePointBender.ChannelId.MAX_CURRENT_EV, null) //
				.output(EvseChargePointBender.ChannelId.MIN_CURRENT_LIMIT, null) //
				.output(EvseChargePointBender.ChannelId.CHARGE_DURATION, null) //

				.output(EvseChargePointBender.ChannelId.SOFTWARE_VERSION_MAJOR, 1) //
				.output(EvseChargePointBender.ChannelId.SOFTWARE_VERSION_MINOR, 5) //
				.output(EvseChargePointBender.ChannelId.SOFTWARE_VERSION_PATCH, 22) //
				.output(EvseChargePointBender.ChannelId.SOFTWARE_VERSION_BUILD, null) //
				.output(EvseChargePointBender.ChannelId.RAW_DEVICE_ID, 16717) //
				.output(EvseChargePointBender.ChannelId.CHARGE_POINT_MODEL, "ABCD1234EFGH5678IJKL") //
				.output(Mennekes.ChannelId.DEVICE_ID, DeviceID.FOUR_YOU) //

				.output(ElectricityMeter.ChannelId.CURRENT, 18_000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L1, 6_000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L2, 6_000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, 6_000) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1380) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1380) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1380) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 4140) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER, null) // no register for reactive Power
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, null) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //
				.output(ElectricityMeter.ChannelId.VOLTAGE, 230_000) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230_000) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 230_000) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 230_000) //
				.output(ElectricityMeter.ChannelId.FREQUENCY, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 0L) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //

				.output(EvseChargePointBender.ChannelId.VEHICLE_STATE, VehicleState.STATE_C) //
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true)//

				.output(Mennekes.ChannelId.EMS_CURRENT_LIMIT, 16)) //
				.deactivate();

		var abilities = this.mennekes.getChargePointAbilities();
		assertTrue(abilities.isReadyForCharging());
		assertTrue(abilities.isEvConnected());
		assert (abilities.phaseSwitch().ability() instanceof ApplyPhaseSwitch.PhaseSwitchAbility.Internal);
		assertEquals(ApplyPhaseSwitch.PhaseSwitchDirection.TO_SINGLE_PHASE, abilities.phaseSwitch().direction());
		assertEquals(SingleOrThreePhase.SINGLE_PHASE, abilities.phaseSwitch().oppositePhaseApplySetPoint().phase());
		assertEquals(1380, abilities.phaseSwitch().oppositePhaseApplySetPoint().min());
		assertEquals(3680, abilities.phaseSwitch().oppositePhaseApplySetPoint().max());
		assert (abilities.applySetPoint() instanceof ApplySetPoint.Ability.Watt);
		assertEquals(11040, abilities.applySetPoint().max());
		assertEquals(4140, abilities.applySetPoint().min());
	}

	@Test
	void testGetChargePointAbilitiesKeepsCurrentPhaseWhileInternalPhaseSwitchIsRunning() throws Exception {
		this.test.next(new TestCase()//
				.output(Mennekes.ChannelId.PHASE_SWITCH_MODE, PhaseSwitchMode.DYNAMIC_PHASE_SWITCH) //
				.output(Mennekes.ChannelId.PHASE_SWITCH_RUNNING, true) //
				.output(ElectricityMeter.ChannelId.CURRENT_L1, 6_000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L2, 6_000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, 6_000) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1380) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1380) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1380) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 4140) //
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true));

		this.mennekes.apply(ChargePointActions.from(this.mennekes.getChargePointAbilities()) //
				.setApplySetPointInWatt(1380) //
				.setPhaseSwitch(new ApplyPhaseSwitch(ApplyPhaseSwitch.PhaseSwitchDirection.TO_SINGLE_PHASE,
						new ApplyPhaseSwitch.PhaseSwitchAbility.Internal())) //
				.build());

		var abilities = this.mennekes.getChargePointAbilities();

		assertEquals(SingleOrThreePhase.SINGLE_PHASE, abilities.applySetPoint().phase());
		assertEquals(1380, abilities.applySetPoint().min());
		assertEquals(3680, abilities.applySetPoint().max());
		assertEquals(ApplyPhaseSwitch.PhaseSwitchDirection.TO_THREE_PHASE, abilities.phaseSwitch().direction());
		assertEquals(SingleOrThreePhase.THREE_PHASE, abilities.phaseSwitch().oppositePhaseApplySetPoint().phase());
		assertEquals(4140, abilities.phaseSwitch().oppositePhaseApplySetPoint().min());
		assertEquals(11040, abilities.phaseSwitch().oppositePhaseApplySetPoint().max());
	}

	@Test
	void testGetChargePointAbilitiesReportsTargetPhaseWhileInternalPhaseSwitchIsRunningForThreePhase()
			throws Exception {
		this.test.next(new TestCase()//
				.output(Mennekes.ChannelId.PHASE_SWITCH_MODE, PhaseSwitchMode.DYNAMIC_PHASE_SWITCH) //
				.output(Mennekes.ChannelId.PHASE_SWITCH_RUNNING, true) //
				.output(ElectricityMeter.ChannelId.CURRENT_L1, 6_000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L2, 6_000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, 6_000) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1380) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1380) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1380) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 4140) //
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true));

		this.mennekes.apply(ChargePointActions.from(this.mennekes.getChargePointAbilities()) //
				.setApplySetPointInWatt(1380) //
				.setPhaseSwitch(new ApplyPhaseSwitch(ApplyPhaseSwitch.PhaseSwitchDirection.TO_SINGLE_PHASE,
						new ApplyPhaseSwitch.PhaseSwitchAbility.Internal())) //
				.build());

		var abilitiesWhileSwitchingToSingle = this.mennekes.getChargePointAbilities();
		assertEquals(SingleOrThreePhase.SINGLE_PHASE, abilitiesWhileSwitchingToSingle.applySetPoint().phase());
		assertEquals(ApplyPhaseSwitch.PhaseSwitchDirection.TO_THREE_PHASE,
				abilitiesWhileSwitchingToSingle.phaseSwitch().direction());

		this.mennekes.apply(ChargePointActions.from(abilitiesWhileSwitchingToSingle) //
				.setApplySetPointInWatt(1380) //
				.setPhaseSwitch(new ApplyPhaseSwitch(ApplyPhaseSwitch.PhaseSwitchDirection.TO_THREE_PHASE,
						new ApplyPhaseSwitch.PhaseSwitchAbility.Internal())) //
				.build());

		this.test.next(new TestCase()//
				.output(Mennekes.ChannelId.PHASE_SWITCH_MODE, PhaseSwitchMode.DYNAMIC_PHASE_SWITCH) //
				.output(Mennekes.ChannelId.PHASE_SWITCH_RUNNING, true) //
				.output(ElectricityMeter.ChannelId.CURRENT_L1, 6_000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L2, 6_000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, 6_000) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1380) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1380) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1380) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 4140) //
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true));

		var abilities = this.mennekes.getChargePointAbilities();

		assertEquals(SingleOrThreePhase.THREE_PHASE, abilities.applySetPoint().phase());
		assertEquals(ApplyPhaseSwitch.PhaseSwitchDirection.TO_SINGLE_PHASE, abilities.phaseSwitch().direction());
	}

}
