package io.openems.edge.simulator.ess.singlephase.reacting;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.api.SinglePhase;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;
import io.openems.edge.simulator.datasource.csv.direct.SimulatorDatasourceCsvDirectImplTest;

public class SimulatorEssSinglePhaseReactingImplTest {

	private static final String ESS_ID = "ess0";
	private static final String DATASOURCE_ID = "datasource0";

	@Test
	public void test() throws OpenemsException, Exception {
		new ManagedSymmetricEssTest(new SimulatorEssSinglePhaseReactingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("datasource", SimulatorDatasourceCsvDirectImplTest.create("datasource0", "123")) //
				.addReference("power", new DummyPower()) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setDatasourceId(DATASOURCE_ID) //
						.setCapacity(10_000) //
						.setMaxApparentPower(10_000) //
						.setInitialSoc(50) //
						.setGridMode(GridMode.ON_GRID) //
						.setPhase(SinglePhase.L1) //
						.build()) //
				.next(new TestCase()); //
	}

}
