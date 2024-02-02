package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_12786_20231121;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRICES_12786_20231121;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRICES_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_12786_20231121;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_888_20231106;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.calculateCost;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.getBestSchedule;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.interpolateArray;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.toEnergy;
import static java.lang.Math.abs;
import static java.util.Arrays.stream;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.DoubleStream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.jenetics.util.RandomRegistry;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;

public class SimulatorTest {

	public static final ZonedDateTime TIME = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

	@Before
	public void before() {
		// Make reproducable results
		System.setProperty("io.jenetics.util.defaultRandomGenerator", "Random");
		RandomRegistry.random(new Random(123));
	}

	@Test
	public void testMaxPrice() {
		assertEquals(332F, createParams888d20231106().maxPrice(), 0.000F);

		assertNull(Params.create() //
				.prices() //
				.build() //
				.maxPrice());
	}

	@Test
	public void testGetFirstSchedule0() {
		var existingSchedule = new StateMachine[] { CHARGE, DELAY_DISCHARGE, CHARGE, BALANCING };

		var p = Params.create() //
				.time(TIME) //
				.essTotalEnergy(22000) //
				.essMinSocEnergy(0) //
				.essMaxSocEnergy(22000) //
				.essInitialEnergy((int) (22000 * 0.1)) //
				.essMaxEnergyPerPeriod(toEnergy(10000)) //
				.maxBuyFromGrid(toEnergy(24_000)) //
				.productions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.consumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.prices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
				.states(ControlMode.CHARGE_CONSUMPTION.states) //
				.existingSchedule(existingSchedule) //
				.build();
		var s = getBestSchedule(p, //
				/* executionLimitSeconds */ 30, //
				/* populationSize */ 1, //
				/* limit */ 1);

		assertArrayEquals(existingSchedule, Arrays.copyOfRange(s, 0, existingSchedule.length));
	}

	@Test
	@Ignore
	public void testOnlyBalancing888d20231106() {
		var p = createParams888d20231106(BALANCING);
		var schedule = getBestSchedule(p, //
				/* executionLimitSeconds */ 10 * 60);

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
		var p = createParams888d20231106(DELAY_DISCHARGE);
		var schedule = getBestSchedule(p, //
				/* executionLimitSeconds */ 10 * 60);

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
		var p = createParams888d20231106(CHARGE);
		var schedule = getBestSchedule(p, //
				/* executionLimitSeconds */ 10 * 60);

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
		var schedule = getBestSchedule(p, //
				/* executionLimitSeconds */ 10 * 60);

		// Cost: 1,6973 €
		// Grid Buy: 7794 Wh
		// Grid Sell: 2597 Wh

		logSchedule(p, schedule);
		logCost(p, schedule);
		logGridEnergy(p, schedule);

		assertArrayEquals(
				new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1454,
						0, 432, 314, 0, -80, 0, -337, 85, -552, -242, -293, -997, -976, -970, -1657, -1055, 0, 0, -1773,
						-2266, -1980, -1310, -1487, -1546, -1099, -1297, -792, 308, -218, 48, 844, 171, 380, 755, 381,
						632, 1265, 744, 764, 1210, 1048, 1118, 1187, 1219, 309, 348, 356, 280, 841, 1022, 104, 109, 808,
						376, 273, 463, 91, 157, 523, 138, 278, 452, 805, 407, 332, 66 },
				getEssChargeDischarges(p, schedule));
	}

	@Test
	@Ignore
	public void testCharge888d20231106() {
		var p = createParams888d20231106(ControlMode.CHARGE_CONSUMPTION.states);
		var schedule = getBestSchedule(p, //
				/* executionLimitSeconds */ 10 * 60);

		// Cost: 1,5012 €
		// Grid Buy: 7794 Wh
		// Grid Sell: 1439 Wh

		logSchedule(p, schedule);
		logCost(p, schedule);
		logGridEnergy(p, schedule);

		assertArrayEquals(
				new int[] { 0, 0, -654, 0, 0, 0, -634, -638, 0, -667, 0, 0, 446, 83, 0, 0, 0, 0, -681, 0, 0, 0, 0, -676,
						728, 0, 1101, 0, 118, 1454, 951, 432, 811, 26, 0, -208, -337, 85, -552, 0, 0, -997, -976, -970,
						-1657, -1055, -861, -1528, -1773, -2266, -1980, -1310, -1487, -1546, -1099, -1297, -186, 308, 0,
						48, 844, 171, 380, 755, 381, 632, 1265, 744, 764, 1210, 1048, 1118, 1187, 1219, 309, 348, 356,
						280, 841, 1022, 104, 109, 808, 376, 273, 463, 91, 157, 523, 138, 278, 452, 805, 407, 332, 66 },
				getEssChargeDischarges(p, schedule));
	}

	@Test
	@Ignore
	public void testCharge12786d20231121() {
		var p = createParams12786d20231121(ControlMode.CHARGE_CONSUMPTION.states);
		var schedule = getBestSchedule(p, //
				/* executionLimitSeconds */ 10 * 60);

		// Cost: 1,0649 €
		// Grid Buy: 8592 Wh
		// Grid Sell: 277 Wh

		logSchedule(p, schedule);
		logCost(p, schedule);
		logGridEnergy(p, schedule);

		assertArrayEquals(
				new int[] { 0, -45, 0, 0, 0, 0, 458, 0, 390, 0, 483, 281, 236, 216, 0, 0, 181, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -705, 71,
						47, 0, 0, 0, 0, 0, 0, 56, 125, 71, 66, 0, 67, 0, 66, 54, 0, 50, 32, -39, 0, 16, -2, 0, -41, -54,
						-41, 24, 0, -115, 2, 0, -63, -68, 0, 0, 0, -49, -6, 83, 16 },
				getEssChargeDischarges(p, schedule));
	}

	protected static Params createParams888d20231106(StateMachine... states) {
		return Params.create() //
				.time(TIME) //
				.essTotalEnergy(22000) //
				.essMinSocEnergy(0) //
				.essMaxSocEnergy(22000) //
				.essMaxEnergyPerPeriod(toEnergy(10000)) //
				.maxBuyFromGrid(toEnergy(24_000)) //
				.productions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.consumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.prices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
				.states(states) //
				.build();
	}

	private static Params createParams12786d20231121(StateMachine... states) {
		return Params.create() //
				.time(TIME.plusMinutes(15 * 60 + 30)) //
				.essTotalEnergy(22000) //
				.essMinSocEnergy(0) //
				.essMaxSocEnergy(22000) //
				.essMaxEnergyPerPeriod(toEnergy(10000)) //
				.maxBuyFromGrid(toEnergy(24_000)) //
				.productions(stream(interpolateArray(PRODUCTION_12786_20231121)).map(v -> toEnergy(v)).toArray()) //
				.consumptions(stream(interpolateArray(CONSUMPTION_12786_20231121)).map(v -> toEnergy(v)).toArray()) //
				.prices(interpolateArray(PRICES_12786_20231121)) //
				.states(states) //
				.build();
	}

	private static void logCost(Params p, StateMachine[] schedule) {
		System.out.println("Cost: " //
				+ "%.4f €".formatted(calculateCost(p, schedule) / 1_000_000 /* convert to € */));
	}

	protected static void logSchedule(Params p, StateMachine[] schedule) {
		var periods = new TreeMap<ZonedDateTime, Period>();
		calculateCost(p, schedule, period -> periods.put(period.time(), period));
		Utils.logSchedule(p, periods);
	}

	private static int[] getEssChargeDischarges(Params p, StateMachine[] schedule) {
		var periods = new ArrayList<Integer>();
		calculateCost(p, schedule, period -> periods.add(period.essChargeDischarge()));
		return periods.stream().mapToInt(Integer::intValue).toArray();
	}

	private static void logGridEnergy(Params p, StateMachine[] schedule) {
		var gridBuy = new AtomicInteger();
		var gridSell = new AtomicInteger();

		calculateCost(p, schedule, periods -> {
			if (periods.grid() > 0) {
				gridBuy.getAndUpdate(v -> v + periods.grid());
			} else {
				gridSell.getAndUpdate(v -> v + abs(periods.grid()));
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
	protected static double[] hourlyToQuarterly(double[] values) {
		return DoubleStream.of(values) //
				.flatMap(v -> DoubleStream.of(v, v, v, v)) //
				.toArray();
	}
}