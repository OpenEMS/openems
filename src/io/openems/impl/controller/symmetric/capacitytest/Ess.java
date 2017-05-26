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
package io.openems.impl.controller.symmetric.capacitytest;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.SymmetricEssNature;

@IsThingMap(type = SymmetricEssNature.class)
public class Ess extends ThingMap {
	public ReadChannel<Long> soc;
	public ReadChannel<Long> activePower;
	public ReadChannel<Long> allowedCharge;
	public ReadChannel<Long> allowedDischarge;
	public ReadChannel<Integer> minSoc;
	public WriteChannel<Long> setWorkState;
	public WriteChannel<Long> setActivePower;
	public ReadChannel<Long> allowedApparent;
	public ReadChannel<Long> systemState;
	public boolean empty = false;
	public boolean full = false;

	public Ess(SymmetricEssNature ess) {
		super(ess);
		activePower = ess.activePower().required();
		allowedCharge = ess.allowedCharge().required();
		allowedDischarge = ess.allowedDischarge().required();
		minSoc = ess.minSoc().required();
		setActivePower = ess.setActivePower().required();
		soc = ess.soc().required();
		setWorkState = ess.setWorkState().required();
		allowedApparent = ess.allowedApparent().required();
		systemState = ess.systemState().required();
	}
}
