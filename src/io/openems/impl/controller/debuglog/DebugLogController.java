/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
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
package io.openems.impl.controller.debuglog;

import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;

@ThingInfo("Output debug information on the systemlog")
public class DebugLogController extends Controller {

	// TODO Access all relevant channels directly via ThingRepository

	@ConfigInfo(title = "Sets all Ess", type = Ess.class, isOptional = true)
	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this);

	@ConfigInfo(title = "Sets all Meters", type = Meter.class, isOptional = true)
	public final ConfigChannel<Set<Meter>> meters = new ConfigChannel<Set<Meter>>("meters", this);

	@ConfigInfo(title = "Sets all RealTimeClocks", type = RealTimeClock.class, isOptional = true)
	public final ConfigChannel<RealTimeClock> rtc = new ConfigChannel<RealTimeClock>("rtc", this);

	public DebugLogController() {
		super();
	}

	public DebugLogController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			StringBuilder b = new StringBuilder();
			if (meters.valueOptional().isPresent()) {
				for (Meter meter : meters.value()) {
					b.append(meter.toString());
					b.append(" ");
				}
			}
			if (rtc.valueOptional().isPresent()) {
				b.append(rtc.valueOptional().get().toString());
				b.append(" ");
			}
			if (esss.valueOptional().isPresent()) {
				for (Ess ess : esss.value()) {
					b.append(ess.toString());
					b.append(" ");
				}
			}
			log.info(b.toString());
		} catch (InvalidValueException e) {
			e.printStackTrace();
		}
	}

}
