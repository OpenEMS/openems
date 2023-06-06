package io.openems.edge.batteryinverter.refu88k;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class RefuStore88kImplTest {

	private static final String BATTERY_INVERTER_ID = "batteryInverter0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new BatteryInverterRefuStore88kImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_INVERTER_ID) //
						.setModbusId(MODBUS_ID) //
						.setStartStop(StartStopConfig.AUTO) //
						.setTimeLimitNoPower(0) //
						.setWatchdoginterval(0) //
						.build()) //
		;
	}

}
