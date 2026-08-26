package io.openems.edge.simulator.predictor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.prediction.LogVerbosity;
import io.openems.edge.simulator.datasource.csv.direct.SimulatorDatasourceCsvDirectImplTest;

public class SimulatorPredictorImplTest {

	private static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");

	@Test
	void test() throws OpenemsException, Exception {
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(946684800), ZoneId.of("UTC"));
		final var datasource = SimulatorDatasourceCsvDirectImplTest.create("datasource0", """
				10
				20
				30
				40
				""");
		final var sut = new SimulatorPredictorImpl();
		new ComponentTest(sut) //
				.addReference("datasource", datasource) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("predictor0") //
						.setDatasourceId("datasource0") //
						.setChannelAddresses(SUM_PRODUCTION.toString()) //
						.setLogVerbosity(LogVerbosity.REQUESTED_PREDICTIONS) //
						.build()) //
				.deactivate();

		var p = sut.createNewPrediction(SUM_PRODUCTION);
		assertEquals(192, p.asArray().length);
		assertEquals(Integer.valueOf(20), p.asArray()[0]);
		assertEquals(Integer.valueOf(23), p.asArray()[1]);
		assertEquals(Integer.valueOf(27), p.asArray()[2]);
		assertEquals(ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant(), p.getFirstTime());
	}
}
