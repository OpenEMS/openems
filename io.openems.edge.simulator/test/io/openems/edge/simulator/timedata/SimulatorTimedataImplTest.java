package io.openems.edge.simulator.timedata;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.simulator.CsvFormat;

public class SimulatorTimedataImplTest {

	private static final String COMPONENT_ID = "thermometer0";

	@Test
	public void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorTimedataImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setFilename("") //
						.setFormat(CsvFormat.ENGLISH) //
						.build()) //
				.next(new TestCase());
	}

}
