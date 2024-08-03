package io.openems.edge.energy.optimizer;

import static com.google.common.math.Quantiles.percentiles;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController.PERIODS_PER_HOUR;
import static io.openems.edge.energy.optimizer.Utils.ESS_CHARGE_C_RATE;
import static io.openems.edge.energy.optimizer.Utils.findFirstPeakIndex;
import static io.openems.edge.energy.optimizer.Utils.findFirstValleyIndex;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.util.Arrays.stream;

import java.time.ZonedDateTime;
import java.util.stream.IntStream;

import com.google.common.primitives.ImmutableIntArray;

import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.energy.optimizer.Simulator.Period;

public class ParamsUtils {

	private ParamsUtils() {
	}

	/**
	 * Calculates the default ESS charge energy per period in
	 * {@link StateMachine#CHARGE_GRID}.
	 * 
	 * <p>
	 * Applies {@link #ESS_CHARGE_C_RATE} with the minimum of usable ESS energy or
	 * predicted consumption energy that cannot be supplied from production.
	 * 
	 * @param essMinSocEnergy ESS energy below a configured minimum SoC [Wh]
	 * @param essMaxSocEnergy ESS energy below a configured maximum SoC [Wh]
	 * @param productions     Production predictions per period
	 * @param consumptions    Consumption predictions per period
	 * @param prices          Prices per period
	 * @return the value in [Wh]
	 */
	protected static int calculateChargeEnergyInChargeGrid(int essMinSocEnergy, int essMaxSocEnergy, int[] productions,
			int[] consumptions, double[] prices) {
		var refs = ImmutableIntArray.builder();

		// Uses the total available energy as reference (= fallback)
		var fallback = max(0, essMaxSocEnergy - essMinSocEnergy);
		add(refs, fallback);

		// Uses the total excess consumption as reference
		add(refs, IntStream.range(0, min(productions.length, consumptions.length)) //
				.map(i -> consumptions[i] - productions[i]) // calculates excess Consumption Energy per Period
				.sum());

		// Uses the excess consumption till first production > consumption as reference
		add(refs, IntStream.range(0, min(productions.length, consumptions.length)) //
				.takeWhile(i -> consumptions[i] >= productions[i]) // take only first Periods
				.map(i -> consumptions[i] - productions[i]) // calculates excess Consumption Energy per Period
				.sum());

		// Uses the excess consumption during high price periods as reference
		{
			var peakIndex = findFirstPeakIndex(findFirstValleyIndex(0, prices), prices);
			var firstPrices = stream(prices).limit(peakIndex).toArray();
			if (firstPrices.length > 0) {
				var percentilePrice = percentiles().index(95).compute(firstPrices);
				add(refs, IntStream.range(0, min(productions.length, consumptions.length)) //
						.limit(peakIndex) //
						.filter(i -> prices[i] >= percentilePrice) // takes only prices >
						.map(i -> consumptions[i] - productions[i]) // calculates excess Consumption Energy per Period
						.sum());
			}
		}

		return (int) round(//
				refs.build().stream() //
						.average() //
						.orElse(fallback) //
						* ESS_CHARGE_C_RATE / PERIODS_PER_HOUR);
	}

	private static void add(ImmutableIntArray.Builder builder, int value) {
		if (value > 0) {
			builder.add(value);
		}
	}

	/**
	 * Calculates the index when period length switches from
	 * {@link Period.Length#QUARTER} to {@link Period.Length#HOUR}.
	 * 
	 * <p>
	 * The index is calculated as "6 hours" plus remaining quarters of the current
	 * hour.
	 * 
	 * @param time Start-Timestamp of the Schedule
	 * @return the index
	 */
	protected static int calculatePeriodLengthHourFromIndex(ZonedDateTime time) {
		var minute = time.getMinute();
		if (minute == 0) {
			minute = 60;
		}
		return 6 * 4 + (60 - minute) / 15;
	}
}
