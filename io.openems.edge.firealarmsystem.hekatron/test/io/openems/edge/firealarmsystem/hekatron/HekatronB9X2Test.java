package io.openems.edge.firealarmsystem.hekatron;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class HekatronB9X2Test {

	@Test
	public void testM0Module() throws Exception {
		final var blue0 = new HekatronB9X2Impl();
		new ComponentTest(blue0) //
				.addReference("cm", new DummyConfigurationAdmin())//
				.activate(MyConfig.create() //
						.setId("bma0") //
						.setConfigVersion(ConfigVersion.INDUSTRIAL_XL_V1)//
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	public void testIndustrialXlV1DetectorAlarmBits() throws Exception {
		new ComponentTest(new HekatronB9X2Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(1, //
								0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // Registers 1-14
								0b1, // Register 15: Detector 1 Alarm (Bit 0)
								0b1000000000, // Register 16: Detector 2 Alarm (Bit 9)
								0, 0)) //
				.activate(MyConfig.create() //
						.setId("hekatron0") //
						.setConfigVersion(ConfigVersion.INDUSTRIAL_XL_V1)//
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.build()) //
				.next(new TestCase(), 2) //
				.next(new TestCase() //
						.output(HekatronB9X2.ChannelId.DETECTOR_1_ALARM, true) //
						.output(HekatronB9X2.ChannelId.DETECTOR_1_ALARM_BIT9, false) //
						.output(HekatronB9X2.ChannelId.DETECTOR_2_ALARM, false) //
						.output(HekatronB9X2.ChannelId.DETECTOR_2_ALARM_BIT9, true)) //
				.deactivate();
	}

	@Test
	public void testIndustrialXlV2DetectorAlarmBits() throws Exception {
		new ComponentTest(new HekatronB9X2Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						// Registers 0-26; Detector 1 status in 0, Detector 2 status in 1
						.withRegisters(0, //
								new int[] { //
										0b1, // Register 0: Detector 1 Alarm (Bit 0)
										0b1000000000 }, // Register 1: Detector 2 Alarm (Bit 9)
								new int[25])) //
				.activate(MyConfig.create() //
						.setId("hekatron0") //
						.setConfigVersion(ConfigVersion.INDUSTRIAL_XL_V2)//
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.build()) //
				.next(new TestCase(), 2) //
				.next(new TestCase() //
						.output(HekatronB9X2.ChannelId.DETECTOR_1_ALARM, true) //
						.output(HekatronB9X2.ChannelId.DETECTOR_1_ALARM_BIT9, false) //
						.output(HekatronB9X2.ChannelId.DETECTOR_2_ALARM, false) //
						.output(HekatronB9X2.ChannelId.DETECTOR_2_ALARM_BIT9, true)) //
				.deactivate();
	}
}
