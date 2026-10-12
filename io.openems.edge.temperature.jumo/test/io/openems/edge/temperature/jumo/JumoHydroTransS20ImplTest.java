package io.openems.edge.temperature.jumo;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class JumoHydroTransS20ImplTest {

	@Test
	public void testJumoHydroTransS20() throws Exception {
		final var tempSensor = new JumoHydroTransS20Impl();
		new ComponentTest(tempSensor) //
				.activate(MyConfig.create() //
						.setId("temp0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.build()) //
				.next(new TestCase());
	}
}
