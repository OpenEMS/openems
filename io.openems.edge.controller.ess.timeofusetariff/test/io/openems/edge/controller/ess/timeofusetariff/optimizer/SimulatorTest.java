package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRICES_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.calculateCost;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.interpolateArray;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.toEnergy;
import static java.lang.Math.abs;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.jenetics.util.RandomRegistry;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.Params;

public class SimulatorTest {

	@Before
	public void before() {
		// Make reproducable results
		System.setProperty("io.jenetics.util.defaultRandomGenerator", "Random");
		RandomRegistry.random(new Random(123));
	}

	@Test
	@Ignore
	public void testBalancing888d20231106() {
		var p = createParams888d20231106(StateMachine.BALANCING);
		var schedule = Simulator.getBestSchedule(p);

		// Cost: 1,9436 €
		// Grid Buy: 7794 Wh
		// Grid Sell: 1221 Wh

		logSchedule(p, schedule);
		logCost(p, schedule);
		logGridEnergy(p, schedule);
	}

	@Test
	@Ignore
	public void testDelayDischarge888d20231106() {
		var p = createParams888d20231106(ControlMode.DELAY_DISCHARGE.states);
		var schedule = Simulator.getBestSchedule(p);

		// Cost: 1,7103 €
		// Grid Buy: 7794 Wh
		// Grid Sell: 2270 Wh

		logSchedule(p, schedule);
		logCost(p, schedule);
		logGridEnergy(p, schedule);
	}

	@Test
	@Ignore
	public void testCharge888d20231106() {
		var p = createParams888d20231106(ControlMode.CHARGE_CONSUMPTION.states);
		var schedule = Simulator.getBestSchedule(p);

		// Cost: 1,3917 €
		// Grid Buy: 7799 Wh
		// Grid Sell: 2608 Wh

		logSchedule(p, schedule);
		logCost(p, schedule);
		logGridEnergy(p, schedule);
	}

	private static Params createParams888d20231106(StateMachine... states) {
		return new Simulator.Params(//
				/* start */ ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), //
				/* essEnergy */ (int) (22000 * 0.1), //
				/* essCapacity */ 22000, //
				/* essMaxEnergyPerPeriod */ toEnergy(10000), //
				/* maxBuyFromGrid */ toEnergy(3000), //
				stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray(),
				stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray(),
				hourlyToQuarterly(interpolateArray(PRICES_888_20231106)), //
				states);
	}

	private static void logCost(Params p, StateMachine[] schedule) {
		System.out.println("Cost: " //
				+ "%.4f €".formatted(calculateCost(p, schedule) / 1_000_000 /* convert to € */));
	}

	private static void logSchedule(Params p, StateMachine[] schedule) {
		var debug = new ArrayList<String>();
		debug.add(Period.header());
		calculateCost(p, schedule, dp -> debug.add(dp.toString()));
		System.out.println(debug.stream().collect(joining("\n")));
	}

	private static void logGridEnergy(Params p, StateMachine[] schedule) {
		var gridBuy = new AtomicInteger();
		var gridSell = new AtomicInteger();

		calculateCost(p, schedule, dp -> {
			if (dp.grid() > 0) {
				gridBuy.getAndUpdate(v -> v + dp.grid());
			} else {
				gridSell.getAndUpdate(v -> v + abs(dp.grid()));
			}
		});
		System.out.println("Grid Buy: " + gridBuy.get() + " Wh");
		System.out.println("Grid Sell: " + gridSell.get() + " Wh");
	}

	/**
	 * Convert hourly values to quarterly.
	 * 
	 * @param values hourly values
	 * @return quarterly values
	 */
	private static float[] hourlyToQuarterly(float[] values) {
		var result = new float[values.length * 4];
		for (var i = 0; i < values.length; i++) {
			result[(i * 4)] = values[i];
			result[(i * 4) + 1] = values[i];
			result[(i * 4) + 2] = values[i];
			result[(i * 4) + 3] = values[i];
		}
		return result;
	}
}