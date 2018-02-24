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
package io.openems.impl.controller.chargerlimitation;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.exception.WriteChannelException;

@IsThingMap(type = AsymmetricEssNature.class)
public class Ess extends ThingMap {

	public ReadChannel<Long> soc;
	public ReadChannel<Long> allowedCharge;
	public WriteChannel<Long> setActivePowerL1;
	public WriteChannel<Long> setActivePowerL2;
	public WriteChannel<Long> setActivePowerL3;
	public ReadChannel<Long> activePowerL1;
	public ReadChannel<Long> activePowerL2;
	public ReadChannel<Long> activePowerL3;

	public Ess(AsymmetricEssNature thing) {
		super(thing);
		this.soc = thing.soc().required();
		this.allowedCharge = thing.allowedCharge().required();
		this.setActivePowerL1 = thing.setActivePowerL1();
		this.setActivePowerL2 = thing.setActivePowerL2();
		this.setActivePowerL3 = thing.setActivePowerL3();
		this.activePowerL1 = thing.activePowerL1();
		this.activePowerL2 = thing.activePowerL2();
		this.activePowerL3 = thing.activePowerL3();
	}

	public Long getWrittenActivePower() {
		long writePower = setActivePowerL1.peekWrite().orElse(0l) + setActivePowerL2.peekWrite().orElse(0l)
				+ setActivePowerL3.peekWrite().orElse(0l);
		long currentPower = activePowerL1.valueOptional().orElse(0L) + activePowerL2.valueOptional().orElse(0L)
				+ activePowerL3.valueOptional().orElse(0L);
		return writePower < currentPower ? writePower : currentPower;
	}

	public void setMaxCharge(float power) throws WriteChannelException {
		power *= -1;
		setActivePowerL1.pushWriteMin((long) (power / 3f));
		setActivePowerL2.pushWriteMin((long) (power / 3f));
		setActivePowerL3.pushWriteMin((long) (power / 3f));
	}

}
