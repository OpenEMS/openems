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
package io.openems.impl.controller.api.websocket;

import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.exception.WriteChannelException;

@IsThingMap(type = EssNature.class)
public class Ess extends ThingMap {

	private EssNature ess;

	public Ess(EssNature ess) {
		super(ess);
		this.ess = ess;

		if (ess instanceof SymmetricEssNature) {
			SymmetricEssNature e = (SymmetricEssNature) ess;
			e.setActivePower().required();
			e.setReactivePower().required();
		} else {
			AsymmetricEssNature e = (AsymmetricEssNature) ess;
			e.setActivePowerL1().required();
			e.setActivePowerL2().required();
			e.setActivePowerL3().required();
			e.setReactivePowerL1().required();
			e.setReactivePowerL2().required();
			e.setReactivePowerL3().required();
		}
	}

	public void setPower(long p, long q) throws WriteChannelException {
		if (ess instanceof SymmetricEssNature) {
			SymmetricEssNature e = (SymmetricEssNature) ess;
			log.info("Set Power: " + p + ", " + q);
			e.setActivePower().pushWrite(p);
			e.setReactivePower().pushWrite(q);
		} else {
			AsymmetricEssNature e = (AsymmetricEssNature) ess;
			e.setActivePowerL1().pushWrite(p / 3);
			e.setActivePowerL2().pushWrite(p / 3);
			e.setActivePowerL3().pushWrite(p / 3);
			e.setReactivePowerL1().pushWrite(q / 3);
			e.setReactivePowerL2().pushWrite(q / 3);
			e.setReactivePowerL3().pushWrite(q / 3);
		}
	}
}
