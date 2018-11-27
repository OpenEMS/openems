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
package io.openems.impl.controller.asymmetric.capacitytest;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.AsymmetricEssNature;

@IsThingMap(type = AsymmetricEssNature.class)
public class Ess extends ThingMap {
	public ReadChannel<Long> soc;
	public ReadChannel<Long> activePowerL1;
	public ReadChannel<Long> activePowerL2;
	public ReadChannel<Long> activePowerL3;
	public ReadChannel<Long> allowedCharge;
	public ReadChannel<Long> allowedDischarge;
	public ReadChannel<Integer> minSoc;
	public WriteChannel<Long> setWorkState;
	public WriteChannel<Long> setActivePowerL1;
	public WriteChannel<Long> setActivePowerL2;
	public WriteChannel<Long> setActivePowerL3;
	public WriteChannel<Long> setReactivePowerL1;
	public WriteChannel<Long> setReactivePowerL2;
	public WriteChannel<Long> setReactivePowerL3;
	public ReadChannel<Long> allowedApparent;
	public ReadChannel<Long> systemState;
	public boolean empty = false;
	public boolean full = false;

	public Ess(AsymmetricEssNature ess) {
		super(ess);
		activePowerL1 = ess.activePowerL1().required();
		activePowerL2 = ess.activePowerL2().required();
		activePowerL3 = ess.activePowerL3().required();
		allowedCharge = ess.allowedCharge().required();
		allowedDischarge = ess.allowedDischarge().required();
		minSoc = ess.minSoc().required();
		setActivePowerL1 = ess.setActivePowerL1().required();
		setActivePowerL2 = ess.setActivePowerL2().required();
		setActivePowerL3 = ess.setActivePowerL3().required();
		setReactivePowerL1 = ess.setReactivePowerL1().required();
		setReactivePowerL2 = ess.setReactivePowerL2().required();
		setReactivePowerL3 = ess.setReactivePowerL3().required();
		soc = ess.soc().required();
		setWorkState = ess.setWorkState().required();
		allowedApparent = ess.allowedApparent().required();
		systemState = ess.systemState().required();
	}
}
