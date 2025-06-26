package io.openems.edge.ess.generic.offgrid;

import org.junit.Test;

import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.batteryinverter.test.DummyOffGridBatteryInverter;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyOffGridSwitch;

public class EssGenericOffGridImplTest {

	@Test
	public void testStart() throws Exception {
		new ComponentTest(new EssGenericOffGridImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("batteryInverter", new DummyOffGridBatteryInverter("batteryInverter0")) //
				.addReference("battery", new DummyBattery("battery0")) //
				.addReference("offGridSwitch", new DummyOffGridSwitch("offGridSwitch0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setStartStopConfig(StartStopConfig.START) //
						.setBatteryInverterId("batteryInverter0") //
						.setBatteryId("battery0") //
						.setOffGridSwitchId("offGridSwitch0") //
						.build()) //
		;
	}
}
