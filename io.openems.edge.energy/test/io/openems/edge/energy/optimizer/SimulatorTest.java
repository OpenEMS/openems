package io.openems.edge.energy.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.energy.TestData.CONSUMPTION_888_20231106;
import static io.openems.edge.energy.TestData.PRICES_888_20231106;
import static io.openems.edge.energy.TestData.PRODUCTION_888_20231106;
import static io.openems.edge.energy.optimizer.Simulator.getBestSchedule;
import static io.openems.edge.energy.optimizer.Simulator.simulate;
import static io.openems.edge.energy.optimizer.Utils.interpolateArray;
import static io.openems.edge.energy.optimizer.Utils.toEnergy;
import static java.util.Arrays.stream;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.DoubleStream;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSortedMap;

import io.jenetics.util.RandomRegistry;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.energy.optimizer.Simulator.Period;

public class SimulatorTest {

	public static final ZonedDateTime TIME = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

	@Before
	public void before() {
		// Make reproducible results
		System.setProperty("io.jenetics.util.defaultRandomGenerator", "Random");
		RandomRegistry.random(new Random(123));
	}

	private static Period simulatePeriod(StateMachine state, int production, int consumption, double price,
			int essInitial) {
		var result = new AtomicReference<Period>();
		var params = Params.create() //
				.setTime(TIME) //
				.setEssTotalEnergy(22000) //
				.setEssMinSocEnergy(0) //
				.setEssMaxSocEnergy(20000) //
				.setEssInitialEnergy(essInitial) //
				.setEssMaxChargeEnergy(3000 /* [Wh/15 Minutes] */) //
				.setEssMaxDischargeEnergy(3000 /* [Wh/15 Minutes] */) //
				.seMaxBuyFromGrid(4000 /* [Wh/15 Minutes] */) //
				.setProductions(new int[] { production }) //
				.setConsumptions(new int[] { consumption }) //
				.setPrices(new double[] { price }) //
				.setStates(new StateMachine[] { state }) //
				.setExistingSchedule(ImmutableSortedMap.of()) //
				.build();
		Simulator.simulatePeriod(params, params.optimizePeriods().get(0), state, new AtomicInteger(essInitial),
				result::set);

		return result.get();
	}

	private static void assertPeriod(String message, Period period, int essChargeDischarge, int grid, double cost) {
		assertEquals(period.state() + "-essChargeDischarge: " + message, essChargeDischarge, period.ef().ess());
		assertEquals(period.state() + "-grid: " + message, grid, period.ef().grid());
	}

	@Test
	public void testCalculatePeriodCostBalancing() {
		assertPeriod("Consumption > Production; SoC ok", //
				simulatePeriod(BALANCING, 200, 300, 0.1, 10000), //
				100, 0, 0);
		assertPeriod("Consumption > Production; discharge limited by essMaxEnergyPerPeriod", //
				simulatePeriod(BALANCING, 1000, 5000, 0.1, 10000), //
				3000, 1000, 100);
		assertPeriod("Consumption > Production; discharge limited by essMinSocEnergy", //
				simulatePeriod(BALANCING, 1000, 5000, 0.1, 2500), //
				2500, 1500, 150);

		assertPeriod("Production > Consumption; SoC ok", //
				simulatePeriod(BALANCING, 300, 200, 0.1, 10000), //
				-100, 0, 0);
		assertPeriod("Production > Consumption; charge limited by essMaxEnergyPerPeriod", //
				simulatePeriod(BALANCING, 5000, 1000, 0.1, 10000), //
				-3000, -1000, 0);
		assertPeriod("Production > Consumption; charge limited by essTotalEnergy", //
				simulatePeriod(BALANCING, 5000, 1000, 0.1, 19500), //
				-2500, -1500, 0);
	}

	@Test
	public void testCalculatePeriodCostDelayDischarge() {
		assertPeriod("Consumption > Production", //
				simulatePeriod(DELAY_DISCHARGE, 200, 300, 0.1, 10000), //
				0, 100, 10);

		assertPeriod("Production > Consumption; SoC ok", //
				simulatePeriod(DELAY_DISCHARGE, 300, 200, 0.1, 10000), //
				-100, 0, 0);
		assertPeriod("Production > Consumption; charge limited by essMaxEnergyPerPeriod", //
				simulatePeriod(DELAY_DISCHARGE, 5000, 1000, 0.1, 10000), //
				-3000, -1000, 0);
		assertPeriod("Production > Consumption; charge limited by essTotalEnergy", //
				simulatePeriod(DELAY_DISCHARGE, 5000, 1000, 0.1, 19500), //
				-2500, -1500, 0);
	}

	@Test
	public void testCalculatePeriodCostChargeGrid() {
		assertPeriod("Consumption > Production", //
				simulatePeriod(CHARGE_GRID, 200, 300, 0.1, 10000), //
				-842, 942 /* 842 + 100 */, 302.5);

		assertPeriod("Consumption > Production; charge limited by maxBuyFromGrid", //
				simulatePeriod(CHARGE_GRID, 0, 4500, 0.1, 10000), //
				500, 4000, 450.12);

		assertPeriod("Production > Consumption", //
				simulatePeriod(CHARGE_GRID, 300, 200, 0.1, 10000), //
				-2600 /* 2500 + 100 */, 2500, 292.5);

		assertPeriod("Production > Consumption; charge limited by essMaxEnergyPerPeriod", //
				simulatePeriod(CHARGE_GRID, 3000, 900, 0.1, 10000), //
				-3000, 900, 105.3);

		assertPeriod("Production > Consumption", //
				simulatePeriod(CHARGE_GRID, 2000, 1700, 0.1, 10000), //
				-2800, 2500, 292.5);

		assertPeriod("Production > Consumption; battery nearly full", //
				simulatePeriod(CHARGE_GRID, 3000, 100, 0.1, 19600), //
				-400 /* 400 from PV; then full */, -2500 /* sell-to-grid */, 292.5);
	}

	@Test
	public void testGetFirstSchedule0() {
		var existingSchedule = new StateMachine[] { CHARGE_GRID, DELAY_DISCHARGE, CHARGE_GRID, BALANCING };

		var p = Params.create() //
				.setTime(TIME) //
				.setEssTotalEnergy(22000) //
				.setEssMinSocEnergy(0) //
				.setEssMaxSocEnergy(22000) //
				.setEssInitialEnergy((int) (22000 * 0.1)) //
				.setEssMaxChargeEnergy(toEnergy(10000)) //
				.setEssMaxDischargeEnergy(toEnergy(10000)) //
				.seMaxBuyFromGrid(toEnergy(24_000)) //
				.setProductions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.setConsumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.setPrices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
				.setStates(ControlMode.CHARGE_CONSUMPTION.states) //
				.setExistingSchedule(UtilsTest.prepareExistingSchedule(TIME, existingSchedule)) //
				.build();
		var s = getBestSchedule(p, //
				/* executionLimitSeconds */ 30, //
				/* populationSize */ 2, //
				/* limit */ 1);

		assertArrayEquals(existingSchedule, Arrays.copyOfRange(s, 0, existingSchedule.length));
	}

	/**
	 * Creates dummy {@link Params}.
	 * 
	 * @param states the allowed states
	 * @return {@link Params}
	 */
	public static Params createParams888d20231106(StateMachine... states) {
		return Params.create() //
				.setTime(TIME) //
				.setEssTotalEnergy(22000) //
				.setEssMinSocEnergy(0) //
				.setEssMaxSocEnergy(22000) //
				.setEssMaxChargeEnergy(toEnergy(10000)) //
				.setEssMaxDischargeEnergy(toEnergy(10000)) //
				.seMaxBuyFromGrid(toEnergy(24_000)) //
				.setProductions(stream(interpolateArray(PRODUCTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.setConsumptions(stream(interpolateArray(CONSUMPTION_888_20231106)).map(v -> toEnergy(v)).toArray()) //
				.setPrices(hourlyToQuarterly(interpolateArray(PRICES_888_20231106))) //
				.setStates(states) //
				.build();
	}

	protected static void logSchedule(Params p, StateMachine[] schedule) {
		Utils.logSchedule(p, simulate(p, schedule));
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