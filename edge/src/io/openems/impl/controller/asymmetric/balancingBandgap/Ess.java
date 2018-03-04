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
package io.openems.impl.controller.asymmetric.balancingBandgap;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.AsymmetricPower;

@IsThingMap(type = AsymmetricEssNature.class)
public class Ess extends ThingMap {
	public ReadChannel<Long> soc;
	public ReadChannel<Long> activePowerL1;
	public ReadChannel<Long> activePowerL2;
	public ReadChannel<Long> activePowerL3;
	public ReadChannel<Long> reactivePowerL1;
	public ReadChannel<Long> reactivePowerL2;
	public ReadChannel<Long> reactivePowerL3;
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
	public AsymmetricPower power;

	public Ess(AsymmetricEssNature ess) {
		super(ess);
		activePowerL1 = ess.activePowerL1().required();
		activePowerL2 = ess.activePowerL2().required();
		activePowerL3 = ess.activePowerL3().required();
		reactivePowerL1 = ess.reactivePowerL1().required();
		reactivePowerL2 = ess.reactivePowerL2().required();
		reactivePowerL3 = ess.reactivePowerL3().required();
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
		power = new AsymmetricPower(ess.allowedDischarge().required(), ess.allowedCharge().required(),
				ess.allowedApparent().required(), ess.setActivePowerL1().required(), ess.setActivePowerL2().required(),
				ess.setActivePowerL3().required(), ess.setReactivePowerL1().required(),
				ess.setReactivePowerL2().required(), ess.setReactivePowerL3().required());

	}

	public long useableSoc() throws InvalidValueException {
		return soc.value() - minSoc.value();
	}

	public String getSetValueLog() {
		return String.format(
				"%s : Set ActivePower L1: %d W L2: %d W L3: %d W , Set ReactivePower L1: %d Var L2: %d Var L3: %d Var",
				id(), setActivePowerL1.peekWrite().orElse(null), setActivePowerL2.peekWrite().orElse(null),
				setActivePowerL3.peekWrite().orElse(null), setReactivePowerL1.peekWrite().orElse(null),
				setReactivePowerL2.peekWrite().orElse(null), setReactivePowerL3.peekWrite().orElse(null));
	}
}
