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

import java.util.List;

import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMapping;

public class DebugLogController extends Controller {
	@IsThingMapping
	public List<Ess> esss = null;

	// @IsThingMapping
	// public Meter meter = null;

	@Override
	public void run() {
		StringBuilder b = new StringBuilder();
		// b.append(meter.getThingId() + ": " + meter.activePower.toString() + " ");
		for (Ess ess : esss) {
			b.append(ess.getThingId() + ": " + ess.soc.format() + ", act=" + ess.activePower.format() + ", setAct="
					+ ess.setActivePower.format() + ", state=" + ess.systemState.format() + " ");
		}
		log.info(b.toString());
	}

}
