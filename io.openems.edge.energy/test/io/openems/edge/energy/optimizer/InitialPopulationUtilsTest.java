package io.openems.edge.energy.optimizer;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.energy.optimizer.InitialPopulationUtils.generateFromPreviousSchedule;
import static io.openems.edge.energy.optimizer.InitialPopulationUtils.generateInitialPopulation;
import static io.openems.edge.energy.optimizer.InitialPopulationUtils.getScheduleFromPreviousResult;
import static io.openems.edge.energy.optimizer.SimulatorTest.DUMMY_SIMULATOR;
import static io.openems.edge.energy.optimizer.SimulatorTest.ESH_TIME_OF_USE_TARIFF_CTRL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.energy.api.handler.DifferentModes.Period.Transition;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.Fitness;

public class InitialPopulationUtilsTest {

	private static final ZonedDateTime TIME = ZonedDateTime.now(createDummyClock());

	public static final SimulationResult DUMMY_PREVIOUS_RESULT = new SimulationResult(new Fitness(),
			ImmutableSortedMap.of(), //
			ImmutableMap.<EnergyScheduleHandler.WithDifferentModes, ImmutableSortedMap<ZonedDateTime, Transition>>builder() //
					.put(ESH_TIME_OF_USE_TARIFF_CTRL, ImmutableSortedMap.<ZonedDateTime, Transition>naturalOrder() //
							.put(TIME.plusHours(0).plusMinutes(00), mode(2)) //
							.put(TIME.plusHours(0).plusMinutes(15), mode(2)) //
							.put(TIME.plusHours(0).plusMinutes(30), mode(2)) //
							.build()) //
					.build(), //
			ImmutableSet.of());

	@Test
	public void testGenerateFromPreviousSchedule() {
		final var simulator = DUMMY_SIMULATOR;
		final var codec = EshCodec.of(simulator.goc, DUMMY_PREVIOUS_RESULT, false);
		var previousSchedule = getScheduleFromPreviousResult(ESH_TIME_OF_USE_TARIFF_CTRL, codec.previousResult);
		var ip = generateFromPreviousSchedule(simulator.goc, ESH_TIME_OF_USE_TARIFF_CTRL, previousSchedule);
		assertTrue(ip.toString().startsWith("InitialPopulation{[2, 2, 2, 0,"));
	}

	@Test
	public void testGenerateInitialPopulationNotFixed() {
		final var simulator = DUMMY_SIMULATOR;
		final var codec = EshCodec.of(simulator.goc, DUMMY_PREVIOUS_RESULT, false);
		var schedules = generateInitialPopulation(codec).population();
		assertEquals(12, schedules.size()); // variations of 6 x 2

		assertTrue(schedules.get(0).get(0).toString().startsWith("[[0],[0],[0],"));
		assertTrue(schedules.get(0).get(1).toString().startsWith("[[1],[1],[1],"));

		assertTrue(schedules.get(1).get(0).toString().startsWith("[[0],[0],[0],"));
		assertTrue(schedules.get(1).get(1).toString().startsWith("[[0],[1],[1],"));

		assertTrue(schedules.get(2).get(0).toString().startsWith("[[2],[2],[2],"));
		assertTrue(schedules.get(2).get(1).toString().startsWith("[[1],[1],[1],"));
	}

	@Test
	public void testGenerateInitialPopulationFixed() {
		final var simulator = DUMMY_SIMULATOR;
		final var codec = EshCodec.of(simulator.goc, DUMMY_PREVIOUS_RESULT, true);
		var schedules = generateInitialPopulation(codec).population();
		assertEquals(12, schedules.size()); // variations of 6 x 2

		assertTrue(schedules.get(0).get(0).toString().startsWith("[[2],[0],[0],"));
		assertTrue(schedules.get(0).get(1).toString().startsWith("[[1],[1],[1],"));

		assertTrue(schedules.get(1).get(0).toString().startsWith("[[2],[0],[0],"));
		assertTrue(schedules.get(1).get(1).toString().startsWith("[[0],[1],[1],"));

		assertTrue(schedules.get(2).get(0).toString().startsWith("[[2],[2],[2],"));
		assertTrue(schedules.get(2).get(1).toString().startsWith("[[1],[1],[1],"));
	}

	protected static Transition mode(int mode) {
		return new Transition(mode, 0., null, 0);
	}
}
