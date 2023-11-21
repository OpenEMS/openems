package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRICES_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.calculateCost;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.isBadSolution;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.interpolateArray;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.toEnergy;
import static java.lang.Math.abs;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

public class SimulatorTest {

	private static ZonedDateTime TIME = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

	@Before
	public void before() {
		// Make reproducable results
		System.setProperty("io.jenetics.util.defaultRandomGenerator", "Random");
		RandomRegistry.random(new Random(123));
	}

	@Test
	public void testGetInitialPopulation() {
		Simulator.getInitialPopulation(Params.create() //
				.time(TIME) //
				.essAvailableEnergy((int) (22000 * 0.1)) //
				.essCapacity(22000) //
				.essMaxEnergyPerPeriod(toEnergy(10000)) //
				.maxBuyFromGrid(toEnergy(8000)) //
				.productions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.consumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.prices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
				.states(StateMachine.BALANCING, StateMachine.DELAY_DISCHARGE, StateMachine.CHARGE) //
				.build());
	}

	@Test
	public void testGetBestSchedule0() {
		Simulator.getBestSchedule(Params.create() //
				.time(TIME) //
				.essAvailableEnergy((int) (22000 * 0.1)) //
				.essCapacity(22000) //
				.essMaxEnergyPerPeriod(toEnergy(10000)) //
				.maxBuyFromGrid(toEnergy(8000)) //
				.productions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.consumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.prices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
				.states(StateMachine.BALANCING, StateMachine.DELAY_DISCHARGE, StateMachine.CHARGE) //
				.build(), //

				/* populationSize */ 1, //
				/* limit */ 1);
	}

	@Test
	@Ignore
	public void testOnlyBalancing888d20231106() {
		var p = createParams888d20231106(StateMachine.BALANCING);
		var schedule = Simulator.getBestSchedule(p);

		// Cost: 1,9436 €
		// Grid Buy: 7794 Wh
		// Grid Sell: 1221 Wh

		logSchedule(p, schedule);
		logCost(p, schedule);
		logGridEnergy(p, schedule);

		assertArrayEquals(
				new int[] { 77, 71, 96, 117, 120, 96, 116, 112, 72, 83, 86, 78, 446, 83, 75, 64, 93, 89, 69, 77, 77, 3,
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -80, -208, -337, 85, -552, -242, -293, -997, -976, -970,
						-1657, -1055, -861, -1528, -1773, -2266, -1980, -1310, -1487, -1546, -1099, -868, 0, 308, -218,
						48, 844, 171, 380, 755, 381, 632, 1265, 744, 764, 1210, 1048, 1118, 1187, 1219, 309, 348, 356,
						280, 841, 1022, 104, 109, 808, 376, 273, 463, 91, 157, 523, 138, 278, 452, 805, 407, 332, 66 },
				getEssChargeDischarges(p, schedule));
	}

	@Test
	@Ignore
	public void testOnlyDelayDischarge888d20231106() {
		var p = createParams888d20231106(StateMachine.DELAY_DISCHARGE);
		var schedule = Simulator.getBestSchedule(p);

		// Cost: 1,9436 €
		// Grid Buy: 7794 Wh
		// Grid Sell: 1221 Wh

		logSchedule(p, schedule);
		logCost(p, schedule);
		logGridEnergy(p, schedule);

		assertArrayEquals(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				getEssChargeDischarges(p, schedule));
	}

	@Test
	@Ignore
	public void testOnlyCharge888d20231106() {
		var p = createParams888d20231106(StateMachine.CHARGE);
		var schedule = Simulator.getBestSchedule(p);

		// Cost: 1,9436 €
		// Grid Buy: 7794 Wh
		// Grid Sell: 1221 Wh

		logSchedule(p, schedule);
		logCost(p, schedule);
		logGridEnergy(p, schedule);

		assertArrayEquals(new int[] { -673, -679, -654, -633, -630, -654, -634, -638, -678, -667, -664, -672, -304,
				-667, -675, -686, -657, -661, -681, -673, -673, -647, -652, -676, -22, 0, 0, 0, -632, 0, 0, -318, 0,
				-705, -830, -958, -807, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				getEssChargeDischarges(p, schedule));
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

		assertArrayEquals(
				new int[] { 77, 71, 96, 117, 120, 96, 116, 112, 72, 83, 86, 78, 446, 83, 75, 64, 93, 89, 69, 77, 77, 3,
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -80, 0, 0, 80, -552, -242, 0, -997, -976, -970, -1657,
						-1055, -861, 0, -1773, -2266, -1980, -1310, -1487, -1546, -1099, -1297, -792, 308, -218, 48,
						844, 171, 380, 755, 381, 632, 1265, 744, 764, 1210, 1048, 1118, 1187, 1219, 309, 348, 356, 280,
						841, 1022, 104, 109, 808, 376, 273, 463, 91, 157, 523, 138, 278, 452, 805, 407, 332, 66 },
				getEssChargeDischarges(p, schedule));
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

		assertArrayEquals(new int[] { 77, -679, 0, 117, 0, 0, 116, 112, -678, 0, 0, -672, 0, 0, 0, 0, -657, -661, 0, 77,
				77, 103, 0, 74, -22, 0, 1101, 0, 118, 1454, 951, 432, 760, 0, -80, -208, -337, 85, -552, -242, -293,
				-997, -976, -970, -1657, -1055, -861, -1528, -1773, 0, -1980, -1310, -1487, -1546, -1099, -1297, -792,
				308, 0, 48, 844, 171, 380, 755, 381, 632, 1265, 744, 764, 1210, 1048, 1118, 1187, 1219, 309, 348, 356,
				280, 841, 1022, 104, 109, 808, 376, 273, 463, 91, 157, 523, 138, 278, 452, 805, 407, 332, 66 },
				getEssChargeDischarges(p, schedule));
	}

	@Test
	public void testIsBadSolution() {
		var p = Params.create() //
				.time(TIME) //
				.essAvailableEnergy(0) //
				.essCapacity(22000) //
				.essMaxEnergyPerPeriod(0) //
				.maxBuyFromGrid(0) //
				.productions(new int[0]) //
				.consumptions(new int[0]) //
				.prices(new float[0]) //
				.states(new StateMachine[0]) //
				.build();

		assertFalse("DELAY_DISCHARGE and would discharge in balancing", //
				isBadSolution(p, DELAY_DISCHARGE, 1000, 0, 2500));
		assertTrue("DELAY_DISCHARGE and would charge from PV in balancing", //
				isBadSolution(p, DELAY_DISCHARGE, -1000, 0, 0));

		// TODO add test for DELAY_DISCHARGE with low SoC

		assertFalse("CHARGE actually from grid", //
				isBadSolution(p, CHARGE, 0, 1000, 0));
		assertTrue("CHARGE but fully supplied by excess PV", //
				isBadSolution(p, CHARGE, 0, 0, 0));

		assertFalse("CHARGE and battery is not full", //
				isBadSolution(p, CHARGE, 0, 1, 19799 /* 89 % */));
		assertTrue("CHARGE but battery is full (> 90 %)", //
				isBadSolution(p, CHARGE, 0, 1, 19800 /* 90 % */));
	}

	private static Params createParams888d20231106(StateMachine... states) {
		return Params.create() //
				.time(TIME) //
				.essAvailableEnergy((int) (22000 * 0.1)) //
				.essCapacity(22000) //
				.essMaxEnergyPerPeriod(toEnergy(10000)) //
				.maxBuyFromGrid(toEnergy(3000)) //
				.productions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.consumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.prices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
				.states(states) //
				.build();
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

	private static int[] getEssChargeDischarges(Params p, StateMachine[] schedule) {
		var result = new ArrayList<Integer>();
		calculateCost(p, schedule, dp -> result.add(dp.essChargeDischarge()));
		return result.stream().mapToInt(Integer::intValue).toArray();
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