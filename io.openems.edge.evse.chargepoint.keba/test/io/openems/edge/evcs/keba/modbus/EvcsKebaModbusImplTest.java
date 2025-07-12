package io.openems.edge.evcs.keba.modbus;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.evcs.api.PhaseRotation.L2_L3_L1;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evse.chargepoint.keba.common.enums.ProductTypeAndFeatures;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvcsKebaModbusImplTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		new ComponentTest(new EvcsKebaModbusImpl()) //
				.addReference("evcsPower", new DummyEvcsPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock))
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setDebugMode(false)//
						.setMinHwCurrent(6000)//
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.setModbusId("modbus0") //
						.setModbusUnitId(255) //
						.setReadOnly(false) //
						.build()) //

				.next(new TestCase()//
						.input(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, 10350) // 15A
						.output(EvcsKebaModbus.ChannelId.DEBUG_SET_CHARGING_CURRENT, 15000)
						.output(EvcsKebaModbus.ChannelId.DEBUG_SET_ENABLE, 1))

				.next(new TestCase("no change because time hasnt passed") //
						.timeleap(clock, 2, ChronoUnit.SECONDS) //
						.input(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, 0) // 0A
						.output(EvcsKebaModbus.ChannelId.DEBUG_SET_CHARGING_CURRENT, 15000)
						.output(EvcsKebaModbus.ChannelId.DEBUG_SET_ENABLE, 1))

				.next(new TestCase("changes after 5 seconds have passed") //
						.timeleap(clock, 3, ChronoUnit.SECONDS) //
						.input(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, 0) // 0A
						.output(EvcsKebaModbus.ChannelId.DEBUG_SET_CHARGING_CURRENT, 0)
						.output(EvcsKebaModbus.ChannelId.DEBUG_SET_ENABLE, 0))

				.next(new TestCase()//
						.output(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER, 22080) //
						.output(Evcs.ChannelId.MINIMUM_HARDWARE_POWER, 4140) //
						.output(Evcs.ChannelId.PHASES, Phases.THREE_PHASE)) //

				.deactivate();
	}

	@Test
	public void test2() throws Exception {
		final var clock = createDummyClock();
		new ComponentTest(new EvcsKebaModbusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("evcsPower", new DummyEvcsPower()) //
				.addReference("componentManager", new DummyComponentManager(clock))
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(1000, // STATUS - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1004, // PLUG - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1006, // ERROR_CODE - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1008, // CURRENT_L1: 7_000
								new int[] { 0x0000, 0x1B58 }) //
						.withRegisters(1010, // CURRENT_L2: 8_000
								new int[] { 0x0000, 0x1F40 }) //
						.withRegisters(1012, // CURRENT_L3: 9_000
								new int[] { 0x0000, 0x2328 }) //
						.withRegisters(1014, // SERIAL_NUMBER - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1016, // Product Type and Features
								new int[] { 0x0004, 0xCAFE }) //
						.withRegisters(1018, // FIRMWARE
								new int[] { 0x0000, 0x2775 }) //
						.withRegisters(1020, // ACTIVE_POWER: 6_000_000
								new int[] { 0x005B, 0x8D80 }) //
						.withRegisters(1036, // ACTIVE_CONSUMPTION_ENERGY - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1040, // VOLTAGE_L1: 229
								new int[] { 0x0000, 0x00E5 }) //
						.withRegisters(1042, // VOLTAGE_L2: 230
								new int[] { 0x0000, 0x00E6 }) //
						.withRegisters(1044, // VOLTAGE_L3: 231
								new int[] { 0x0000, 0x00E7 }) //
						.withRegisters(1046, // POWER_FACTOR - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1100, // MAX_CHARGING_CURRENT - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1500, // RFID - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1502, // ENERGY_SESSION - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1550, // PHASE_SWITCH_SOURCE - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1552, // PHASE_SWITCH_STATE - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1600, // FAILSAFE_CURRENT_SETTING - TODO
								new int[] { 0x0000, 0x0000 }) //
						.withRegisters(1602, // FAILSAFE_TIMEOUT_SETTING - TODO
								new int[] { 0x0000, 0x0000 })) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setDebugMode(false)//
						.setMinHwCurrent(6000)//
						.setPhaseRotation(L2_L3_L1) //
						.setModbusId("modbus0") //
						.setModbusUnitId(255) //
						.setReadOnly(false) //
						.build()) //
				.next(new TestCase(), 15) //
				.next(new TestCase() //
						.output(EvcsKebaModbus.ChannelId.FIRMWARE, "1.1.1") //
						.output(EvcsKebaModbus.ChannelId.PTAF_PRODUCT_TYPE, ProductTypeAndFeatures.ProductType.KC_P30) //
						.output(EvcsKebaModbus.ChannelId.PTAF_CABLE_OR_SOCKET,
								ProductTypeAndFeatures.CableOrSocket.CABLE) //
						.output(EvcsKebaModbus.ChannelId.PTAF_SUPPORTED_CURRENT,
								ProductTypeAndFeatures.SupportedCurrent.A32) //
						.output(EvcsKebaModbus.ChannelId.PTAF_DEVICE_SERIES,
								ProductTypeAndFeatures.DeviceSeries.C_SERIES) //
						.output(EvcsKebaModbus.ChannelId.PTAF_ENERGY_METER, ProductTypeAndFeatures.EnergyMeter.STANDARD) //
						.output(EvcsKebaModbus.ChannelId.PTAF_AUTHORIZATION,
								ProductTypeAndFeatures.Authorization.NO_RFID)
						.output(ElectricityMeter.ChannelId.CURRENT, 24_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 9_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 7_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 8_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 230_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 231_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 229_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 230_000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 6000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2_259) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1_742) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1_999) //
				) //
				.deactivate();
	}

}
