package io.openems.edge.simulator.ess.asymmetric.reacting;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;
import io.openems.edge.simulator.datasource.csv.direct.SimulatorDatasourceCsvDirectImplTest;

public class SimulatorEssAsymmetricReactingImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		new ManagedSymmetricEssTest(new SimulatorEssAsymmetricReactingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("datasource", SimulatorDatasourceCsvDirectImplTest.create("datasource0", "123")) //
				.addReference("power", new DummyPower()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setDatasourceId("datasource0") //
						.setCapacity(10_000) //
						.setMaxApparentPower(10_000) //
						.setInitialSoc(50) //
						.setGridMode(GridMode.ON_GRID) //
						.build()) //
				.next(new TestCase()); //
	}
}
