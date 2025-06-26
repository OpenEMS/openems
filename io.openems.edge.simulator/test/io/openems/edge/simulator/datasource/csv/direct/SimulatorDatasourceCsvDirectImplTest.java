package io.openems.edge.simulator.datasource.csv.direct;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.simulator.CsvFormat;

public class SimulatorDatasourceCsvDirectImplTest {

	private static final String COMPONENT_ID = "datasource0";

	private static ComponentTest createTest(String componentId, String source) throws Exception {
		return new ComponentTest(new SimulatorDatasourceCsvDirectImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(componentId) //
						.setFactor(1) //
						.setFormat(CsvFormat.ENGLISH) //
						.setSource(source) //
						.setTimeDelta(0) //
						.build()); //
	}

	/**
	 * Creates and activates a {@link SimulatorDatasourceCsvDirectImpl}.
	 * 
	 * @param componentId the Component-ID
	 * @param source      the data
	 * @return a {@link SimulatorDatasourceCsvDirectImpl} object
	 * @throws Exception on error
	 */
	public static SimulatorDatasourceCsvDirectImpl create(String componentId, String source) throws Exception {
		return (SimulatorDatasourceCsvDirectImpl) createTest(componentId, source).getSut();
	}

	@Test
	public void test() throws Exception {
		createTest(COMPONENT_ID, "") //
				.next(new TestCase()) //
		;
	}
}
