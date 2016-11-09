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

import java.util.Optional;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;

public class DebugLogController extends Controller {

	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this, Ess.class);

	public final ConfigChannel<Set<Meter>> meters = new ConfigChannel<Set<Meter>>("meters", this, Meter.class)
	/* .optional() */;

	@Override public void run() {
		try {
			StringBuilder b = new StringBuilder();
			if (meters.valueOptional().isPresent()) {
				for (Meter meter : meters.value()) {
					b.append(meter.id() + ": " + meter.activePower.format() + " ");
				}
			}

			for (Ess ess : esss.value()) {
				b.append(ess.id() + " [" + ess.soc.format() + "] " //
				// + "Act[" + ess.activePower.format() + "] " //
						+ "Charge[" + ess.allowedCharge.format() + "] " //
						+ "Discharge[" + ess.allowedDischarge.format() + "] " //
						+ "State[" + ess.systemState.format() + "]");
				Optional<String> warning = ess.warning.labelOptional();
				if (warning.isPresent()) {
					b.append(" Warning[" + warning.get() + "]");
				}
			}
			log.info(b.toString());
		} catch (InvalidValueException e) {
			e.printStackTrace();
		}
	}

}
