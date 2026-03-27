package io.openems.edge.evcs.mennekes;

import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evcs.api.ChargeMode;
import io.openems.edge.evcs.api.ChargeState;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evse.chargepoint.bender.EvseChargePointBender;
import io.openems.edge.evse.chargepoint.bender.OcppState;
import io.openems.edge.evse.chargepoint.bender.VehicleState;
import io.openems.edge.evse.chargepoint.mennekes.common.Mennekes;
import io.openems.edge.evse.chargepoint.mennekes.common.TestUtils;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;

public class EvcsMennekesTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsMennekesImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", TestUtils.testModbus()) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.setReadOnly(false) //
						.setDebugMode(false) //
						.setMinHwCurrent(6) //
						.setMaxHwCurrent(16) //
						.build()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()//
						.activateStrictMode()//
						.output(OpenemsComponent.ChannelId.STATE, Level.WARNING)//
						.output(Mennekes.ChannelId.SET_CURRENT_LIMIT, null) // WRITE_ONLY Channel
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false)//

						.output(EvseChargePointBender.ChannelId.VEHICLE_STATE, VehicleState.STATE_C) //
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

						.output(EvseChargePointBender.ChannelId.SAFE_CURRENT, 6.0f) //
						.output(EvseChargePointBender.ChannelId.MAX_CURRENT_EV, null) //
						.output(EvseChargePointBender.ChannelId.MIN_CURRENT_LIMIT, null) //
						.output(EvseChargePointBender.ChannelId.CHARGE_DURATION, null) //

						.output(EvseChargePointBender.ChannelId.SOFTWARE_VERSION_MAJOR, 1) //
						.output(EvseChargePointBender.ChannelId.SOFTWARE_VERSION_MINOR, 5) //
						.output(EvseChargePointBender.ChannelId.SOFTWARE_VERSION_PATCH, 22) //
						.output(EvseChargePointBender.ChannelId.SOFTWARE_VERSION_BUILD, null) //

						.output(EvseChargePointBender.ChannelId.MAX_CURRENT, 16) //
						.output(EvseChargePointBender.ChannelId.MIN_CURRENT, 6) //
						.output(EvseChargePointBender.ChannelId.VEHICLE_STATE, VehicleState.STATE_C) //

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
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //

						.output(Evcs.ChannelId.STATUS, Status.CHARGING) //
						.output(Evcs.ChannelId.CHARGING_TYPE, ChargingType.AC) //
						.output(Evcs.ChannelId.PHASES, Phases.THREE_PHASE) //
						.output(Evcs.ChannelId.FIXED_MINIMUM_HARDWARE_POWER, 0) //
						.output(Evcs.ChannelId.FIXED_MAXIMUM_HARDWARE_POWER, 0) //
						.output(Evcs.ChannelId.MINIMUM_HARDWARE_POWER, 0) //
						.output(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER, 0) //
						.output(Evcs.ChannelId.MAXIMUM_POWER, null) //
						.output(Evcs.ChannelId.MINIMUM_POWER, null) //
						.output(Evcs.ChannelId.ENERGY_SESSION, null) //
						.output(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED, false) //

						.output(ManagedEvcs.ChannelId.POWER_PRECISION, 230.0) //
						.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, null) //
						.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT_WITH_FILTER, null) //
						.output(ManagedEvcs.ChannelId.IS_CLUSTERED, null) // set by cluster
						.output(ManagedEvcs.ChannelId.CHARGE_MODE, ChargeMode.FORCE_CHARGE) //
						.output(ManagedEvcs.ChannelId.SET_DISPLAY_TEXT, null) //
						.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_REQUEST, null) //
						.output(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT, null) //
						.output(ManagedEvcs.ChannelId.CHARGE_STATE, ChargeState.CHARGING) //

						.output(Mennekes.ChannelId.EMS_CURRENT_LIMIT, 16)) //
				.deactivate();
	}
}
