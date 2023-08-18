package io.openems.edge.simulator.battery;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class SimulatorBatteryImplTest {

	private static final String COMPONENT_ID = "battery0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new SimulatorBatteryImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setCapacityKWh(20) //
						.setChargeMaxCurrent(40) //
						.setChargeMaxVoltage(700) //
						.setDisChargeMaxCurrent(40) //
						.setDisChargeMinVoltage(400) //
						.setMinCellVoltage_mV(2800) //
						.setNumberOfSlaves(1) //
						.build()) //
				.next(new TestCase()) //
		;
	}
}
