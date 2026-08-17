package io.openems.edge.simulator.ess.singlephase.reacting;

import static io.openems.edge.common.sum.GridMode.ON_GRID;
import static io.openems.edge.common.type.Phase.SinglePhase.L1;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;
import io.openems.edge.simulator.datasource.csv.direct.SimulatorDatasourceCsvDirectImplTest;

public class SimulatorEssSinglePhaseReactingImplTest {

	@Test
	void test() throws OpenemsException, Exception {
		new ManagedSymmetricEssTest(new SimulatorEssSinglePhaseReactingImpl()) //
				.addReference("datasource", SimulatorDatasourceCsvDirectImplTest.create("datasource0", "123")) //
				.addReference("power", new DummyPower()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setDatasourceId("datasource0") //
						.setCapacity(10_000) //
						.setMaxApparentPower(10_000) //
						.setInitialSoc(50) //
						.setGridMode(ON_GRID) //
						.setPhase(L1) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
