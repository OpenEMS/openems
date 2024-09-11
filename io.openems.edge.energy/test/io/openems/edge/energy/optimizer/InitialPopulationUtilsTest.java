package io.openems.edge.energy.optimizer;

import static io.openems.edge.energy.optimizer.InitialPopulationUtils.buildInitialPopulation;
import static io.openems.edge.energy.optimizer.SimulatorTest.ESH_TIME_OF_USE_TARIFF_CTRL;
import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyScheduleHandler.WithDifferentStates.Period.Transition;

public class InitialPopulationUtilsTest {

	private static final ZonedDateTime TIME = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

	@Test
	public void testBuildInitialPopulation() {
		final var gsc = SimulatorTest.DUMMY_GSC;
		final var previousResult = new SimulationResult(0., ImmutableMap.of(), //
				ImmutableMap.<EnergyScheduleHandler.WithDifferentStates<?, ?>, ImmutableSortedMap<ZonedDateTime, Transition>>builder() //
						.put(ESH_TIME_OF_USE_TARIFF_CTRL, ImmutableSortedMap.<ZonedDateTime, Transition>naturalOrder() //
								.put(TIME.plusHours(0).plusMinutes(00), state(2)) //
								.build()) //
						.build());

		// Initialize EnergyScheduleHandlers
		for (var esh : gsc.handlers()) {
			esh.onBeforeSimulation(gsc);
		}

		var lgt = buildInitialPopulation(gsc, previousResult);
		assertEquals(2, lgt.size());

		// Last-Schedules
		assertEquals(2, lgt.get(1).get(0).get(0).allele().intValue()); // from previousResult
		assertEquals(0, lgt.get(1).get(0).get(1).allele().intValue()); // unknown before -> default
	}

	protected static Transition state(int state) {
		return new Transition(state, 0., null, 0);
	}
}
