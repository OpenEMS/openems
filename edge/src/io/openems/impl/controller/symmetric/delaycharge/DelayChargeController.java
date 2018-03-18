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
package io.openems.impl.controller.symmetric.delaycharge;

import java.time.LocalDateTime;
import java.util.Optional;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.common.session.Role;
import io.openems.core.utilities.power.symmetric.PowerException;

@ThingInfo(title = "Delay Charge (Symmetric)", description = "Delays the time of 100 % SoC to a set time of the day. For symmetric Ess.")
public class DelayChargeController extends Controller {

	private ThingStateChannels thingState = new ThingStateChannels(this);

	/*
	 * Constructors
	 */
	public DelayChargeController() {
		super();
	}

	public DelayChargeController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Hour of day", description = "Hour of day, when SoC should be 100 %.", type = Integer.class, writeRoles = {
			Role.OWNER, Role.INSTALLER }, defaultValue = "15", isArray = false)
	public final ConfigChannel<Integer> targetHour = new ConfigChannel<Integer>("targetHour", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		// Get required variables
		Ess ess = this.ess.getValue();
		long soc = ess.soc.getValue();
		long capacity = ess.capacity.getValue();
		long targetSecondOfDay = this.targetHour.getValue() * 3600;

		// calculate remaining capacity in Ws
		long remainingCapacity = capacity * (100 - soc) * 36;

		// We already passed the "target hour of day" -> no restrictions
		if (!(remainingCapacity > 0)) {
			log.error("RemainingCapacity is [" + remainingCapacity + "Ws] must be > 0");
			return;
		}

		// calculate remaining time
		long remainingTime = targetSecondOfDay - currentSecondOfDay();

		// We already passed the "target hour of day" -> no restrictions
		if (remainingTime < 0) {
			log.info("We already passed the \"target hour of day\" -> no restrictions");
			return;
		}

		// Get current charge limitation
		Optional<Long> currentLimitOpt = ess.power.getMinP();
		if (!currentLimitOpt.isPresent()) {
			return;
		}
		long currentLimit = currentLimitOpt.get();

		// Set limitation for ChargePower
		long newLimit = remainingCapacity / remainingTime * -1;
		ess.limit.setP(newLimit);
		try {
			ess.power.applyLimitation(ess.limit);
			log.info("Limit Ess [" + ess.id() + "] charging from [" + currentLimit + "W] to [" + newLimit + "W]");
		} catch (PowerException e) {
			log.warn("Unable to limit Ess [" + ess.id() + "] charging from [" + currentLimit + "W] to [" + newLimit
					+ "W]");
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

	private static long currentSecondOfDay() {
		LocalDateTime now = LocalDateTime.now();
		return now.getHour() * 3600 + now.getMinute() * 60 + now.getSecond();
	}
}
