package io.openems.edge.meter.hager.ecr380d;


import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.common.test.ComponentTest;

/**
 * minimal smoke test with {@link DummyModbusBridge}.
 */
public class HagerEcr380dMeterTest {

	private static final String CID = "meter0";
	private static final String MID = "modbus0";
	private static final MyConfig CONFIG = MyConfig.create() //
			.setId(CID)
			.setModbusId(MID)
			.setType(MeterType.GRID)
			.build();
	
	private ComponentTest componentTest;

	@Before
	public void before() throws Exception {
		final DummyModbusBridge bridge = new DummyModbusBridge(MID) //
				.withRegister(HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0020, 0x0203)
				.withRegister(HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0021, 0x0000)
				.withRegister(HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0062, 0x0401)
				.withRegister(HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0063, 0x0000)
				.withRegister(HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0076, 0x0001)
				.withRegister(HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0077, 0x07E9)

				.withRegisters(HagerEcr380dMeter.INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0000, //
						23000, //
						23150, //
						22950, //
						39500, //
						39600, //
						39000, //
						5010, //
						0x0000, //
						0x0000, //
						0x0000, 0x4E20,//
						0x0000, 0x9C40,//
						0x0000, 0xEA60,//
						0x0000, 0x2710,//
						0x0027, 0x1F3C,// P_SUM
						0x0008, 0xF494,// Q_SUM
						0x0001, 0x2624,// S_SUM
						0x024B, // PF_SUM_IEC
						0x035C, // PF_SUM_IEEE
						0x0003, 0x5E80, // ACTIVE_POWER_L1
						0xFFFC, 0xA180, // ACTIVE_POWER_L2
						0x0001, 0xAF40, // ACTIVE_POWER_L3
						0x0003, 0x5E80, // REACTIVE_POWER_L1
						0x0001, 0xAF40, // REACTIVE_POWER_L2
						0xFFFE, 0x50C0, // REACTIVE_POWER_L3
						0x0003, 0x5E80, // S_L1
						0x0001, 0xAF40, // S_L2
						0x0000, 0x0000, // S_L3
						0x0000, // PF_L1_IEC
						0x0000, // PF_L1_IEC
						0x0000, // PF_L2_IEC
						0x0000, // PF_L1_IEEE
						0x0000, // PF_L2_IEEE
						0x0000  // PF_L3_IEEE
						) //
				.withRegisters(HagerEcr380dMeter.ENERGY_START_ADDRESS | 0x0000, //
						0x0098, 0x967F,// ACTIVE_CONSUMPTION_ENERGY
						0x0032, 0xDCD5,// ER_PLUS_SUM
						0x0009, 0xFBF1,// ACTIVE_PRODUCTION_ENERGY
						0x0012, 0xD687,// ER_MINUS_SUM
						0x0023, 0xCACE,// EA_PLUS_DETAILED_SUM
						0x0011, 0x201E) // EA_MINUS_DETAILED_SUM
				.withRegisters(HagerEcr380dMeter.ENERGY_PER_PHASE_START_ADDRESS | 0x0000, //
						0x0098, 0x961B,// ACTIVE_CONSUMPTION_ENERGY_L1
						0x0098, 0x95B7,// ACTIVE_CONSUMPTION_ENERGY_L2
						0x0098, 0x9553,// ACTIVE_CONSUMPTION_ENERGY_L3
						0x0098, 0x9297,// ACTIVE_PRODUCTION_ENERGY_L1
						0x0098, 0x8EAF,// ACTIVE_PRODUCTION_ENERGY_L2
						0x0098, 0x8AC7,// ACTIVE_PRODUCTION_ENERGY_L3
						0x0010, 0xF703,// ER_PLUS_L1
						0x0010, 0xF69F,// ER_PLUS_L2
						0x0010, 0xF63B,// ER_PLUS_L2
						0x0054, 0xC68F,// ER_MINUS_L1
						0x0054, 0xC62B,// ER_MINUS_L1
						0x0054, 0xC5C7); // ER_MINUS_L1
		this.withRegister(bridge, HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0000, "Hager                           ");
		this.withRegister(bridge, HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0010, "ECR380D                         ");
		this.withRegister(bridge, HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0022, "http://www.hager.de             ");
		this.withRegister(bridge, HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0032, "3P Meter 80A 4M                 ");
		this.withRegister(bridge, HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0042, "AGARDIO RJ45 MID                ");
		this.withRegister(bridge, HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0052, "APL                             ");
		this.withRegister(bridge, HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0064, "12345678                        ");
		this.withRegister(bridge, HagerEcr380dMeter.DEVICE_START_ADDRESS | 0x0074, "DE  ");
		
		this.componentTest = new ComponentTest(new HagerEcr380dMeterImpl())
			.addReference("cm", new io.openems.edge.common.test.DummyConfigurationAdmin())
			.addReference("setModbus", bridge);
	}
	
	@After
	public void after() throws Exception {
		if (this.componentTest != null) {
			this.componentTest.deactivate();
		}
		this.componentTest = null;
	}
	
	@Test
	public void activateDeactivateTest() throws Exception {
		this.componentTest.activate(CONFIG)
		.next(new TestCase())
		.deactivate();

		assertTrue(true);
	}
	
	@Test
	public void testVoltage() throws Exception {
		this.componentTest.activate(CONFIG)
		.next(new TestCase()//
				.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230000) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231500) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 229500) //
				.output(HagerEcr380dMeter.ChannelId.V_L1_L2, 395000) //
				.output(HagerEcr380dMeter.ChannelId.V_L2_L3, 396000) //
				.output(HagerEcr380dMeter.ChannelId.V_L3_L1, 390000) //
				)
		.deactivate();
	}
	
	@Test
	public void testFrequency() throws Exception {
		this.componentTest.activate(CONFIG)
		.next(new TestCase()//
				.output(ElectricityMeter.ChannelId.FREQUENCY, 50100) //
				)
		.deactivate();
	}
	
	@Test
	public void testCurrent() throws Exception {
		this.componentTest.activate(CONFIG)
		.next(new TestCase()//
				.output(ElectricityMeter.ChannelId.CURRENT_L1, 20000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L2, 40000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, 60000) //
				.output(HagerEcr380dMeter.ChannelId.I_NEUTRAL, 10000) //
				)
		.deactivate();
	}
	
	@Test
	public void testPower() throws Exception {
		this.componentTest.activate(CONFIG)
		.next(new TestCase()//
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 25639000) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER, 5869000) //
				.output(HagerEcr380dMeter.ChannelId.S_SUM, 753000) //
				.output(HagerEcr380dMeter.ChannelId.PF_SUM_IEC, 587) //
				.output(HagerEcr380dMeter.ChannelId.PF_SUM_IEEE, 860) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2208000) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, -2208000) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1104000) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, 2208000)
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, 1104000)
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, -1104000)
				.output(HagerEcr380dMeter.ChannelId.S_L1, 2208000) //
				.output(HagerEcr380dMeter.ChannelId.S_L2, 1104000) //
				.output(HagerEcr380dMeter.ChannelId.S_L3, 0) //
				)
		.deactivate();
	}
	
	@Test
	public void testEnergy() throws Exception {
		this.componentTest.activate(CONFIG)
		.next(new TestCase()//
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 9999999000L) //
				.output(HagerEcr380dMeter.ChannelId.ER_PLUS_SUM, 3333333000L) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 654321000L) //
				.output(HagerEcr380dMeter.ChannelId.ER_MINUS_SUM, 1234567000L) //
				.output(HagerEcr380dMeter.ChannelId.EA_PLUS_DETAILED_SUM, 2345678000L) //
				.output(HagerEcr380dMeter.ChannelId.EA_MINUS_DETAILED_SUM, 1122334000L) //
				)
		.deactivate();
	}
	
	@Test
	public void testEnergyPerPhase() throws Exception {
		this.componentTest.activate(CONFIG)
		.next(new TestCase()//
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, 9999899000L) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, 9999799000L) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, 9999699000L) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, 9998999000L) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, 9997999000L) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, 9996999000L) //
				.output(HagerEcr380dMeter.ChannelId.ER_PLUS_L1, 1111811000L) //
				.output(HagerEcr380dMeter.ChannelId.ER_PLUS_L2, 1111711000L) //
				.output(HagerEcr380dMeter.ChannelId.ER_PLUS_L3, 1111611000L) //
				.output(HagerEcr380dMeter.ChannelId.ER_MINUS_L1, 5555855000L) //
				.output(HagerEcr380dMeter.ChannelId.ER_MINUS_L2, 5555755000L) //
				.output(HagerEcr380dMeter.ChannelId.ER_MINUS_L3, 5555655000L) //
				)
		.deactivate();
	}
	
	
	private void withRegister(DummyModbusBridge bridge, int address, String value) {
		final byte[] registers = value.getBytes();
		for (int i = 0; i < registers.length; i += 2) {
			bridge.withRegister(address++, registers[i], registers[i + 1]);
		}
	}
}
