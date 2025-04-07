package io.openems.edge.controller.api.modbus.readwrite.rtu;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerApiModbusRtuReadWriteImplTest {

	private static final String CTRL_ID = "ctrl0";

	private TimeLeapClock clock = new TimeLeapClock(Instant.parse("2024-01-01T01:00:00.00Z"), ZoneOffset.UTC);

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerApiModbusRtuReadWriteImpl()) //
				.addReference("componentManager", new DummyComponentManager(this.clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEnabled(false) // do not actually start server
						.setParity(Parity.NONE).setStopbit(Stopbit.ONE) //
						.setBaudrate(9600) //
						.setComponentIds() //
						.setMaxConcurrentConnections(5) //
						.setPortName("/dev/ttyUSB0") //
						.build()) //
				.next(new TestCase()) //
		;
	}
}
