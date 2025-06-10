package io.openems.edge.batteryinverter.refu88k;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class BatteryInverterRefuStore88kImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new BatteryInverterRefuStore88kImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setModbusId("modbus0") //
						.setStartStop(StartStopConfig.AUTO) //
						.setTimeLimitNoPower(0) //
						.setWatchdoginterval(0) //
						.build()) //
		;
	}

}
