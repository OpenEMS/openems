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
package io.openems.impl.controller.energysaver;

import java.util.Optional;
import java.util.Set;

import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMapping;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.exception.WriteChannelException;

public class EnergysavingController extends Controller {

	@IsThingMapping public Set<Ess> esss = null;

	// private Long lastTimeValueWritten = System.currentTimeMillis();

	@Override public void run() {
		for (Ess ess : esss) {
			try {
				Optional<String> systemState = ess.systemState.labelOptional();
				if (!systemState.isPresent() || !systemState.get().equals(SymmetricEssNature.START)) {
					/*
					 * Always start ESS if it was stopped
					 */
					// Current system state is not START
					if (ess.setWorkState.peekWriteLabel().orElse(SymmetricEssNature.START)
							.equals(SymmetricEssNature.START)) {
						// SetWorkState was not set to anything different than START before -> START the system
						log.info("ESS [" + ess.id() + "] was stopped. Starting...");
						ess.setWorkState.pushWriteFromLabel(SymmetricEssNature.START);
					}
					// } else {
					// /*
					// * TODO go to Standby if no values were written since two minutes
					// */
					// // TODO lastTimeValueWritten = System.currentTimeMillis();
					// if (lastTimeValueWritten + 2 * 60 * 1000 < System.currentTimeMillis()) {
					// Optional<String> systemState = ess.systemState.labelOptional();
					// if (!systemState.isPresent() || (!systemState.get().equals(SymmetricEssNature.STANDBY)
					// && !systemState.get().equals("PV-Charge"))) {
					// // System state was not yet STANDBY or PV-Charge
					// if (ess.setWorkState.peekWriteLabel().orElse(SymmetricEssNature.STANDBY)
					// .equals(SymmetricEssNature.STANDBY)) {
					// // SetWorkState was not set to anything different than STANDBY before -> put the system
					// // in STANDBY
					// log.info("ESS [" + ess.id() + "] had no written value since two minutes. Standby...");
					// ess.setWorkState.pushWriteFromLabel(SymmetricEssNature.STANDBY);
					// }
					// }
					// }
				}
			} catch (WriteChannelException e) {
				log.error("", e);
			}
		}
	}

}
