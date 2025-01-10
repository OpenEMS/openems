package io.openems.edge.energy.v1.optimizer;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.common.test.TestUtils.withValue;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.v1.UtilsV1.calculateLimitChargePowerFor14aEnWG;
import static io.openems.edge.energy.api.EnergyUtils.findFirstPeakIndex;
import static io.openems.edge.energy.api.EnergyUtils.findFirstValleyIndex;
import static io.openems.edge.energy.optimizer.TestData.CONSUMPTION_PREDICTION_QUARTERLY;
import static io.openems.edge.energy.optimizer.TestData.PRODUCTION_PREDICTION_QUARTERLY;
import static io.openems.edge.energy.optimizer.Utils.SUM_ESS_DISCHARGE_POWER;
import static io.openems.edge.energy.optimizer.Utils.SUM_ESS_SOC;
import static io.openems.edge.energy.optimizer.Utils.SUM_GRID;
import static io.openems.edge.energy.v1.EnergySchedulerImplTest.getOptimizer;
import static io.openems.edge.energy.v1.optimizer.EnergyFlowV1Test.NO_FLOW;
import static io.openems.edge.energy.v1.optimizer.SimulatorV1Test.TIME;
import static io.openems.edge.energy.v1.optimizer.TestDataV1.PAST_HOURLY_PRICES;
import static io.openems.edge.energy.v1.optimizer.TestDataV1.PAST_SOC;
import static io.openems.edge.energy.v1.optimizer.TestDataV1.PAST_STATES;
import static io.openems.edge.energy.v1.optimizer.TestDataV1.PRODUCTION_888_20231106;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.SUM_CONSUMPTION;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.SUM_PRODUCTION;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.calculateMaxChargePower;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.generateProductionPrediction;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.getEssMinSocEnergy;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.interpolateArray;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.interpolateDoubleArray;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.joinConsumptionPredictions;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.paramsAreValid;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.toEnergy;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.toPower;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.updateSchedule;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.IntStream;

import org.junit.Test;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limiter14a.ControllerEssLimiter14a;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.v1.EnergyScheduleHandlerV1.ContextV1;
import io.openems.edge.energy.v1.EnergySchedulerImplTest;
import io.openems.edge.energy.v1.optimizer.SimulatorV1.Period;
import io.openems.edge.timedata.test.DummyTimedata;

@SuppressWarnings("deprecation")
public class UtilsV1Test {

	protected static ImmutableSortedMap<ZonedDateTime, StateMachine> prepareExistingSchedule(ZonedDateTime fromDate,
			StateMachine... existingSchedule) {
		return IntStream.range(0, existingSchedule.length) //
				.mapToObj(Integer::valueOf) //
				.collect(ImmutableSortedMap.<Integer, ZonedDateTime, StateMachine>toImmutableSortedMap(
						ZonedDateTime::compareTo, //
						i -> fromDate.plusMinutes(i * 15), //
						i -> existingSchedule[i]));
	}

	@Test
	public void testInterpolateDoubleArray() {
		assertArrayEquals(new double[] { 123, 123, 234, 234, 345 }, //
				interpolateDoubleArray(new Double[] { null, 123., 234., null, 345., null }), //
				0.0001F);

		assertArrayEquals(new double[] {}, //
				interpolateDoubleArray(new Double[] { null }), //
				0.0001F);
	}

	@Test
	public void testToPower() {
		assertEquals(2000, (int) toPower(500));
		assertNull(toPower(null));
	}

	@Test
	public void testGenerateProductionPrediction() {
		final var arr = new Integer[] { 1, 2, 3 };
		assertArrayEquals(arr, generateProductionPrediction(arr, 2));
		assertArrayEquals(new Integer[] { 1, 2, 3, 0 }, generateProductionPrediction(arr, 4));
	}

	@Test
	public void testJoinConsumptionPredictions() {
		assertArrayEquals(//
				new Integer[] { 1, 2, 3, 4, 55, 66, 77, 88, 99 }, //
				joinConsumptionPredictions(4, //
						new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, //
						new Integer[] { 11, 22, 33, 44, 55, 66, 77, 88, 99 }));
	}

	@Test
	public void testFindFirstPeakIndex() {
		assertEquals(0, findFirstPeakIndex(0, new double[0]));
		assertEquals(0, findFirstPeakIndex(0, new double[] { 1 }));
		assertEquals(0, findFirstPeakIndex(0, new double[] { 1, 0 }));
		assertEquals(1, findFirstPeakIndex(0, new double[] { 0, 1, 0 }));
		assertEquals(1, findFirstPeakIndex(0, new double[] { 0, 1, 0, 1 }));
		assertEquals(5, findFirstPeakIndex(5, new double[0]));
	}

	@Test
	public void testFindFirstValleyIndex() {
		assertEquals(0, findFirstValleyIndex(0, new double[0]));
		assertEquals(0, findFirstValleyIndex(0, new double[] { 1 }));
		assertEquals(1, findFirstValleyIndex(0, new double[] { 1, 0 }));
		assertEquals(0, findFirstValleyIndex(0, new double[] { 0, 1, 0 }));
		assertEquals(2, findFirstValleyIndex(1, new double[] { 0, 1, 0, 1 }));
		assertEquals(5, findFirstValleyIndex(5, new double[0]));
	}

	@Test
	public void testParamsAreValid() throws Exception {
		var builder = ParamsV1.create() //
				.setTime(TIME) //
				.setEssInitialEnergy(0) //
				.setEssTotalEnergy(22000) //
				.setEssMinSocEnergy(2_000) //
				.setEssMaxSocEnergy(20_000) //
				.seMaxBuyFromGrid(toEnergy(24_000)) //
				.seMaxBuyFromGrid(0) //
				.setStates(new StateMachine[0]);

		// No periods are available
		assertFalse(paramsAreValid(builder //
				.setProductions() //
				.setConsumptions() //
				.setPrices() //
				.build()));

		// Production and Consumption predictions are all zero
		assertFalse(paramsAreValid(builder //
				.setProductions(0, 0, 0) //
				.setConsumptions(0, 0) //
				.setPrices(123F) //
				.build()));

		// Prices are all the same
		assertFalse(paramsAreValid(builder //
				.setProductions(0, 1, 3) //
				.setConsumptions(0, 2) //
				.setPrices(123F, 123F) //
				.build()));

		// Finally got it right...
		assertTrue(paramsAreValid(builder //
				.setProductions(0, 1, 3) //
				.setConsumptions(0, 2) //
				.setPrices(123F, 124F) //
				.build()));
		assertEquals(2, builder.build().optimizePeriods().size());
	}

	private static class MyControllerEssLimitTotalDischarge
			extends AbstractDummyOpenemsComponent<MyControllerEssLimitTotalDischarge>
			implements ControllerEssLimitTotalDischarge {

		protected MyControllerEssLimitTotalDischarge(Integer minSoc) {
			super("ctrl0", //
					OpenemsComponent.ChannelId.values(), //
					ControllerEssLimitTotalDischarge.ChannelId.values() //
			);
			withValue(this.getMinSocChannel(), minSoc);
		}

		@Override
		public void run() throws OpenemsNamedException {
		}

		@Override
		protected MyControllerEssLimitTotalDischarge self() {
			return this;
		}
	}

	private static class MyControllerEssEmergencyCapacityReserve
			extends AbstractDummyOpenemsComponent<MyControllerEssEmergencyCapacityReserve>
			implements ControllerEssEmergencyCapacityReserve {

		protected MyControllerEssEmergencyCapacityReserve(Integer reserveSoc) {
			super("ctrl0", //
					OpenemsComponent.ChannelId.values(), //
					ControllerEssEmergencyCapacityReserve.ChannelId.values() //
			);
			withValue(this.getActualReserveSocChannel(), reserveSoc);
		}

		@Override
		public void run() throws OpenemsNamedException {
		}

		@Override
		protected MyControllerEssEmergencyCapacityReserve self() {
			return this;
		}
	}

	private static class MyControllerEssLimiter14a extends AbstractDummyOpenemsComponent<MyControllerEssLimiter14a>
			implements ControllerEssLimiter14a {

		protected MyControllerEssLimiter14a(boolean restrictionMode) {
			super("ctrl0", //
					OpenemsComponent.ChannelId.values(), //
					ControllerEssLimiter14a.ChannelId.values() //
			);
			withValue(this.getRestrictionModeChannel(), restrictionMode);
		}

		@Override
		public void run() throws OpenemsNamedException {
		}

		@Override
		protected MyControllerEssLimiter14a self() {
			return this;
		}
	}

	@Test
	public void testGetEssMinSocEnergy() {
		var t1 = new MyControllerEssLimitTotalDischarge(50);
		var t2 = new MyControllerEssLimitTotalDischarge(null);
		var t3 = new MyControllerEssEmergencyCapacityReserve(30);
		var limiter1 = new MyControllerEssLimiter14a(false);
		var limiter2 = new MyControllerEssLimiter14a(true);
		assertEquals(5000, getEssMinSocEnergy(new ContextV1(//
				List.of(t3), List.of(t1, t2), List.of(limiter1, limiter2), //
				null, null, 10000), 10000));
	}

	@Test
	public void testHandleScheduleRequest() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-03-04T14:19:00.00Z"), ZoneOffset.UTC);
		final var energyScheduler = EnergySchedulerImplTest.create(clock);

		// Simulate historic data
		var now = roundDownToQuarter(ZonedDateTime.now(clock));
		final var fromDate = now.minusHours(3);
		var timedata = new DummyTimedata("timedata0");
		for (var i = 0; i < 12; i++) {
			var quarter = fromDate.plusMinutes(i * 15);
			timedata.add(quarter, new ChannelAddress("ctrl0", "QuarterlyPrices"), PAST_HOURLY_PRICES[i]);
			timedata.add(quarter, new ChannelAddress("ctrl0", "StateMachine"), PAST_STATES[i]);
			timedata.add(quarter, SUM_PRODUCTION, PRODUCTION_PREDICTION_QUARTERLY[i]);
			timedata.add(quarter, SUM_CONSUMPTION, CONSUMPTION_PREDICTION_QUARTERLY[i]);
			timedata.add(quarter, SUM_ESS_SOC, PAST_SOC[i]);
			timedata.add(quarter, SUM_ESS_DISCHARGE_POWER, PRODUCTION_888_20231106[i]);
			timedata.add(quarter, SUM_GRID, PRODUCTION_888_20231106[i]);
		}

		var globalContext = EnergySchedulerImplTest.getGlobalContext(energyScheduler);
		assertNotNull(globalContext);
		var optimizer = getOptimizer(energyScheduler);
		assertNotNull(optimizer);
	}

	@Test
	public void testUpdateSchedule() {
		final ZonedDateTime t = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		final Period pOld = new Period(null, DELAY_DISCHARGE, 0, NO_FLOW);
		final Period pNew = new Period(null, BALANCING, 0, NO_FLOW);

		var schedule = new TreeMap<ZonedDateTime, Period>();
		schedule.put(t.minusMinutes(15), pOld); // old entry is removed
		schedule.put(t, pOld); // current entry stays
		schedule.put(t.plusMinutes(15), pOld); // is overridden
		schedule.put(t.plusMinutes(30), pOld); // is overridden
		schedule.put(t.plusMinutes(45), pOld); // timestamp is missing in new Schedule -> remove

		var newSchedule = ImmutableSortedMap.<ZonedDateTime, Period>naturalOrder() //
				.put(t, pNew) //
				.put(t.plusMinutes(15), pNew) //
				.put(t.plusMinutes(30), pNew) //
				.build();

		updateSchedule(t, schedule, newSchedule);

		// One old entry
		assertEquals(1, schedule.values().stream().filter(v -> v == pOld).count());

		// Two new entries
		assertEquals(2, schedule.values().stream().filter(v -> v == pNew).count());

		// No old entry
		assertEquals(0, schedule.keySet().stream().filter(tz -> tz.isBefore(t)).count());

		// Details
		assertEquals(pOld, schedule.get(t));
		assertEquals(pNew, schedule.get(t.plusMinutes(15)));
		assertEquals(pNew, schedule.get(t.plusMinutes(30)));

		// No current entry -> handle null
		schedule.remove(t);
		updateSchedule(t, schedule, newSchedule);
	}

	@Test
	public void testCalculateLimitChargePowerFor14aEnWG() {
		assertEquals(-4200, calculateLimitChargePowerFor14aEnWG(
				List.of(new MyControllerEssLimiter14a(false), new MyControllerEssLimiter14a(true))));
		assertEquals(Integer.MIN_VALUE, calculateLimitChargePowerFor14aEnWG(
				List.of(new MyControllerEssLimiter14a(false), new MyControllerEssLimiter14a(false))));
	}

	@Test
	public void testCalculateMaxChargePower() {
		assertEquals(3000, calculateMaxChargePower(-4200, new Value<>(null, 3000), -1000));
		assertEquals(4200, calculateMaxChargePower(-4200, new Value<>(null, 5000), -1000));
		assertEquals(5000, calculateMaxChargePower(Integer.MIN_VALUE, new Value<>(null, 5000), -1000));
		assertEquals(1000, calculateMaxChargePower(Integer.MIN_VALUE, new Value<>(null, null), -1000));
		assertEquals(1000, calculateMaxChargePower(-4200, new Value<>(null, null), -1000));
	}

	@Test
	public void testInterpolateArrayInteger() {
		assertArrayEquals(new int[] { 123, 123, 234, 234, 345 }, //
				interpolateArray(new Integer[] { null, 123, 234, null, 345, null }));

		assertArrayEquals(new int[] {}, //
				interpolateArray(new Integer[] { null }));

		assertArrayEquals(new int[] { 123, 123 }, //
				interpolateArray(new Integer[] { null, 123 }));

		assertArrayEquals(new int[] { 123 }, //
				interpolateArray(new Integer[] { 123, null }));
	}
}
