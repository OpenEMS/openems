package io.openems.edge.energy.api.test;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.energy.api.EnergyUtils.filterEshsWithDifferentModes;

import java.time.ZonedDateTime;
import java.util.Arrays;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;

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
				CLOCK, Environment.TEST, TIME, //
				eshs, filterEshsWithDifferentModes(eshs).collect(toImmutableList()), //
				new GlobalOptimizationContext.Grid(16000, 20000, JSCalendar.Tasks.empty()), //
				new GlobalOptimizationContext.Ess(5000, 22000, 16000, 16000), //
				GlobalOptimizationContext.Periods.create(Environment.TEST) //
						.add(time(0, 0), null, 0, 106, 293.70) //
						.add(time(0, 15), null, 0, 86, 293.70) //
						.add(time(0, 30), null, 0, 88, 293.70) //
						.add(time(0, 45), null, 0, 81, 293.70) //
						.add(time(1, 0), null, 0, 73, 294.30) //
						.add(time(1, 15), null, 0, 68, 294.30) //
						.add(time(1, 30), null, 0, 76, 294.30) //
						.add(time(1, 45), null, 0, 149, 294.30) //
						.add(time(2, 0), null, 0, 333, 289.30) //
						.add(time(2, 15), null, 0, 61, 289.30) //
						.add(time(2, 30), null, 0, 74, 289.30) //
						.add(time(2, 45), null, 0, 73, 289.30) //
						.add(time(3, 0), null, 0, 68, 288.00) //
						.add(time(3, 15), null, 0, 66, 288.00) //
						.add(time(3, 30), null, 0, 82, 288.00) //
						.add(time(3, 45), null, 0, 99, 288.00) //
						.add(time(4, 0), null, 0, 84, 288.80) //
						.add(time(4, 15), null, 0, 80, 288.80) //
						.add(time(4, 30), null, 0, 97, 288.80) //
						.add(time(4, 45), null, 0, 85, 288.80) //
						.add(time(5, 0), null, 0, 65, 302.90) //
						.add(time(5, 15), null, 0, 69, 302.90) //
						.add(time(5, 30), null, 0, 75, 302.90) //
						.add(time(5, 45), null, 3, 90, 302.90) //
						.add(time(6, 0), null, 6, 394, 331.70) //
						.add(time(6, 15), null, 36, 106, 331.70) //
						.add(time(6, 30), null, 112, 94, 331.70) //
						.add(time(6, 45), null, 205, 74, 331.70) //
						.add(time(7, 0), null, 342, 62, 342.50) //
						.add(time(7, 15), null, 437, 74, 342.50) //
						.add(time(7, 30), null, 518, 72, 342.50) //
						.add(time(7, 45), null, 628, 60, 342.50) //
						.add(time(8, 0), null, 931, 46, 332.70) //
						.add(time(8, 15), null, 1159, 45, 332.70) //
						.add(time(8, 30), null, 1349, 40, 332.70) //
						.add(time(8, 45), null, 1543, 26, 332.70) //
						.add(time(9, 0), null, 1743, 46, 311.80) //
						.add(time(9, 15), null, 1920, 472, 311.80) //
						.add(time(9, 30), null, 2112, 498, 311.80) //
						.add(time(9, 45), null, 2209, 83, 311.80) //
						.add(time(10, 0), null, 2436, 105, 292.10) //
						.add(time(10, 15), null, 2671, 92, 292.10) //
						.add(time(10, 30), null, 2723, 133, 292.10) //
						.add(time(10, 45), null, 2824, 88, 292.10) //
						.add(time(11, 0), null, 2878, 86, 282.90) //
						.add(time(11, 15), null, 2871, 245, 282.90) //
						.add(time(11, 30), null, 2883, 308, 282.90) //
						.add(time(11, 45), null, 2978, 77, 282.90)//
						.add(time(12, 0), null, 3044, 54, 260.70) //
						.add(time(12, 15), null, 3022, 64, 260.70) //
						.add(time(12, 30), null, 3036, 64, 260.70) //
						.add(time(12, 45), null, 3045, 59, 260.70) //
						.build());
	}

	private static ZonedDateTime time(int hours, int minutes) {
		return TIME.plusHours(hours).plusMinutes(minutes);
	}
}
