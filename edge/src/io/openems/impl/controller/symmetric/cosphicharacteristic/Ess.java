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
package io.openems.impl.controller.symmetric.cosphicharacteristic;

import io.openems.api.channel.ReadChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.core.utilities.power.CosPhiLineCharacteristicLimitation;
import io.openems.core.utilities.power.SymmetricPower;

@IsThingMap(type = SymmetricEssNature.class)
public class Ess extends ThingMap {

	public final String id;
	public final ReadChannel<Long> allowedCharge;
	public final ReadChannel<Long> allowedDischarge;
	public final SymmetricPower power;
	public final ReadChannel<Long> nominalPower;
	public final CosPhiLineCharacteristicLimitation limit;

	public Ess(SymmetricEssNature ess) {
		super(ess);
		id = ess.id();
		allowedCharge = ess.allowedCharge();
		allowedDischarge = ess.allowedDischarge();
		nominalPower = ess.maxNominalPower();
		power = ess.getPower();
		limit = new CosPhiLineCharacteristicLimitation(power);
	}

}
