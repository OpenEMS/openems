package io.openems.edge.fluidcooling.hydac;

import static io.openems.edge.common.startstop.StartStopConfig.START;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.fluidcooling.hydac.Hydac.ChannelId;

public class HydacImplTest {

	@Test
	public void testHydac() throws Exception {
		final var hydac = new HydacImpl();
		var test = new ComponentTest(hydac) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin())//
				.activate(MyConfig.create() //
						.setId("hydac0") //
						.setStartStop(START) //
						.setTemperatureSetpoint(2500) //
						.setModbusId("modbus0") //
						.build()) //
				.next(new TestCase("Validate, if config setpoint gets set to channel") //
						.output(ChannelId.COOLING_CIRCUIT_1_SET_POINT, 25));

		assertEquals(
				"Running|CoolingUnit|Temp: UNDEFINED|CoolingRequired: UNDEFINED|Pump: UNDEFINED|Compressor: UNDEFINED",
				hydac.debugLog());

		test.next(new TestCase("Set values to see if debug log recognizes") //
				.input(ChannelId.OUTLET_TEMPERATURE, 25) //
				.input(ChannelId.COOLING_REQUIRED, false) //
				.input(ChannelId.PUMP, false) //
				.input(ChannelId.COMPRESSOR, false) //
		);

		assertEquals("Running|CoolingUnit|Temp: 25 °C|CoolingRequired: false|Pump: false|Compressor: false",
				hydac.debugLog());
	}
}
