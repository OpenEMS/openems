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
package io.openems.impl.controller.evcs;

import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;

@IsThingMap(type = EssNature.class)
public class Ess extends ThingMap {
	private final EssNature ess;

	public Ess(EssNature ess) {
		super(ess);
		this.ess = ess;

		if (ess instanceof AsymmetricEssNature) {
			AsymmetricEssNature e = (AsymmetricEssNature) ess;
			e.activePowerL1().required();
			e.activePowerL2().required();
			e.activePowerL3().required();
		} else if (ess instanceof SymmetricEssNature) {
			SymmetricEssNature e = (SymmetricEssNature) ess;
			e.activePower().required();
		}
	}

	public long getActivePowerSum() {
		if (this.ess instanceof AsymmetricEssNature) {
			AsymmetricEssNature e = (AsymmetricEssNature) ess;
			return e.activePowerL1().valueOptional().orElse(0l) //
					+ e.activePowerL2().valueOptional().orElse(0l) //
					+ e.activePowerL3().valueOptional().orElse(0l);
		} else if (ess instanceof SymmetricEssNature) {
			SymmetricEssNature e = (SymmetricEssNature) ess;
			return e.activePower().valueOptional().orElse(0l);
		}
		return 0;
	}
}
