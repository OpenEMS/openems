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
package io.openems.impl.controller.symmetric.balancingoffset;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.SymmetricPower;

@IsThingMap(type = SymmetricEssNature.class)
public class Ess extends ThingMap {

	public final ReadChannel<Integer> minSoc;
	public final WriteChannel<Long> setActivePower;
	public final WriteChannel<Long> setReactivePower;
	public final ReadChannel<Long> soc;
	public final ReadChannel<Long> activePower;
	public final ReadChannel<Long> reactivePower;
	public final ReadChannel<Long> allowedCharge;
	public final ReadChannel<Long> allowedDischarge;
	public final ReadChannel<Long> systemState;
	public final SymmetricPower power;

	public Ess(SymmetricEssNature ess) {
		super(ess);
		minSoc = ess.minSoc().required();

		setActivePower = ess.setActivePower().required();
		setReactivePower = ess.setReactivePower().required();

		soc = ess.soc().required();
		activePower = ess.activePower().required();
		allowedCharge = ess.allowedCharge().required();
		allowedDischarge = ess.allowedDischarge().required();
		systemState = ess.systemState().required();
		reactivePower = ess.reactivePower().required();
		this.power = new SymmetricPower(ess.allowedDischarge().required(), ess.allowedCharge().required(),
				ess.allowedApparent().required(), ess.setActivePower().required(), ess.setReactivePower().required());
	}

	public long useableSoc() throws InvalidValueException {
		return soc.value() - minSoc.value();
	}
}
