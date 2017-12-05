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

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

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
	public final ReadChannel<Integer> minSoc;
	public final WriteChannel<Long> setWorkState;
	public final ReadChannel<Long> systemState;
	private Supplybus activeSupplybus;
	private WorkState currentState;

	enum WorkState {
		START, STOP, STANDBY
	}

	public Supplybus getActiveSupplybus() {
		return activeSupplybus;
	}

	public void setActiveSupplybus(Supplybus supplybus) {
		this.activeSupplybus = supplybus;
		log.info(this.id()+": set ActiveSupplyBus: "+(this.activeSupplybus != null ? this.activeSupplybus.getName():"null"));
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
	}

	public long useableSoc() throws InvalidValueException {
		return soc.value() - minSoc.value();
	}

	public void start() {
		currentState = WorkState.START;
		// if (systemState.labelOptional().isPresent() &&
		// (systemState.labelOptional().equals(Optional.of(EssNature.STOP))
		// || systemState.labelOptional().equals(Optional.of(EssNature.STANDBY)))) {
		// setWorkState.pushWriteFromLabel(EssNature.START);
		// }
	}

	public void standby() {
		currentState = WorkState.STANDBY;
		// if (systemState.labelOptional().isPresent()
		// && systemState.labelOptional().equals(Optional.of(EssNature.START))) {
		// setWorkState.pushWriteFromLabel(EssNature.STANDBY);
		// }
	}

	public void setWorkState() throws WriteChannelException {
		if (currentState != null) {
			switch (currentState) {
			case STANDBY:
				setWorkState.pushWriteFromLabel(EssNature.STANDBY);
				break;
			case START:
				setWorkState.pushWriteFromLabel(EssNature.START);
				break;
			case STOP:
				setWorkState.pushWriteFromLabel(EssNature.STOP);
				break;
			default:
				setWorkState.pushWriteFromLabel(EssNature.START);
				break;
			}
		}
	}

}
