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
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, //
						0x0000, //
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, //
						0x0000, //
						0x0000, //
						0x0000, //
						0x0000, //
						0x0000) //
				.withRegisters(HagerEcr380dMeter.ENERGY_START_ADDRESS | 0x0000, //
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000) // 
				.withRegisters(HagerEcr380dMeter.ENERGY_PER_PHASE_START_ADDRESS | 0x0000, //
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000,//
						0x0000, 0x0000);
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
	
	private void withRegister(DummyModbusBridge bridge, int address, String value) {
		final byte[] registers = value.getBytes();
		for (int i = 0; i < registers.length; i += 2) {
			bridge.withRegister(address++, registers[i], registers[i + 1]);
		}
	}
}
