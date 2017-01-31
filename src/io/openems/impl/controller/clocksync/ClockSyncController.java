package io.openems.impl.controller.clocksync;

import java.io.IOException;
import java.util.Calendar;
import java.util.Optional;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.WriteChannelException;

public class ClockSyncController extends Controller {
	@ConfigInfo(title = "realtTimeClock to set systemTime with", type = RealTimeClock.class)
	public final ConfigChannel<RealTimeClock> rtc = new ConfigChannel<RealTimeClock>("rtc", this).optional();

	private boolean isDateSet = false;

	public ClockSyncController() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ClockSyncController(String thingId) {
		super(thingId);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		if (isDateSet) {
			// Set time only once in the beginning
			return;
		}
		if (rtc.valueOptional().isPresent()) {
			RealTimeClock r = rtc.valueOptional().get();
			Optional<Long> rtcYear = r.year.valueOptional();
			Optional<Long> rtcMonth = r.month.valueOptional();
			Optional<Long> rtcDay = r.day.valueOptional();
			Optional<Long> rtcHour = r.hour.valueOptional();
			Optional<Long> rtcMinute = r.hour.valueOptional();
			Optional<Long> rtcSecond = r.second.valueOptional();

			if (rtcYear.isPresent() && rtcMonth.isPresent() && rtcDay.isPresent() && rtcHour.isPresent()
					&& rtcMinute.isPresent() && rtcSecond.isPresent()) {

				Calendar systemNow = Calendar.getInstance();
				int year = systemNow.get(Calendar.YEAR);
				if (year < 2016) {
					// System date is wrong -> set system date from RTC
					log.info("Setting system time from RTC: " + rtc.id() + ": " + rtcYear.get() + "-" + rtcMonth.get()
							+ "-" + rtcDay.get() + " " + rtcHour.get() + ":" + rtcMinute.get() + ":" + rtcSecond.get());
					try {
						Runtime.getRuntime()
								.exec(new String[] { "/usr/bin/timedatectl", "set-time", "2016-10-11 13:13:16" });
						// process is running in a separate process from now...
					} catch (IOException e) {
						log.error("Error while setting system time: ", e);
					}
				} else {
					// System date is correct -> set RTC from system date
					log.info("Setting RTC from system time: " + rtc.id() + ": " + systemNow.get(Calendar.YEAR) + "-"
							+ systemNow.get(Calendar.MONTH) + "-" + systemNow.get(Calendar.DAY_OF_MONTH) + " "
							+ systemNow.get(Calendar.HOUR_OF_DAY) + ":" + systemNow.get(Calendar.MINUTE) + ":"
							+ systemNow.get(Calendar.SECOND));
					try {
						r.year.pushWrite(Long.valueOf(systemNow.get(Calendar.YEAR)));
						r.month.pushWrite(Long.valueOf(systemNow.get(Calendar.MONTH)));
						r.day.pushWrite(Long.valueOf(systemNow.get(Calendar.DAY_OF_MONTH)));
						r.hour.pushWrite(Long.valueOf(systemNow.get(Calendar.HOUR_OF_DAY)));
						r.minute.pushWrite(Long.valueOf(systemNow.get(Calendar.MINUTE)));
						r.second.pushWrite(Long.valueOf(systemNow.get(Calendar.SECOND)));
					} catch (WriteChannelException e) {
						log.error("Error while setting RTC time: ", e);
					}
				}

				isDateSet = true;
			}
		}
	}
}
