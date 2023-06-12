package io.openems.edge.simulator.datasource.csv.predefined;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.simulator.CsvFormat;

public class SimulatorDatasourceCsvPredefinedImplTest {

	private static final String COMPONENT_ID = "datasource0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new SimulatorDatasourceCsvPredefinedImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setFactor(1) //
						.setFormat(CsvFormat.ENGLISH) //
						.setSource(Source.H0_HOUSEHOLD_SUMMER_WEEKDAY_NON_REGULATED_CONSUMPTION) //
						.setTimeDelta(0) //
						.build()) //
				.next(new TestCase()) //
		;
	}

}
