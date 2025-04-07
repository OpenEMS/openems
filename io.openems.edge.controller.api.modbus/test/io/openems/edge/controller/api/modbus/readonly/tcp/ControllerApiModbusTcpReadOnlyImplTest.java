package io.openems.edge.controller.api.modbus.readonly.tcp;

import static io.openems.edge.controller.api.modbus.AbstractModbusTcpApi.DEFAULT_PORT;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiModbusTcpReadOnlyImplTest {

	private TimeLeapClock clock = new TimeLeapClock(Instant.parse("2024-01-01T01:00:00.00Z"), ZoneOffset.UTC);

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerApiModbusTcpReadOnlyImpl()) //
				.addReference("componentManager", new DummyComponentManager(this.clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEnabled(false) // do not actually start server
						.setComponentIds() //
						.setMaxConcurrentConnections(5) //
						.setPort(DEFAULT_PORT) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
