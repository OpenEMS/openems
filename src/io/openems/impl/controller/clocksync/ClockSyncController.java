package io.openems.impl.controller.clocksync;

import java.util.Optional;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;

public class ClockSyncController extends Controller {
	public final ConfigChannel<RealTimeClock> rtc = new ConfigChannel<RealTimeClock>("rtc", this, RealTimeClock.class)
			.optional();

	private boolean isDateSet = false;

	@Override public void run() {
		if (isDateSet) {
			// Set time only once in the beginning
			return;
		}
		if (rtc.valueOptional().isPresent()) {
			RealTimeClock r = rtc.valueOptional().get();
			Optional<Long> year = r.year.valueOptional();
			Optional<Long> month = r.month.valueOptional();
			Optional<Long> day = r.day.valueOptional();
			Optional<Long> hour = r.hour.valueOptional();
			Optional<Long> minute = r.hour.valueOptional();
			Optional<Long> second = r.second.valueOptional();

			if (year.isPresent() && month.isPresent() && day.isPresent() && hour.isPresent() && minute.isPresent()
					&& second.isPresent()) {
				log.info("Current RTC of ESS: " + year.get() + "-" + month.get() + "-" + day.get() + " " + hour.get()
						+ ":" + minute.get() + ":" + second.get());
				isDateSet = true;
			}
		}
	}
}
