package io.openems.edge.simulator.predictor;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.predictor.api.prediction.LogVerbosity;
import io.openems.edge.simulator.datasource.csv.direct.SimulatorDatasourceCsvDirectImplTest;

public class SimulatorPredictorImplTest {

	private static final TimeLeapClock CLOCK = new TimeLeapClock(Instant.ofEpochSecond(946684800), ZoneId.of("UTC"));
	private static final String COMPONENT_ID = "predictor0";
	private static final String DATASOURCE_ID = "datasource0";
	private static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");

	@Test
	public void test() throws OpenemsException, Exception {
		final var datasource = SimulatorDatasourceCsvDirectImplTest.create(DATASOURCE_ID, """
				10
				20
				30
				40
				""");
		final var sut = new SimulatorPredictorImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("datasource", datasource) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setDatasourceId(DATASOURCE_ID) //
						.setChannelAddresses(SUM_PRODUCTION.toString()) //
						.setLogVerbosity(LogVerbosity.REQUESTED_PREDICTIONS) //
						.build()); //

		var p = sut.createNewPrediction(SUM_PRODUCTION);
		assertEquals(192, p.asArray().length);
		assertEquals(Integer.valueOf(20), p.asArray()[0]);
		assertEquals(Integer.valueOf(23), p.asArray()[1]);
		assertEquals(Integer.valueOf(27), p.asArray()[2]);
		assertEquals(ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), p.valuePerQuarter.firstKey());
	}

}
