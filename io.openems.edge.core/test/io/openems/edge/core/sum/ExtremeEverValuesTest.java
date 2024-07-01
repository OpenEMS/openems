package io.openems.edge.core.sum;

import static io.openems.edge.common.test.TestUtils.activateNextProcessImage;
import static io.openems.edge.common.test.TestUtils.withValue;
import static io.openems.edge.core.sum.ExtremeEverValues.Range.NEGATIVE;
import static io.openems.edge.core.sum.ExtremeEverValues.Range.POSTIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class ExtremeEverValuesTest {

	@Test
	public void test() throws OpenemsException, Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T20:00:00.00Z"), ZoneOffset.UTC);
		var cm = new DummyConfigurationAdmin();
		var sum = new SumImpl();
		new ComponentTest(sum) //
				.addReference("cm", cm) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setGridMinActivePower(0) //
						.setIgnoreStateComponents() //
						.build()); //

		var sut = ExtremeEverValues.create(clock, "Core.Sum") //
				.add(Sum.ChannelId.GRID_MIN_ACTIVE_POWER, "gridMinActivePower", //
						NEGATIVE, Sum.ChannelId.GRID_ACTIVE_POWER) //
				.add(Sum.ChannelId.GRID_MAX_ACTIVE_POWER, "gridMaxActivePower", //
						POSTIVE, Sum.ChannelId.GRID_ACTIVE_POWER) //
				.add(Sum.ChannelId.PRODUCTION_MAX_ACTIVE_POWER, "productionMaxActivePower", //
						POSTIVE, Sum.ChannelId.PRODUCTION_ACTIVE_POWER) //
				.add(Sum.ChannelId.CONSUMPTION_MAX_ACTIVE_POWER, "consumptionMaxActivePower", //
						POSTIVE, Sum.ChannelId.CONSUMPTION_ACTIVE_POWER) //
				.build();

		var context = sum.getComponentContext();
		sut.initializeFromContext(context);

		// Before setting the Channel
		assertNull(sum.getGridMinActivePower().get());
		assertEquals(0, context.getProperties().get("gridMinActivePower"));

		// Update Channel; not the config
		withValue(sum, Sum.ChannelId.GRID_ACTIVE_POWER, -100);
		sut.update(sum, cm);
		activateNextProcessImage(sum);

		assertEquals(-100, (int) sum.getGridMinActivePower().get());
		assertEquals(0, getProperty(cm, "gridMinActivePower"));

		// Still the same
		clock.leap(24, ChronoUnit.HOURS);
		withValue(sum, Sum.ChannelId.GRID_ACTIVE_POWER, -100);
		sut.update(sum, cm);
		activateNextProcessImage(sum);
		assertEquals(0, getProperty(cm, "gridMinActivePower"));

		// 24 hours passed -> update config
		clock.leap(1, ChronoUnit.SECONDS);
		withValue(sum, Sum.ChannelId.GRID_ACTIVE_POWER, -101);
		sut.update(sum, cm);
		activateNextProcessImage(sum);
		assertEquals(-101, (int) sum.getGridMinActivePower().get());
		assertEquals(-101, getProperty(cm, "gridMinActivePower"));

		// Update Channel; not the config
		clock.leap(1, ChronoUnit.SECONDS);
		withValue(sum, Sum.ChannelId.GRID_ACTIVE_POWER, -101);
		sut.update(sum, cm);
		activateNextProcessImage(sum);
		assertEquals(-101, (int) sum.getGridMinActivePower().get());
		assertEquals(-101, getProperty(cm, "gridMinActivePower"));
	}

	private static int getProperty(DummyConfigurationAdmin cm, String property) throws IOException {
		return (int) (cm.getConfiguration("Core.Sum").getProperties().get(property));
	}
}
