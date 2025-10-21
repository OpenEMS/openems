package io.openems.edge.ruhfass.battery.rbti;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.ruhfass.battery.rbti.enums.BatteryCellType;
import io.openems.edge.ruhfass.battery.rbti.enums.BatteryChannel;

public class RuhfassBatteryRbtiImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new RuhfassBatteryRbtiImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(64, new int[] { 0x0000, 0x0000, 0x0000, 0x0005 }) //
						.withRegisters(68, new int[] { 0x0007 }) //
				)//
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setStartStop(StartStopConfig.START) //
						.setModbusId("modbus0") //
						.setBatteryChannel(BatteryChannel.ONE) //
						.build()) //
				.next(new TestCase(), 10) // Execute read tasks
				.next(new TestCase() //
						.output(RuhfassBatteryRbti.ChannelId.BATTERY_CELL_TYPE, BatteryCellType.NMC) //
						.output(RuhfassBatteryRbti.ChannelId.TOTAL_NUMBER_OF_CELLS, 7) //
				) //
				.deactivate();
	}

}
