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
package io.openems.impl.controller.asymmetric.balancingcurrent;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.AsymmetricPower.ReductionType;

@ThingInfo(title = "Balancing current (Asymmetric)", description = "Tries to keep the grid meter at a given current. For asymmetric Ess.")
public class BalancingCurrentController extends Controller {

	/*
	 * Constructors
	 */
	public BalancingCurrentController() {
		super();
	}

	public BalancingCurrentController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ChannelInfo(title = "Current offset", description = "The current to hold on the grid-meter.", type = Meter.class)
	public final ConfigChannel<Integer> currentOffset = new ConfigChannel<>("CurrentOffset", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			// Calculate required sum values
			long currentL1 = meter.value().currentL1.value();
			if (meter.value().activePowerL1.value() < 0) {
				currentL1 *= -1;
			}
			long powerL1 = ((currentL1 - currentOffset.value() / 3) / 1000) * (meter.value().voltageL1.value() / 1000);
			long currentL2 = meter.value().currentL2.value();
			if (meter.value().activePowerL2.value() < 0) {
				currentL2 *= -1;
			}
			long powerL2 = ((currentL2 - currentOffset.value() / 3) / 1000) * (meter.value().voltageL2.value() / 1000);
			long currentL3 = meter.value().currentL3.value();
			if (meter.value().activePowerL3.value() < 0) {
				currentL3 *= -1;
			}
			long powerL3 = ((currentL3 - currentOffset.value() / 3) / 1000) * (meter.value().voltageL3.value() / 1000);
			powerL1 += ess.activePowerL1.value();
			powerL2 += ess.activePowerL2.value();
			powerL3 += ess.activePowerL3.value();
			ess.power.setActivePower(powerL1, powerL2, powerL3);
			ess.power.writePower(ReductionType.PERPHASE);
			log.info(ess.id() + " Set ActivePower L1: " + ess.power.getActivePowerL1() + ", L2: "
					+ ess.power.getActivePowerL2() + ", L3: " + ess.power.getActivePowerL3());
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		} catch (WriteChannelException e) {
			log.warn("write failed.", e);
		}
	}

}
