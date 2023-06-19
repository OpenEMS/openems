package io.openems.edge.simulator.datasource.csv.direct;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.simulator.CsvFormat;

public class SimulatorDatasourceCsvDirectImplTest {

	private static final String COMPONENT_ID = "datasource0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new SimulatorDatasourceCsvDirectImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setFactor(1) //
						.setFormat(CsvFormat.ENGLISH) //
						.setSource("") //
						.setTimeDelta(0) //
						.build()) //
				.next(new TestCase()) //
		;
	}
}
