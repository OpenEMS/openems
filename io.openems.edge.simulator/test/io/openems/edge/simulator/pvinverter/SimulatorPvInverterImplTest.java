package io.openems.edge.simulator.pvinverter;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.simulator.datasource.csv.direct.SimulatorDatasourceCsvDirectImpl;

public class SimulatorPvInverterImplTest {

	private static final String COMPONENT_ID = "pvInverter0";
	private static final String DATASOURCE_ID = "datasource0";

	@Test
	public void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorPvInverterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("datasource", new SimulatorDatasourceCsvDirectImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setDatasourceId(DATASOURCE_ID) //
						.build()); //
		// .next(new TestCase()); // TODO requires DummyDatasource
	}

}
