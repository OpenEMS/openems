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
package io.openems.impl.controller.asymmetric.fixvalue;

import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.core.utilities.AsymmetricPower;

@IsThingMap(type = AsymmetricEssNature.class)
public class Ess extends ThingMap {
	public AsymmetricPower power;

	public Ess(AsymmetricEssNature ess) {
		super(ess);
		power = new AsymmetricPower(ess.allowedDischarge().required(), ess.allowedCharge().required(),
				ess.allowedApparent().required(), ess.setActivePowerL1().required(), ess.setActivePowerL2().required(),
				ess.setActivePowerL3().required(), ess.setReactivePowerL1().required(),
				ess.setReactivePowerL2().required(), ess.setReactivePowerL3().required());
	}

}
