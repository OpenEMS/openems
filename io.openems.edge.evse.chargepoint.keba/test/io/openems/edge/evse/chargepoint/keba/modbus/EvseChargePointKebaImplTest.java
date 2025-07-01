package io.openems.edge.evse.chargepoint.keba.modbus;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.chargepoint.PhaseRotation.L2_L3_L1;
import static io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity.DEBUG_LOG;
import static io.openems.edge.evse.chargepoint.keba.modbus.EvseChargePointKebaModbusImpl.CONVERT_FIRMWARE_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evse.chargepoint.keba.common.EvseChargePointKeba;
import io.openems.edge.evse.chargepoint.keba.common.enums.ProductTypeAndFeatures;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvseChargePointKebaImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvseChargePointKebaModbusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
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
								new int[] { 0x030A, 0x0D00 }) //
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
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setWiring(
								THREE_PHASE) //
						.setP30hasS10PhaseSwitching(false) //
						.setPhaseRotation(L2_L3_L1) //
						.setLogVerbosity(DEBUG_LOG) //
						.build()) //
				.next(new TestCase(), 15) //
				.next(new TestCase() //
						.output(EvseChargePointKeba.ChannelId.FIRMWARE, "3.10.13") //
						.output(EvseChargePointKebaModbus.ChannelId.PTAF_PRODUCT_TYPE,
								ProductTypeAndFeatures.ProductType.KC_P30) //
						.output(EvseChargePointKebaModbus.ChannelId.PTAF_CABLE_OR_SOCKET,
								ProductTypeAndFeatures.CableOrSocket.CABLE) //
						.output(EvseChargePointKebaModbus.ChannelId.PTAF_SUPPORTED_CURRENT,
								ProductTypeAndFeatures.SupportedCurrent.A32) //
						.output(EvseChargePointKebaModbus.ChannelId.PTAF_DEVICE_SERIES,
								ProductTypeAndFeatures.DeviceSeries.C_SERIES) //
						.output(EvseChargePointKebaModbus.ChannelId.PTAF_ENERGY_METER,
								ProductTypeAndFeatures.EnergyMeter.STANDARD) //
						.output(EvseChargePointKebaModbus.ChannelId.PTAF_AUTHORIZATION,
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

	@Test
	public void testConvertFirmwareVersion() {
		assertNull(CONVERT_FIRMWARE_VERSION.elementToChannel(null));
		assertEquals("3.10.13", CONVERT_FIRMWARE_VERSION.elementToChannel(50990336L));
		assertEquals("3.10.57", CONVERT_FIRMWARE_VERSION.elementToChannel(51001600L));
	}

	@Test
	public void testConvertProductTypeAndFeatures() {
		{
			var ptaf = ProductTypeAndFeatures.from(null);
			assertEquals(ProductTypeAndFeatures.ProductType.UNDEFINED, ptaf.productType());
			assertEquals(ProductTypeAndFeatures.CableOrSocket.UNDEFINED, ptaf.cableOrSocket());
			assertEquals(ProductTypeAndFeatures.SupportedCurrent.UNDEFINED, ptaf.supportedCurrent());
			assertEquals(ProductTypeAndFeatures.DeviceSeries.UNDEFINED, ptaf.deviceSeries());
			assertEquals(ProductTypeAndFeatures.EnergyMeter.UNDEFINED, ptaf.energyMeter());
			assertEquals(ProductTypeAndFeatures.Authorization.UNDEFINED, ptaf.authorization());
		}
		{
			var ptaf = ProductTypeAndFeatures.from(304111L);
			assertEquals(ProductTypeAndFeatures.ProductType.KC_P30, ptaf.productType());
			assertEquals(ProductTypeAndFeatures.CableOrSocket.SOCKET, ptaf.cableOrSocket());
			assertEquals(ProductTypeAndFeatures.SupportedCurrent.A32, ptaf.supportedCurrent());
			assertEquals(ProductTypeAndFeatures.DeviceSeries.C_SERIES, ptaf.deviceSeries());
			assertEquals(ProductTypeAndFeatures.EnergyMeter.STANDARD, ptaf.energyMeter());
			assertEquals(ProductTypeAndFeatures.Authorization.WITH_RFID, ptaf.authorization());
		}
		{
			var ptaf = ProductTypeAndFeatures.from(314110L);
			assertEquals(ProductTypeAndFeatures.ProductType.KC_P30, ptaf.productType());
			assertEquals(ProductTypeAndFeatures.CableOrSocket.CABLE, ptaf.cableOrSocket());
			assertEquals(ProductTypeAndFeatures.SupportedCurrent.A32, ptaf.supportedCurrent());
			assertEquals(ProductTypeAndFeatures.DeviceSeries.C_SERIES, ptaf.deviceSeries());
			assertEquals(ProductTypeAndFeatures.EnergyMeter.STANDARD, ptaf.energyMeter());
			assertEquals(ProductTypeAndFeatures.Authorization.NO_RFID, ptaf.authorization());
		}
	}
}