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
package io.openems.impl.controller.asymmetric.balancingcurrent;

import io.openems.api.channel.ReadChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.core.utilities.AsymmetricPower;

@IsThingMap(type = SymmetricEssNature.class)
public class Ess extends ThingMap {

	public ReadChannel<Long> soc;
	public ReadChannel<Long> activePowerL1;
	public ReadChannel<Long> activePowerL2;
	public ReadChannel<Long> activePowerL3;
	public ReadChannel<Long> reactivePowerL1;
	public ReadChannel<Long> reactivePowerL2;
	public ReadChannel<Long> reactivePowerL3;
	public AsymmetricPower power;

	public Ess(AsymmetricEssNature ess) {
		super(ess);
		this.soc = ess.soc();
		this.activePowerL1 = ess.activePowerL1().required();
		this.activePowerL2 = ess.activePowerL2().required();
		this.activePowerL3 = ess.activePowerL3().required();
		this.reactivePowerL1 = ess.reactivePowerL1().required();
		this.reactivePowerL2 = ess.reactivePowerL2().required();
		this.reactivePowerL3 = ess.reactivePowerL3().required();
		power = new AsymmetricPower(ess.allowedDischarge().required(), ess.allowedCharge().required(),
				ess.allowedApparent().required(), ess.setActivePowerL1().required(), ess.setActivePowerL2().required(),
				ess.setActivePowerL3().required(), ess.setReactivePowerL1().required(),
				ess.setReactivePowerL2().required(), ess.setReactivePowerL3().required());
	}
}
