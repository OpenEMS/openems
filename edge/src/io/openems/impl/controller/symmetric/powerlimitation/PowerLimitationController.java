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
package io.openems.impl.controller.symmetric.powerlimitation;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

@ThingInfo(title = "Power limitation (Symmetric)", description = "Limits the active and reactive power of the Ess. For symmetric Ess.")
public class PowerLimitationController extends Controller {

	/*
	 * Constructors
	 */
	public PowerLimitationController() {
		super();
	}

	public PowerLimitationController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Min-Charge ActivePower", description = "The minimum allowed active power for discharge. Value is negative.", type = Long.class)
	public ConfigChannel<Long> pMin = new ConfigChannel<Long>("pMin", this);

	@ChannelInfo(title = "Max-Charge ActivePower", description = "The maximum allowed active power for discharge. Value is positive.", type = Long.class)
	public ConfigChannel<Long> pMax = new ConfigChannel<Long>("pMax", this);

	@ChannelInfo(title = "Min-Charge ReactivePower", description = "The minimum allowed reactive power for discharge. Value is negative.", type = Long.class)
	public ConfigChannel<Long> qMin = new ConfigChannel<Long>("qMin", this);

	@ChannelInfo(title = "Max-Charge ReactivePower", description = "The maximum allowed reactive power for discharge. Value is positive.", type = Long.class)
	public ConfigChannel<Long> qMax = new ConfigChannel<Long>("qMax", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			try {
				if (pMax.value() < ess.value().setActivePower.writeMax().orElse(Long.MAX_VALUE)) {
					ess.value().setActivePower.pushWriteMax(pMax.value());
				}
			} catch (WriteChannelException | InvalidValueException e) {
				log.error("Failed to write Max P value for [" + ess.value().id + "]: " + e.getMessage());
			}
			try {
				if (pMin.value() > ess.value().setActivePower.writeMin().orElse(Long.MIN_VALUE)) {
					ess.value().setActivePower.pushWriteMin(pMin.value());
				}
			} catch (WriteChannelException | InvalidValueException e) {
				log.error("Failed to write Min P value for [" + ess.value().id + "]: " + e.getMessage());
			}
			try {
				if (qMin.value() > ess.value().setReactivePower.writeMin().orElse(Long.MIN_VALUE)) {
					ess.value().setReactivePower.pushWriteMin(qMin.value());
				}
			} catch (WriteChannelException | InvalidValueException e) {
				log.error("Failed to write Min Q value for [" + ess.value().id + "]: " + e.getMessage());
			}
			try {
				if (qMax.value() < ess.value().setReactivePower.writeMax().orElse(Long.MAX_VALUE)) {
					ess.value().setReactivePower.pushWriteMax(qMax.value());
				}
			} catch (WriteChannelException | InvalidValueException e) {
				log.error("Failed to write Max Q value for [" + ess.value().id + "]: " + e.getMessage());
			}
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		}
	}

}
