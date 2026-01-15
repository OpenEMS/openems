package io.openems.edge.timeofusetariff.api;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Tasks;

public class TouManualHelper {

	public static final TouManualHelper EMPTY_TOU_MANUAL_HELPER = new TouManualHelper(Clock.systemDefaultZone(),
			Tasks.empty(), 0.0d);

	private final Clock clock;
	private final double standardPrice;
	private final JSCalendar.Tasks<Double> schedule;
	private ImmutableSortedMap<ZonedDateTime, Double> prices;
	private ZonedDateTime lastAccessTime = null;

	public TouManualHelper(Clock clock, JSCalendar.Tasks<Double> schedule, double standardPrice) {
		this.clock = clock;
		this.schedule = schedule;
		this.standardPrice = standardPrice;
	}

	/**
	 * Retrieves the current time-of-use pricing details.
	 * 
	 * <p>
	 * This method returns an instance of {@link TimeOfUsePrices}, which contains
	 * the standard and low price rates along with any applicable scheduling
	 * information.
	 * </p>
	 * 
	 * @return an instance of {@link TimeOfUsePrices} representing the configured
	 *         pricing details.
	 */
	public TimeOfUsePrices getPrices() {
		final var now = roundDownToQuarter(ZonedDateTime.now(this.clock));

		if (this.lastAccessTime != null && !now.isAfter(this.lastAccessTime)) {
			// Avoids recalculation within 15 minutes.
			return TimeOfUsePrices.from(this.prices);
		}

		final var fromDate = now;
		final var toDate = fromDate.plusDays(1).plusHours(12); // 36 hours

		final var ots = this.schedule.getOneTasksBetween(fromDate, toDate);
		this.prices = Stream.iterate(fromDate, d -> d.plusMinutes(15)) //
				.takeWhile(d -> d.isBefore(toDate)) //
				.collect(ImmutableSortedMap.toImmutableSortedMap(//
						ZonedDateTime::compareTo, //
						Function.identity(), //
						t -> Optional.ofNullable(ots.getPayloadAt(t)) //
								// No active OneTask -> fallback
								.orElse(this.standardPrice)));
		this.lastAccessTime = now;
		return TimeOfUsePrices.from(this.prices);
	}
}
