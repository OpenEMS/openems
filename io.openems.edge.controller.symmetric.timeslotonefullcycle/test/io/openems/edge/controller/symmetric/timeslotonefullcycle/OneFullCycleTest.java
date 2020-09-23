package io.openems.edge.controller.symmetric.timeslotonefullcycle;

import java.time.DayOfWeek;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class OneFullCycleTest {

	ChannelAddress essActivePower = new ChannelAddress("ess0", "ActivePower");
	ChannelAddress essSetPower = new ChannelAddress("ess0", "SetActivePowerEquals");

	@Test
	public void test() throws Exception {

		MyConfig myconfig = new MyConfig("ctrl0", "ess0", CycleOrder.START_WITH_DISCHARGE,
				"[{ \"year\" :2019, \"month\" : 9 , \"day\" : 19, \"hour\" : 12,\"minute\" : 06}]", false,
				DayOfWeek.MONDAY, 8, true, 10000);

		new ControllerTest(new OneFullCycle()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(myconfig) //
				.next( //
						new TestCase() //
								.output(essSetPower, 10000)) //
		;
	}
}