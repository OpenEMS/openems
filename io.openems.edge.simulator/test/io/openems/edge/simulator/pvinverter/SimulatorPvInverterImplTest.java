package io.openems.edge.simulator.pvinverter;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.simulator.datasource.csv.direct.SimulatorDatasourceCsvDirectImpl;

public class SimulatorPvInverterImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorPvInverterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("datasource", new SimulatorDatasourceCsvDirectImpl()) //
				.activate(MyConfig.create() //
						.setId("pvInverter0") //
						.setDatasourceId("datasource0") //
						.build()); //
		// .next(new TestCase()); // TODO requires DummyDatasource
	}

}
