package io.openems.edge.meter.carlo.gavazzi.em300;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.types.MeterType.GRID;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.ElectricityMeter;

public class MeterCarloGavazziEm300ImplTest {

	final int offset = 300000 + 1;

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterCarloGavazziEm300Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setType(GRID) //
						.build()) //
		;
	}

	@Test
	public void testReadFromModbus() throws Exception {
		var sut = new MeterCarloGavazziEm300Impl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addComponent(new DummyComponentManager(createDummyClock()))
				.addReference("setModbus", new DummyModbusBridge("modbus2") //
						.withInputRegisters(300001 - this.offset, //
								new int[] { 0x08c3, 0x0000, //
										0x0000, 0x0000, //
										0x0000, 0x0000 }) //
						.withInputRegisters(300013 - this.offset, //
								new int[] { 0x1102, 0x0000, //
										0x0000, 0x0000, //
										0x0000, 0x0000, //
										0x261c, 0x0000, //
										0x0000, 0x0000, //
										0x0000, 0x0000, //
										0x261d, 0x0000, //
										0x0000, 0x0000, //
										0x0000, 0x0000, //
										0x0090, 0x0000, //
										0x0000, 0x0000, //
										0x0000, 0x0000, //
										0x02eb, 0x0000, //
										0x0000, 0x0000, //
										0x261c, 0x0000, //
										0x261d, 0x0000, //
										0x0090, 0x0000 }) //
				).activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus2") //
						.setModbusUnitId(1) //
						.setType(GRID) //
						.setInvert(false) //
						.build()) //
				.next(new TestCase() //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 224300) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 0) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 0) //
						//
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 4354) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 0) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 0) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 976) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
						.output(MeterCarloGavazziEm300.ChannelId.APPARENT_POWER_L1, 976) //
						.output(MeterCarloGavazziEm300.ChannelId.APPARENT_POWER_L2, 0) //
						.output(MeterCarloGavazziEm300.ChannelId.APPARENT_POWER_L3, 0) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, 14) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, 0) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, 0) //
						//
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 976) //
						.output(MeterCarloGavazziEm300.ChannelId.APPARENT_POWER, 976) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER, 14) //
				) //
				.deactivate();
	}
}