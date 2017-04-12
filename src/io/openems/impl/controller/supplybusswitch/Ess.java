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
package io.openems.impl.controller.supplybusswitch;

import java.util.Optional;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.SymmetricPower;

@IsThingMap(type = SymmetricEssNature.class)
public class Ess extends ThingMap {

	public final WriteChannel<Long> setActivePower;
	public final WriteChannel<Long> setReactivePower;
	public final ReadChannel<Long> soc;
	public final ReadChannel<Long> activePower;
	public final ReadChannel<Long> reactivePower;
	public final ReadChannel<Long> allowedCharge;
	public final ReadChannel<Long> allowedDischarge;
	public final ReadChannel<Long> gridMode;
	public final SymmetricPower power;
	public final ReadChannel<Integer> minSoc;
	public final WriteChannel<Long> setWorkState;
	public final ReadChannel<Long> systemState;
	private Supplybus activeSupplybus;

	public Supplybus getActieSupplybus() {
		return activeSupplybus;
	}

	public void setActiveSupplybus(Supplybus supplybus) {
		this.activeSupplybus = supplybus;
	}

	public Ess(SymmetricEssNature ess) {
		super(ess);
		setActivePower = ess.setActivePower().required();
		setReactivePower = ess.setReactivePower().required();
		reactivePower = ess.reactivePower();
		soc = ess.soc().required();
		minSoc = ess.minSoc().required();
		activePower = ess.activePower().required();
		allowedCharge = ess.allowedCharge().required();
		allowedDischarge = ess.allowedDischarge().required();
		gridMode = ess.gridMode().required();
		systemState = ess.systemState().required();
		setWorkState = ess.setWorkState().required();
		this.power = new SymmetricPower(allowedDischarge, allowedCharge, ess.allowedApparent(), setActivePower, setReactivePower,
				1, 1);
	}

	public long useableSoc() throws InvalidValueException {
		return soc.value() - minSoc.value();
	}

	public void start() throws WriteChannelException {
		if (systemState.labelOptional().isPresent() && systemState.labelOptional().equals(Optional.of(EssNature.OFF))) {
			setWorkState.pushWriteFromLabel(EssNature.ON);
		}
	}

	public void stop() throws WriteChannelException {
		if (systemState.labelOptional().isPresent() && systemState.labelOptional().equals(Optional.of(EssNature.ON))) {
			setWorkState.pushWriteFromLabel(EssNature.OFF);
		}
	}

}
