package io.openems.edge.simulator.pvinverter;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.simulator.datasource.csv.direct.SimulatorDatasourceCsvDirectImpl;

public class SimulatorPvInverterImplTest {

	@Test
	void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorPvInverterImpl()) //
				.addReference("datasource", new SimulatorDatasourceCsvDirectImpl()) //
				.activate(MyConfig.create() //
						.setId("pvInverter0") //
						.setDatasourceId("datasource0") //
						.build()) //
				// .next(new TestCase()); // TODO requires DummyDatasource
				.deactivate();
	}
}
