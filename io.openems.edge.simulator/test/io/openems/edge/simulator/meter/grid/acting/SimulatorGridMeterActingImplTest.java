package io.openems.edge.simulator.meter.grid.acting;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.simulator.datasource.api.DummyDatasource;

public class SimulatorGridMeterActingImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		new ComponentTest(new SimulatorGridMeterActingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("datasource", new DummyDatasource(123)) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setStartTime("")//
						.needFrequencyStepResponse(false)//
						.setDatasourceId("datasource0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	public void test1() throws OpenemsException, Exception {
		final var clock = new TimeLeapClock(Instant.parse("2024-01-29T19:05:00Z"), ZoneOffset.UTC);
		new ComponentTest(new SimulatorGridMeterActingImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("datasource", new DummyDatasource(123)) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setStartTime("")//
						.needFrequencyStepResponse(true)//
						.setDatasourceId("datasource0") //
						.build()) //
				.deactivate();
	}
}
