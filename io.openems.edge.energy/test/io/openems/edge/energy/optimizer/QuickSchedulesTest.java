package io.openems.edge.energy.optimizer;

import static io.openems.edge.energy.optimizer.QuickSchedules.fromExistingSimulationResult;
import static io.openems.edge.energy.optimizer.QuickSchedules.variationsFromExistingSimulationResult;
import static io.openems.edge.energy.optimizer.QuickSchedules.variationsOfAllStatesDefault;
import static io.openems.edge.energy.optimizer.SimulatorTest.ESH_TIME_OF_USE_TARIFF_CTRL;
import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyScheduleHandler.WithDifferentStates.Period.Transition;

public class QuickSchedulesTest {

	private static final ZonedDateTime TIME = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

	@Test
	public void testAllStatesDefault() {
		final var gsc = SimulatorTest.DUMMY_GSC;
		gsc.initializeEnergyScheduleHandlers();
		var gts = variationsOfAllStatesDefault(gsc).toList();
		assertEquals(6, gts.size());
		var gt = gts.get(0);
		assertEquals(0, gt.get(0).get(1).allele().intValue());
	}

	@Test
	public void testFromExistingSimulationResult() {
		final var gsc = SimulatorTest.DUMMY_GSC;
		final var previousResult = new SimulationResult(0., ImmutableMap.of(), //
				ImmutableMap.<EnergyScheduleHandler.WithDifferentStates<?, ?>, ImmutableSortedMap<ZonedDateTime, Transition>>builder() //
						.put(ESH_TIME_OF_USE_TARIFF_CTRL, ImmutableSortedMap.<ZonedDateTime, Transition>naturalOrder() //
								.put(TIME.plusHours(0).plusMinutes(00), state(2)) //
								.build()) //
						.build());
		gsc.initializeEnergyScheduleHandlers();
		var gt = fromExistingSimulationResult(gsc, previousResult);
		assertEquals(2, gt.get(0).get(0).allele().intValue());
	}

	@Test
	public void testVariationsFromExistingSimulationResult() {
		final var gsc = SimulatorTest.DUMMY_GSC;
		final var previousResult = new SimulationResult(0., ImmutableMap.of(), //
				ImmutableMap.<EnergyScheduleHandler.WithDifferentStates<?, ?>, ImmutableSortedMap<ZonedDateTime, Transition>>builder() //
						.put(ESH_TIME_OF_USE_TARIFF_CTRL, ImmutableSortedMap.<ZonedDateTime, Transition>naturalOrder() //
								.put(TIME.plusHours(0).plusMinutes(00), state(2)) //
								.put(TIME.plusHours(0).plusMinutes(15), state(2)) //
								.build()) //
						.build());
		gsc.initializeEnergyScheduleHandlers();
		var gts = variationsFromExistingSimulationResult(gsc, previousResult).toList();
		assertEquals(6, gts.size());
		var gt = gts.get(0);
		assertEquals(2, gt.get(0).get(1).allele().intValue());
	}

	protected static Transition state(int state) {
		return new Transition(state, 0., null, 0);
	}
}
