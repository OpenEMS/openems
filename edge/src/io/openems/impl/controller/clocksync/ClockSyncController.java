/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.controller.clocksync;

import java.io.IOException;
import java.util.Calendar;
import java.util.Optional;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.WriteChannelException;

@ThingInfo(title = "Sychronizes system clocks", description = "Synchronizes the sytem clocks of OpenEMS and a connected real-time clock device.")
public class ClockSyncController extends Controller {

	private ThingStateChannels thingState = new ThingStateChannels(this);
	/*
	 * Constructors
	 */
	public ClockSyncController() {
		super();
	}

	public ClockSyncController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Real-time clock", description = "Sets the real-time clock device.", type = RealTimeClock.class, isOptional = true)
	public final ConfigChannel<RealTimeClock> rtc = new ConfigChannel<RealTimeClock>("rtc", this);

	/*
	 * Fields
	 */
	private boolean isDateSet = false;

	/*
	 * Methods
	 */
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

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}
}
