package io.openems.edge.simulator.meter.grid.acting;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.simulator.datasource.csv.direct.SimulatorDatasourceCsvDirectImpl;

public class SimulatorGridMeterActingImplTest {

	private static final String COMPONENT_ID = "meter0";
	private static final String DATASOURCE_ID = "datasource0";

	@Test
	public void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorGridMeterActingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("datasource", new SimulatorDatasourceCsvDirectImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setStartTime("")//
						.needFrequencyStepResponse(false)//
						.setDatasourceId(DATASOURCE_ID) //
						.build()); //
		// .next(new TestCase()); // TODO requires DummyDatasource
	}

	@Test
	public void test1() throws OpenemsException, Exception {
		final var clock = new TimeLeapClock(Instant.parse("2024-01-29T19:05:00Z"), ZoneOffset.UTC);
		new ComponentTest(new SimulatorGridMeterActingImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("datasource", new SimulatorDatasourceCsvDirectImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setStartTime("")//
						.needFrequencyStepResponse(true)//
						.setDatasourceId(DATASOURCE_ID) //
						.build()); //
		// .next(new TestCase()); // TODO requires DummyDatasource
	}
}
