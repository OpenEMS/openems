package io.openems.edge.simulator.meter.production.acting;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.simulator.datasource.csv.direct.SimulatorDatasourceCsvDirectImpl;

public class SimulatorProductionMeterActingImplTest {

	private static final String COMPONENT_ID = "meter0";
	private static final String DATASOURCE_ID = "datasource0";

	@Test
	void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorProductionMeterActingImpl()) //
				.addReference("datasource", new SimulatorDatasourceCsvDirectImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setDatasourceId(DATASOURCE_ID) //
						.build()) //
				// .next(new TestCase()); // TODO requires DummyDatasource
				.deactivate();
	}
}
