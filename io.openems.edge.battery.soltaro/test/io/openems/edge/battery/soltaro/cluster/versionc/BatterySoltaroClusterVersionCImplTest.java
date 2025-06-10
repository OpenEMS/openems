package io.openems.edge.battery.soltaro.cluster.versionc;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class BatterySoltaroClusterVersionCImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new BatterySoltaroClusterVersionCImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setStartStop(StartStopConfig.AUTO) //
						.build()) //
		;
	}
}
