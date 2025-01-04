package io.openems.edge.energy.optimizer;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.energy.optimizer.EshCodec.schedulesToStringArray;
import static io.openems.edge.energy.optimizer.InitialPopulation.variationsFromExistingSimulationResult;
import static io.openems.edge.energy.optimizer.InitialPopulation.variationsOfAllStatesDefault;
import static io.openems.edge.energy.optimizer.SimulatorTest.DUMMY_SIMULATOR;
import static io.openems.edge.energy.optimizer.SimulatorTest.ESH_TIME_OF_USE_TARIFF_CTRL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyScheduleHandler.WithDifferentStates.Period.Transition;

public class InitialPopulationTest {

	private static final ZonedDateTime TIME = ZonedDateTime.now(createDummyClock());

	public static final SimulationResult PREVIOUS_RESULT = new SimulationResult(0., ImmutableSortedMap.of(), //
			ImmutableMap.<EnergyScheduleHandler.WithDifferentStates<?, ?>, ImmutableSortedMap<ZonedDateTime, Transition>>builder() //
					.put(ESH_TIME_OF_USE_TARIFF_CTRL, ImmutableSortedMap.<ZonedDateTime, Transition>naturalOrder() //
							.put(TIME.plusHours(0).plusMinutes(00), state(2)) //
							.put(TIME.plusHours(0).plusMinutes(15), state(2)) //
							.put(TIME.plusHours(0).plusMinutes(30), state(2)) //
							.build()) //
					.build());

	@Test
	public void testAllStatesDefaultCurrentPeriodFixed() {
		final var simulator = DUMMY_SIMULATOR;
		var schedules = variationsOfAllStatesDefault(simulator.gsc, PREVIOUS_RESULT, true).toList();
		assertEquals(6, schedules.size()); // variations of 3 x 2

		var s = schedulesToStringArray(schedules);
		assertTrue(s[0].startsWith("[[2,0],[0,0],[0,1],"));
		assertTrue(s[1].startsWith("[[2,1],[0,1],[0,1],"));
		assertTrue(s[2].startsWith("[[2,0],[1,0],[0,1],"));
		assertTrue(s[3].startsWith("[[2,1],[1,1],[0,1],"));
		assertTrue(s[4].startsWith("[[2,0],[2,0],[0,1],"));
		assertTrue(s[5].startsWith("[[2,1],[2,1],[0,1],"));
	}

	@Test
	public void testAllStatesDefaultCurrentPeriodNotFixed() {
		final var simulator = DUMMY_SIMULATOR;
		var schedules = variationsOfAllStatesDefault(simulator.gsc, PREVIOUS_RESULT, false).toList();
		assertEquals(6, schedules.size()); // variations of 3 x 2

		var s = schedulesToStringArray(schedules);
		assertTrue(s[0].startsWith("[[0,0],[0,0],[0,1],"));
		assertTrue(s[1].startsWith("[[0,1],[0,1],[0,1],"));
		assertTrue(s[2].startsWith("[[1,0],[1,0],[0,1],"));
		assertTrue(s[3].startsWith("[[1,1],[1,1],[0,1],"));
		assertTrue(s[4].startsWith("[[2,0],[2,0],[0,1],"));
		assertTrue(s[5].startsWith("[[2,1],[2,1],[0,1],"));
	}

	@Test
	public void testVariationsFromExistingSimulationResultCurrentPeriodFixed() {
		final var simulator = DUMMY_SIMULATOR;
		var schedules = variationsFromExistingSimulationResult(simulator.gsc, PREVIOUS_RESULT, true).toList();
		assertEquals(6, schedules.size()); // variations of 3 x 2

		var s = schedulesToStringArray(schedules);
		assertTrue(s[0].startsWith("[[2,0],[0,0],[2,1],"));
		assertTrue(s[1].startsWith("[[2,1],[0,1],[2,1],"));
		assertTrue(s[2].startsWith("[[2,0],[1,0],[2,1],"));
		assertTrue(s[3].startsWith("[[2,1],[1,1],[2,1],"));
		assertTrue(s[4].startsWith("[[2,0],[2,0],[2,1],"));
		assertTrue(s[5].startsWith("[[2,1],[2,1],[2,1],"));
	}

	@Test
	public void testVariationsFromExistingSimulationResultCurrentPeriodNotFixed() {
		final var simulator = DUMMY_SIMULATOR;
		var schedules = variationsFromExistingSimulationResult(simulator.gsc, PREVIOUS_RESULT, false).toList();
		assertEquals(6, schedules.size()); // variations of 3 x 2

		var s = schedulesToStringArray(schedules);
		assertTrue(s[0].startsWith("[[0,0],[0,0],[2,1],"));
		assertTrue(s[1].startsWith("[[0,1],[0,1],[2,1],"));
		assertTrue(s[2].startsWith("[[1,0],[1,0],[2,1],"));
		assertTrue(s[3].startsWith("[[1,1],[1,1],[2,1],"));
		assertTrue(s[4].startsWith("[[2,0],[2,0],[2,1],"));
		assertTrue(s[5].startsWith("[[2,1],[2,1],[2,1],"));
	}

	protected static Transition state(int state) {
		return new Transition(state, 0., null, 0);
	}
}
