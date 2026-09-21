package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_CHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DISCHARGE_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DISCHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.LIMIT_CHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargeGrid;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargePowerInChargeGrid;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateDelayDischarge;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateDischargeConsumption;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateDischargeGrid;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateLimitCharge;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableList;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.periods.Periods;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

@ExtendWith(MockitoExtension.class)
class UtilsTest {

	private static final TimeLeapClock CLOCK = new TimeLeapClock(Instant.ofEpochSecond(946684800), ZoneId.of("UTC"));
	private static final ZonedDateTime TIME = ZonedDateTime.now(CLOCK);

	@Test
	void testCalculateChargeGrid_case1() {
		final var ess = new DummyManagedSymmetricEss("ess0");
		final int essActivePower = -6000;
		final int gridActivePower = 10000;
		final int pwrBalancing = essActivePower + gridActivePower;
		final Integer gridSoftLimit = 20000;
		final var coc = mock(EnergyScheduler.OptimizationContext.class);
		when(coc.essChargePowerInChargeGrid()).thenReturn(10000);

		final var result = calculateChargeGrid(ess, essActivePower, gridActivePower, pwrBalancing, gridSoftLimit, coc);

		assertEquals(new Utils.ApplyMode(CHARGE_GRID, -10000), result);
	}

	@Test
	void testCalculateChargeGrid_case2() {
		final var ess = new DummyManagedSymmetricEss("ess0");
		final int essActivePower = -6000;
		final int gridActivePower = 5000;
		final int pwrBalancing = essActivePower + gridActivePower;
		final Integer gridSoftLimit = 20000;
		final var coc = mock(EnergyScheduler.OptimizationContext.class);
		when(coc.essChargePowerInChargeGrid()).thenReturn(10000);

		final var result = calculateChargeGrid(ess, essActivePower, gridActivePower, pwrBalancing, gridSoftLimit, coc);

		assertEquals(new Utils.ApplyMode(CHARGE_GRID, -11000), result);
	}

	@Test
	void testCalculateChargeGrid_case3() {
		final var ess = new DummyManagedSymmetricEss("ess0");
		final int essActivePower = -1000;
		final int gridActivePower = 500;
		final int pwrBalancing = essActivePower + gridActivePower;
		final Integer gridSoftLimit = 20000;
		final var coc = mock(EnergyScheduler.OptimizationContext.class);
		when(coc.essChargePowerInChargeGrid()).thenReturn(1340);

		final var result = calculateChargeGrid(ess, essActivePower, gridActivePower, pwrBalancing, gridSoftLimit, coc);

		assertEquals(new Utils.ApplyMode(CHARGE_GRID, -1840), result);
	}

	@Test
	void testCalculateChargeGrid_case4_peakShaving() {
		final var ess = new DummyManagedSymmetricEss("ess0");
		final int essActivePower = 1000;
		final int gridActivePower = 9000;
		final int pwrBalancing = essActivePower + gridActivePower;
		final Integer gridSoftLimit = 5000;
		final var coc = mock(EnergyScheduler.OptimizationContext.class);
		when(coc.essChargePowerInChargeGrid()).thenReturn(1000);

		final var result = calculateChargeGrid(ess, essActivePower, gridActivePower, pwrBalancing, gridSoftLimit, coc);

		assertEquals(new Utils.ApplyMode(StateMachine.PEAK_SHAVING, 5000), result);
	}

	@Test
	void testCalculateChargeGrid_case5_hybridEss() {
		final var ess = new DummyHybridEss("ess0").withDcDischargePower(-1500);
		final int essActivePower = -1000;
		final int gridActivePower = -2000;
		final int pwrBalancing = essActivePower + gridActivePower;
		final Integer gridSoftLimit = 24000;
		final var coc = mock(EnergyScheduler.OptimizationContext.class);
		when(coc.essChargePowerInChargeGrid()).thenReturn(1340);

		final var result = calculateChargeGrid(ess, essActivePower, gridActivePower, pwrBalancing, gridSoftLimit, coc);

		assertEquals(new Utils.ApplyMode(CHARGE_GRID, -4340), result);
	}

	@Test
	void testCalculateDischargeGrid() {
		final int essActivePower = 2500;
		final int gridActivePower = 800;

		final var result = calculateDischargeGrid(essActivePower, gridActivePower);

		assertEquals(new Utils.ApplyMode(DISCHARGE_GRID, 8300), result);
	}

	@Nested
	@DisplayName("calculateDelayDischarge()")
	class CalculateDelayDischargeTest {

		@Test
		void shouldDelayDischarge_whenNonHybridEss() {
			final var ess = new DummyManagedSymmetricEss("ess0");

			final var result = calculateDelayDischarge(ess, 2500, 3000);

			assertEquals(new Utils.ApplyMode(DELAY_DISCHARGE, 0), result);
		}

		@Test
		void shouldDelayDischarge_whenHybridEss() {
			final var ess = new DummyHybridEss("ess0").withDcDischargePower(1000);

			final var result = calculateDelayDischarge(ess, 3000, 2500);

			assertEquals(new Utils.ApplyMode(DELAY_DISCHARGE, 2000), result);
		}

		@Test
		void shouldDoBalancing_whenDelayDischargeSetpointAtOrAboveBalancingSetpoint() {
			final var ess = new DummyHybridEss("ess0").withDcDischargePower(1000);

			final var result = calculateDelayDischarge(ess, 3000, 1500);

			assertEquals(new Utils.ApplyMode(BALANCING, 1500), result);
		}

		@Test
		void shouldClampNegativeDelayDischargeSetpointToZero_whenHybridEss() {
			final var ess = new DummyHybridEss("ess0").withDcDischargePower(1500);

			final var result = calculateDelayDischarge(ess, 500, 1000);

			assertEquals(new Utils.ApplyMode(DELAY_DISCHARGE, 0), result);
		}
	}

	@Nested
	@DisplayName("calculateLimitCharge()")
	class CalculateLimitChargeTest {

		@Test
		void shouldDoBalancing_whenLimitChargePowerIsNull() {
			final var ess = new DummyManagedSymmetricEss("ess0");

			final var result = calculateLimitCharge(ess, 1000, null, 1500);

			assertEquals(new Utils.ApplyMode(BALANCING, 1500), result);
		}

		@Test
		void shouldDoBalancing_whenLimitChargeSetpointIsBelowOrEqualBalancing_nonHybrid() {
			final var ess = new DummyManagedSymmetricEss("ess0");

			final var result = calculateLimitCharge(ess, 1000, 2000, -1500);

			assertEquals(new Utils.ApplyMode(BALANCING, -1500), result);
		}

		@Test
		void shouldDoBalancing_whenLimitChargeSetpointIsBelowOrEqualBalancing_hybrid() {
			final var ess = new DummyHybridEss("ess0").withDcDischargePower(1000);

			final var result = calculateLimitCharge(ess, 3000, 1000, 1200);

			assertEquals(new Utils.ApplyMode(BALANCING, 1200), result);
		}

		@Test
		void shouldDelayCharge_whenLimitChargePowerIsZeroAndSetpointAboveBalancing() {
			final var ess = new DummyManagedSymmetricEss("ess0");

			final var result = calculateLimitCharge(ess, 0, 0, -1);

			assertEquals(new Utils.ApplyMode(DELAY_CHARGE, 0), result);
		}

		@Test
		void shouldLimitCharge_whenSetpointAboveBalancing_nonHybrid() {
			final var ess = new DummyManagedSymmetricEss("ess0");

			final var result = calculateLimitCharge(ess, 0, 100, -200);

			assertEquals(new Utils.ApplyMode(LIMIT_CHARGE, -100), result);
		}

		@Test
		void shouldLimitCharge_whenSetpointAboveBalancing_hybrid() {
			final var ess = new DummyHybridEss("ess0").withDcDischargePower(1000);

			final var result = calculateLimitCharge(ess, 4000, 500, 1000);

			assertEquals(new Utils.ApplyMode(LIMIT_CHARGE, 2500), result);
		}

		@Test
		void shouldUseZeroWhenHybridDcDischargePowerIsMissing() {
			final var ess = new DummyHybridEss("ess0"); // no withDcDischargePower

			final var result = calculateLimitCharge(ess, 300, 100, 100);

			assertEquals(new Utils.ApplyMode(LIMIT_CHARGE, 200), result);
		}
	}

	@Nested
	@DisplayName("calculateDischargeConsumption()")
	class CalculateDischargeConsumptionTest {

		@ParameterizedTest(name = "[{index}] balancingSetpoint={0}, productionActivePower={1} -> setpoint={2}")
		@CsvSource({ "3000, 1200, 4200", //
				"3000, -800, 2200", //
				"0, 0, 0", //
				"500, -1200, -700" })
		void shouldCalculateDischargeConsumptionSetpoint(int balancingSetpoint, int productionActivePower,
				int expectedSetpoint) {
			final var result = calculateDischargeConsumption(balancingSetpoint, productionActivePower);

			assertEquals(new Utils.ApplyMode(DISCHARGE_CONSUMPTION, expectedSetpoint), result);
		}
	}

	@Nested
	@DisplayName("postProcessApplyMode()")
	class PostProcessApplyModeTest {

		@Test
		void shouldReturnPeakShaving_whenSetpointIsBelowMinSetpointAndBuySoftLimitIsConfigured() {
			final var applyMode = new Utils.ApplyMode(CHARGE_GRID, -5000);

			final var result = Utils.postProcessApplyMode(applyMode, 1000, 3000, 4000);

			assertEquals(new Utils.ApplyMode(StateMachine.PEAK_SHAVING, -2000), result);
		}

		@Test
		void shouldReturnAvoidGridSellLimit_whenSetpointIsAboveMaxSetpoint() {
			final var applyMode = new Utils.ApplyMode(DISCHARGE_GRID, 7000);

			final var result = Utils.postProcessApplyMode(applyMode, 1000, 3000, 4000);

			assertEquals(new Utils.ApplyMode(StateMachine.AVOID_GRID_SELL_LIMIT, 5000), result);
		}

		@Test
		void shouldReturnUnchangedApplyMode_whenWithinLimits() {
			final var applyMode = new Utils.ApplyMode(DELAY_DISCHARGE, 2000);

			final var result = Utils.postProcessApplyMode(applyMode, 1000, 3000, 4000);

			assertEquals(applyMode, result);
		}

		@Test
		void shouldIgnoreBuySoftLimit_whenBuySoftLimitIsNull() {
			final var applyMode = new Utils.ApplyMode(CHARGE_GRID, -5000);

			final var result = Utils.postProcessApplyMode(applyMode, 1000, null, 4000);

			assertEquals(applyMode, result);
		}

		@Test
		void shouldReturnUnchangedApplyMode_whenSetpointEqualsMinSetpoint() {
			final var applyMode = new Utils.ApplyMode(CHARGE_GRID, -2000);

			final var result = Utils.postProcessApplyMode(applyMode, 1000, 3000, 4000);

			assertEquals(applyMode, result);
		}

		@Test
		void shouldReturnUnchangedApplyMode_whenSetpointEqualsMaxSetpoint() {
			final var applyMode = new Utils.ApplyMode(DISCHARGE_GRID, 5000);

			final var result = Utils.postProcessApplyMode(applyMode, 1000, 3000, 4000);

			assertEquals(applyMode, result);
		}
	}

	@Test
	void testCalculateChargePowerInChargeGrid() {
		assertEquals(5745, calculateChargePowerInChargeGrid(//
				new GlobalOptimizationContext(CLOCK, Environment.TEST, TIME, ImmutableList.of(), ImmutableList.of(), //
						new GlobalOptimizationContext.Grid(0, 20000, 19000, JSCalendar.Tasks.empty()), //
						new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
						Periods.empty()),
				/* maxEnergyInChargeGrid */ 11490));

		assertEquals(4336, calculateChargePowerInChargeGrid(//
				new GlobalOptimizationContext(CLOCK, Environment.TEST, TIME, ImmutableList.of(), ImmutableList.of(), //
						new GlobalOptimizationContext.Grid(0, 20000, 19000, JSCalendar.Tasks.empty()), //
						new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
						Periods.builder(Environment.TEST) //
								.addPeriodIfValid(TIME, null, 0, 1000, 0., null) //
								.addPeriodIfValid(TIME.plusMinutes(15), null, 100, 1100, 0., null) //
								.addPeriodIfValid(TIME.plusMinutes(30), null, 200, 0, 0., null) //
								.build()), //
				/* maxEnergyInChargeGrid */ 11490));

		assertEquals(3182, calculateChargePowerInChargeGrid(//
				new GlobalOptimizationContext(CLOCK, Environment.TEST, TIME, ImmutableList.of(), ImmutableList.of(), //
						new GlobalOptimizationContext.Grid(0, 20000, 19000, JSCalendar.Tasks.empty()), //
						new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
						Periods.builder(Environment.TEST) //
								.addPeriodIfValid(TIME, null, 0, 700, 123., null) //
								.addPeriodIfValid(TIME.plusMinutes(15), null, 100, 600, 123., null) //
								.addPeriodIfValid(TIME.plusMinutes(30), null, 200, 500, 125., null) //
								.addPeriodIfValid(TIME.plusMinutes(45), null, 300, 400, 126., null) //
								.addPeriodIfValid(TIME.plusMinutes(60), null, 400, 300, 123., null) //
								.addPeriodIfValid(TIME.plusMinutes(75), null, 500, 200, 122., null) //
								.addPeriodIfValid(TIME.plusMinutes(90), null, 600, 100, 121., null) //
								.addPeriodIfValid(TIME.plusMinutes(105), null, 700, 0, 121., null) //
								.build()), //
				/* maxEnergyInChargeGrid */ 11490));

		assertEquals(3818, calculateChargePowerInChargeGrid(//
				new GlobalOptimizationContext(CLOCK, Environment.TEST, TIME, ImmutableList.of(), ImmutableList.of(), //
						new GlobalOptimizationContext.Grid(0, 20000, 19000, JSCalendar.Tasks.empty()), //
						new GlobalOptimizationContext.Ess(0, 12223, 5000, 5000), //
						Periods.builder(Environment.TEST) //
								.addPeriodIfValid(TIME, null, 0, 700, 120., null) //
								.addPeriodIfValid(TIME.plusMinutes(15), null, 100, 600, 121., null) //
								.addPeriodIfValid(TIME.plusMinutes(30), null, 200, 500, 122., null) //
								.addPeriodIfValid(TIME.plusMinutes(45), null, 300, 1140, 126., null) //
								.addPeriodIfValid(TIME.plusMinutes(60), null, 400, 1150, 125., null) //
								.addPeriodIfValid(TIME.plusMinutes(75), null, 500, 200, 122., null) //
								.addPeriodIfValid(TIME.plusMinutes(90), null, 600, 100, 121., null) //
								.addPeriodIfValid(TIME.plusMinutes(105), null, 700, 0, 121., null) //
								.build()), //
				/* maxEnergyInChargeGrid */ 11490));
	}
}
