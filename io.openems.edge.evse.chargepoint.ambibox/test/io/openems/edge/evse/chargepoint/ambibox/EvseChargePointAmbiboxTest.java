package io.openems.edge.evse.chargepoint.ambibox;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.channel.Level;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.ambibox.enums.BatteryState;
import io.openems.edge.evse.chargepoint.ambibox.enums.ChargeProtocol;
import io.openems.edge.evse.chargepoint.ambibox.enums.Connector;
import io.openems.edge.evse.chargepoint.ambibox.enums.ControlMode;
import io.openems.edge.evse.chargepoint.ambibox.enums.EvseState;
import io.openems.edge.evse.chargepoint.ambibox.enums.InverterState;
import io.openems.edge.meter.api.ElectricityMeter;

class EvseChargePointAmbiboxTest {

	private static final int MAX_HW_POWER = 11_000;

	private EvseChargePointAmbiboxImpl sut;
	private ComponentTest test;

	@BeforeEach
	void setup() throws Exception {
		this.createComponentTest(AmbiboxTestFixtures.createChargingBridge(), false);
		this.test //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase());
	}

	private void createComponentTest(DummyModbusBridge bridge, boolean readOnly) throws Exception {
		this.sut = new EvseChargePointAmbiboxImpl();
		this.test = new ComponentTest(this.sut) //
				.addReference("setModbus", bridge) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setConnector(Connector.CONNECTOR_1) //
						.setReadOnly(readOnly) //
						.setMaxHwPower(MAX_HW_POWER) //
						.build());
	}

	@Test
	void test() throws Exception {
		this.test //
				.next(new TestCase() //
						.activateStrictMode() //

						.output(OpenemsComponent.ChannelId.STATE, Level.OK) //
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 6900) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2300) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 2300) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 2300) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER, null) // no register for reactive Power
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 230_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 230_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 230_000) //
						.output(ElectricityMeter.ChannelId.CURRENT, 30_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 10_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 10_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 10_000) //
						.output(ElectricityMeter.ChannelId.FREQUENCY, 50_000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 1_000_000L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 123_450_000L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //

						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true) //

						.output(EvseChargePointAmbibox.ChannelId.SLEEP, false) //
						.output(EvseChargePointAmbibox.ChannelId.POWER_FACTOR, 0.98F) //
						.output(EvseChargePointAmbibox.ChannelId.VOLTAGE_DC, 400.0F) //
						.output(EvseChargePointAmbibox.ChannelId.CURRENT_DC, 17.25F) //
						.output(EvseChargePointAmbibox.ChannelId.POWER_DC, 6900) //
						.output(EvseChargePointAmbibox.ChannelId.NUMBER_PHASES, 3) //
						.output(EvseChargePointAmbibox.ChannelId.CHARGER_MIN_POWER, 1400) //
						.output(EvseChargePointAmbibox.ChannelId.CHARGER_MAX_POWER, 11_000) //
						.output(EvseChargePointAmbibox.ChannelId.INVERTER_STATE, InverterState.STARTED) //
						.output(EvseChargePointAmbibox.ChannelId.INVERTER_ERROR, 0) //
						.output(EvseChargePointAmbibox.ChannelId.INVERTER_TEMPERATURE, 31.2F) //
						.output(EvseChargePointAmbibox.ChannelId.CAPACITY, 72_000) //
						.output(EvseChargePointAmbibox.ChannelId.MIN_SOC, 5) //
						.output(EvseChargePointAmbibox.ChannelId.MAX_SOC, 95) //
						.output(EvseChargePointAmbibox.ChannelId.EV_SOC, 59) //
						.output(EvseChargePointAmbibox.ChannelId.STATE_OF_HEALTH, 98) //
						.output(EvseChargePointAmbibox.ChannelId.TIME_TO_FULL_SOC, 1257) //
						.output(EvseChargePointAmbibox.ChannelId.NUMBER_CYCLES, 109) //
						.output(EvseChargePointAmbibox.ChannelId.EV_MIN_CHARGE_POWER, 100) //
						.output(EvseChargePointAmbibox.ChannelId.EV_MAX_CHARGE_POWER, 11_000) //
						.output(EvseChargePointAmbibox.ChannelId.EV_MIN_DISCHARGE_POWER, 200) //
						.output(EvseChargePointAmbibox.ChannelId.EV_MAX_DISCHARGE_POWER, 7000) //
						.output(EvseChargePointAmbibox.ChannelId.BATTERY_STATE, BatteryState.CHARGE) //
						.output(EvseChargePointAmbibox.ChannelId.BATTERY_TEMPERATURE, 20.9F) //
						.output(EvseChargePointAmbibox.ChannelId.BATTERY_ERROR, 0) //
						.output(EvseChargePointAmbibox.ChannelId.CONTROL_MODE, ControlMode.CONTROLLABLE) //
						.output(EvseChargePointAmbibox.ChannelId.POWER_DC_BATTERY, 6800) //
						.output(EvseChargePointAmbibox.ChannelId.CHARGE_PROTOCOL, ChargeProtocol.ISO_15118_2) //
						.output(EvseChargePointAmbibox.ChannelId.SESSION_STATE, EvseState.CHARGE_LOOP) //
						.output(EvseChargePointAmbibox.ChannelId.EV_CONNECTED, true) //
						.output(EvseChargePointAmbibox.ChannelId.SECONDS_TO_DEPARTURE, 7305) //
						.output(EvseChargePointAmbibox.ChannelId.DEPARTURE_SOC, 80) //
						.output(EvseChargePointAmbibox.ChannelId.DEPARTURE_ENERGY, 4300) //
						.output(EvseChargePointAmbibox.ChannelId.MIN_ENERGY_REQUEST, 4300) //
						.output(EvseChargePointAmbibox.ChannelId.MAX_ENERGY_REQUEST, 8600) //
						.output(EvseChargePointAmbibox.ChannelId.EV_CHARGER_ERROR, 0) //
						.output(EvseChargePointAmbibox.ChannelId.ERR_EV_COMMUNICATION, false) //
						.output(EvseChargePointAmbibox.ChannelId.ERR_PE_COMMUNICATION, false) //
						.output(EvseChargePointAmbibox.ChannelId.ERR_LOAD_DUMP, false) //
						.output(EvseChargePointAmbibox.ChannelId.ERR_PLUG_TEMPERATURE, false) //
						.output(EvseChargePointAmbibox.ChannelId.ERR_CABLE_CHECK, false) //
						.output(EvseChargePointAmbibox.ChannelId.ERR_EMERGENCY_OFF, false) //
						.output(EvseChargePointAmbibox.ChannelId.ENERGY_SESSION, 5000) //
						.output(EvseChargePointAmbibox.ChannelId.ENERGY_EXPORT_SESSION, 0) //
						.output(EvseChargePointAmbibox.ChannelId.REPLUG_REQUIRED, false) //

						// no pending write values
						.output(EvseChargePointAmbibox.ChannelId.SET_TARGET_POWER, null) //
						.output(EvseChargePointAmbibox.ChannelId.SET_WAKE_UP, null)) // WRITE_ONLY Channel
				.deactivate();

		// read-back of the 'Target PowerAC' Holding Register (-11000)
		assertEquals(11_000, this.sut.getSetTargetPowerChannel().value().orElse(null));

		var abilities = this.sut.getChargePointAbilities();
		assertTrue(abilities.isEvConnected());
		assertTrue(abilities.isReadyForCharging());
		assertNull(abilities.phaseSwitch()); // Phase Switch not available
		var applySetPoint = (ApplySetPoint.Ability.Watt) abilities.applySetPoint();
		assertEquals(THREE_PHASE, applySetPoint.phase());
		// the minimum charge power always defaults to 6 A (per three phases)
		assertEquals(ApplySetPoint.MIN_POWER_THREE_PHASE, applySetPoint.min());
		assertEquals(MAX_HW_POWER, applySetPoint.max());
	}

	@Test
	void testApplySetPoint() throws Exception {
		var actions = ChargePointActions.from(this.sut.getChargePointAbilities()) //
				.setApplySetPointInWatt(6900) //
				.build();
		this.sut.apply(actions);

		// target power is written to the 'Target PowerAC' register (with inverted sign)
		assertEquals(6900, this.sut.getSetTargetPowerChannel().getNextWriteValue().get());
		// charger is awake -> no wake up required
		assertFalse(this.sut.getSetWakeUpChannel().getNextWriteValue().isPresent());

		// stopping the charging session writes a zero set-point
		this.sut.apply(ChargePointActions.from(this.sut.getChargePointAbilities()) //
				.setApplySetPointInWatt(0) //
				.build());
		assertEquals(0, this.sut.getSetTargetPowerChannel().getNextWriteValue().get());

		this.test.deactivate();
	}

	@Test
	void testNotReadyForChargingOnEvChargerError() throws Exception {
		this.createComponentTest(AmbiboxTestFixtures.createErrorBridge(), false);
		this.test //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false) //
						.output(EvseChargePointAmbibox.ChannelId.SLEEP, true) //
						.output(EvseChargePointAmbibox.ChannelId.EV_CONNECTED, true) //
						.output(EvseChargePointAmbibox.ChannelId.EV_CHARGER_ERROR, 8) //
						.output(EvseChargePointAmbibox.ChannelId.ERR_PLUG_TEMPERATURE, true) //
						.output(EvseChargePointAmbibox.ChannelId.ERR_EV_COMMUNICATION, false));

		assertFalse(this.sut.getIsReadyForCharging());

		// charging requested while the charger is asleep -> wake up
		this.sut.apply(ChargePointActions.from(this.sut.getChargePointAbilities()) //
				.setApplySetPointInWatt(6900) //
				.build());
		assertEquals(6900, this.sut.getSetTargetPowerChannel().getNextWriteValue().get());
		assertEquals(1, this.sut.getSetWakeUpChannel().getNextWriteValue().get());

		this.test.deactivate();
	}

	@Test
	void testReadOnly() throws Exception {
		this.createComponentTest(AmbiboxTestFixtures.createChargingBridge(), true);
		this.test //
				.next(new TestCase());

		var abilities = this.sut.getChargePointAbilities();
		// empty ApplySetPoint ability
		assertEquals(0, abilities.applySetPoint().min());
		assertEquals(0, abilities.applySetPoint().max());
		assertFalse(abilities.isReadyForCharging());
		assertFalse(abilities.isEvConnected());

		// apply is not executed in read-only mode
		this.sut.apply(ChargePointActions.from(abilities) //
				.setApplySetPointInWatt(6900) //
				.build());
		assertFalse(this.sut.getSetTargetPowerChannel().getNextWriteValue().isPresent());

		this.test.deactivate();
	}
}
