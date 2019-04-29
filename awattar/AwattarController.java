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
package io.openems.impl.controller.symmetric.awattar;

import java.time.LocalDateTime;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;

@ThingInfo(title = "(Symmetric)", description = "")
public class AwattarController extends Controller {

	private final Logger log = LoggerFactory.getLogger(AwattarController.class);

	private ThingStateChannels thingState = new ThingStateChannels(this);

	/*
	 * Constructors
	 */
	public AwattarController() {
		super();
	}

	public AwattarController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */

	@ChannelInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class, isArray = true)
	public ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public ConfigChannel<Meter> gridMeter = new ConfigChannel<Meter>("gridMeter", this);

	@ChannelInfo(title = "PV-Meter", description = "Sets the PV meter.", type = Meter.class)
	public ConfigChannel<Meter> pvmeter = new ConfigChannel<Meter>("pvmeter", this);

	private final CalculateTotalConsumption calculateTotalConsumption = new CalculateTotalConsumption(this);
	boolean executed = false;
	private long cheapHourStartTimeStamp = 0;
	private long cheapHourEndTimeStamp = 0;
	LocalDateTime startTimeStamp = null;
	LocalDateTime endTimeStamp = null;

	/*
	 * Methods
	 */

	@Override
	public void run() {

		LocalDateTime now = LocalDateTime.now();
		int hourOfDay = now.getHour();
		// int secondOfDay = now.getSecond() + now.getMinute() * 60 + now.getHour() * 3600;
		/*
		 * First stage:
		 *
		 * Find out the consumption over night
		 * e.g. 15 kWh
		 */
		log.info("Calculating the required consumption to charge ");
		this.calculateTotalConsumption.run();

		/*
		 * Second stage:
		 *
		 * Find out when we need to charge the battery and with which power.
		 * e.g. starting from 3 a.m. charge with 2 kW for 1 hour.
		 */

		// if (!executed && hourOfDay == 14) {
		// log.info("Reading the Json file to get cheapest hour and timestamps during " + hourOfDay);
		// JsonData.jsonRead("Data.json");
		// cheapHourStartTimeStamp = JsonData.startTimeStamp();
		// cheapHourEndTimeStamp = JsonData.endTimeStamp();
		// startTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(cheapHourStartTimeStamp),
		// ZoneId.systemDefault());
		// endTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(cheapHourEndTimeStamp), ZoneId.systemDefault());
		// // if(startTimeStamp.getHour() <= 23) {
		// //
		// // }
		// executed = true;
		// }
		// if(executed && hourOfDay > 14) {
		// executed = false;
		// }
		/*
		 * Third stage:
		 *
		 * Actually control the inverter.
		 * e.g. Between 3 and 4 a.m. -> set the charge power to 2 kW
		 */

		// if (System.currentTimeMillis() >= JsonData.startTimeStamp()
		// && System.currentTimeMillis() <= JsonData.endTimeStamp()) {
		// log.info("Charging the power during cheapest hour");
		//
		// // try {
		// // this.ess.value().activePowerLimit.setP(4000l);
		// // this.ess.value().power.applyLimitation(ess.value().activePowerLimit);
		// // } catch (InvalidValueException e) {
		// // log.error("No ess found.", e);
		// // } catch (PowerException e) {
		// // log.error("Failed to set Power!", e);
		// // }
		// }

	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}
}
