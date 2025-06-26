package io.openems.edge.energy.api.test;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.energy.api.EnergyUtils.filterEshsWithDifferentModes;

import java.time.ZonedDateTime;
import java.util.Arrays;

import com.google.common.collect.ImmutableList;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.energy.api.RiskLevel;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;

public class DummyGlobalOptimizationContext {

	private DummyGlobalOptimizationContext() {
	}

	public static final TimeLeapClock CLOCK = createDummyClock();
	public static final ZonedDateTime TIME = ZonedDateTime.now(CLOCK);

	/**
	 * Generates a {@link GlobalOptimizationContext} with the given
	 * {@link EnergyScheduleHandler}s.
	 * 
	 * @param handlers the {@link EnergyScheduleHandler}s
	 * @return a {@link GlobalOptimizationContext}
	 */
	public static GlobalOptimizationContext fromHandlers(EnergyScheduleHandler... handlers) {
		final var eshs = Arrays.stream(handlers).collect(toImmutableList());

		return new GlobalOptimizationContext(//
				CLOCK, RiskLevel.MEDIUM, TIME, //
				eshs, filterEshsWithDifferentModes(eshs).collect(toImmutableList()), //
				new GlobalOptimizationContext.Grid(16000, 20000), //
				new GlobalOptimizationContext.Ess(5000, 22000, 16000, 16000), //
				ImmutableList.of(//
						new Period.Quarter(0, time(0, 0), 0, 106, 293.70), //
						new Period.Quarter(1, time(0, 15), 0, 86, 293.70), //
						new Period.Quarter(2, time(0, 30), 0, 88, 293.70), //
						new Period.Quarter(3, time(0, 45), 0, 81, 293.70), //
						new Period.Quarter(4, time(1, 0), 0, 73, 294.30), //
						new Period.Quarter(5, time(1, 15), 0, 68, 294.30), //
						new Period.Quarter(6, time(1, 30), 0, 76, 294.30), //
						new Period.Quarter(7, time(1, 45), 0, 149, 294.30), //
						new Period.Quarter(8, time(2, 0), 0, 333, 289.30), //
						new Period.Quarter(9, time(2, 15), 0, 61, 289.30), //
						new Period.Quarter(10, time(2, 30), 0, 74, 289.30), //
						new Period.Quarter(11, time(2, 45), 0, 73, 289.30), //
						new Period.Quarter(12, time(3, 0), 0, 68, 288.00), //
						new Period.Quarter(13, time(3, 15), 0, 66, 288.00), //
						new Period.Quarter(14, time(3, 30), 0, 82, 288.00), //
						new Period.Quarter(15, time(3, 45), 0, 99, 288.00), //
						new Period.Quarter(16, time(4, 0), 0, 84, 288.80), //
						new Period.Quarter(17, time(4, 15), 0, 80, 288.80), //
						new Period.Quarter(18, time(4, 30), 0, 97, 288.80), //
						new Period.Quarter(19, time(4, 45), 0, 85, 288.80), //
						new Period.Quarter(20, time(5, 0), 0, 65, 302.90), //
						new Period.Quarter(21, time(5, 15), 0, 69, 302.90), //
						new Period.Quarter(22, time(5, 30), 0, 75, 302.90), //
						new Period.Quarter(23, time(5, 45), 3, 90, 302.90), //
						new Period.Quarter(24, time(6, 0), 6, 394, 331.70), //
						new Period.Quarter(25, time(6, 15), 36, 106, 331.70), //
						new Period.Quarter(26, time(6, 30), 112, 94, 331.70), //
						new Period.Quarter(27, time(6, 45), 205, 74, 331.70), //
						new Period.Quarter(28, time(7, 0), 342, 62, 342.50), //
						new Period.Quarter(29, time(7, 15), 437, 74, 342.50), //
						new Period.Quarter(30, time(7, 30), 518, 72, 342.50), //
						new Period.Quarter(31, time(7, 45), 628, 60, 342.50), //
						new Period.Quarter(32, time(8, 0), 931, 46, 332.70), //
						new Period.Quarter(33, time(8, 15), 1159, 45, 332.70), //
						new Period.Quarter(34, time(8, 30), 1349, 40, 332.70), //
						new Period.Quarter(35, time(8, 45), 1543, 26, 332.70), //
						new Period.Quarter(36, time(9, 0), 1743, 46, 311.80), //
						new Period.Quarter(37, time(9, 15), 1920, 472, 311.80), //
						new Period.Quarter(38, time(9, 30), 2112, 498, 311.80), //
						new Period.Quarter(39, time(9, 45), 2209, 83, 311.80), //
						new Period.Quarter(40, time(10, 0), 2436, 105, 292.10), //
						new Period.Quarter(41, time(10, 15), 2671, 92, 292.10), //
						new Period.Quarter(42, time(10, 30), 2723, 133, 292.10), //
						new Period.Quarter(43, time(10, 45), 2824, 88, 292.10), //
						new Period.Hour(44, time(11, 0), 11610, 716, 282.90, ImmutableList.of(//
								new Period.Quarter(0, time(11, 0), 2878, 86, 282.90), //
								new Period.Quarter(1, time(11, 15), 2871, 245, 282.90), //
								new Period.Quarter(2, time(11, 30), 2883, 308, 282.90), //
								new Period.Quarter(3, time(11, 45), 2978, 77, 282.90))), //
						new Period.Hour(45, time(12, 0), 6118, 241, 260.70, ImmutableList.of(//
								new Period.Quarter(0, time(12, 0), 3044, 54, 260.70), //
								new Period.Quarter(1, time(12, 15), 3022, 64, 260.70), //
								new Period.Quarter(2, time(12, 30), 3036, 64, 260.70), //
								new Period.Quarter(3, time(12, 45), 3045, 59, 260.70))) //
				));
	}

	private static ZonedDateTime time(int hours, int minutes) {
		return TIME.plusHours(hours).plusMinutes(minutes);
	}
}
