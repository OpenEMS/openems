package io.openems.edge.ruhfass.battery.batcon;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.ruhfass.battery.batcon.enums.BatteryType;
import io.openems.edge.ruhfass.battery.batcon.enums.RemainingBusSimulationCommand;

public class AudiBatconImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new BatconImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setStartStop(StartStopConfig.START)//
						.setRemainingBusSimulationCommand(RemainingBusSimulationCommand.OFF)//
						.setBatteryType(BatteryType.CBEV)//
						.build())
				.next(new TestCase()) //
				.deactivate();
	}

}
