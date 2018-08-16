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
package io.openems.impl.controller.asymmetric.powerlimitation;

import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.AsymmetricEssNature;

@IsThingMap(type = AsymmetricEssNature.class)
public class Ess extends ThingMap {

	public final WriteChannel<Long> setActivePowerL1;
	public final WriteChannel<Long> setActivePowerL2;
	public final WriteChannel<Long> setActivePowerL3;
	public final WriteChannel<Long> setReactivePowerL1;
	public final WriteChannel<Long> setReactivePowerL2;
	public final WriteChannel<Long> setReactivePowerL3;
	public final String id;

	public Ess(AsymmetricEssNature ess) {
		super(ess);
		setActivePowerL1 = ess.setActivePowerL1();
		setActivePowerL2 = ess.setActivePowerL2();
		setActivePowerL3 = ess.setActivePowerL3();
		setReactivePowerL1 = ess.setReactivePowerL1();
		setReactivePowerL2 = ess.setReactivePowerL2();
		setReactivePowerL3 = ess.setReactivePowerL3();
		id = ess.id();
	}

}
