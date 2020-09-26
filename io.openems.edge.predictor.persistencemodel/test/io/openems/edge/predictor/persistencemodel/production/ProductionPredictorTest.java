package io.openems.edge.predictor.persistencemodel.production;

import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.predictor.api.HourlyPrediction;

public class ProductionPredictorTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String SUM_ID = "_sum";
	private static final ChannelAddress SUM_PRODUCTION_ACTIVE_ENERGY = new ChannelAddress(SUM_ID,
			"ProductionActiveEnergy");

	@Test
	public void test() throws Exception {
		final TimeLeapClock clock = new TimeLeapClock();
		final ProductionPredictor predictor = new ProductionPredictor();
		new ComponentTest(predictor) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummySum()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.build())
				.next(new TestCase() //
						.input(SUM_PRODUCTION_ACTIVE_ENERGY, 1000))
				.next(new TestCase() //
						.timeleap(clock, 1, ChronoUnit.MINUTES) //
						.input(SUM_PRODUCTION_ACTIVE_ENERGY, 1100))
				.next(new TestCase() //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.input(SUM_PRODUCTION_ACTIVE_ENERGY, 2000))
				.next(new TestCase() //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.input(SUM_PRODUCTION_ACTIVE_ENERGY, 4000))
				.next(new TestCase() //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.input(SUM_PRODUCTION_ACTIVE_ENERGY, 5500));

		HourlyPrediction p = predictor.get24hPrediction();
		assertEquals(p.getStart(), ZonedDateTime.now(clock).withNano(0).withMinute(0).withSecond(0));

		Integer[] v = p.getValues();
		assertEquals(v.length, 24);

		assertEquals(null, v[0]);
		assertEquals(null, v[1]);
		assertEquals(null, v[2]);
		assertEquals(null, v[3]);
		assertEquals(null, v[4]);
		assertEquals(null, v[5]);
		assertEquals(null, v[6]);
		assertEquals(null, v[7]);
		assertEquals(null, v[8]);
		assertEquals(null, v[9]);
		assertEquals(null, v[10]);
		assertEquals(null, v[11]);
		assertEquals(null, v[12]);
		assertEquals(null, v[13]);
		assertEquals(null, v[14]);
		assertEquals(null, v[15]);
		assertEquals(null, v[16]);
		assertEquals(null, v[17]);
		assertEquals(null, v[18]);
		assertEquals(null, v[19]);
		assertEquals(null, v[20]);
		assertEquals(Integer.valueOf(1000), v[21]);
		assertEquals(Integer.valueOf(2000), v[22]);
		assertEquals(Integer.valueOf(1500), v[23]);
	}

}
