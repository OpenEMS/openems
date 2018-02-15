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
package io.openems.impl.controller.systemstate.powerthreshold;

import java.util.ArrayList;
import java.util.List;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;

@IsThingMap(type = EssNature.class)
public class Ess extends ThingMap {

	public final WriteChannel<Long> setWorkState;
	public final ReadChannel<Long> systemState;
	public final ReadChannel<Long> soc;
	public final ReadChannel<Integer> minSoc;
	private List<ReadChannel<Long>> activePowerChannels = new ArrayList<>();

	public enum State {
		ON, OFF, UNKNOWN
	}

	public State currentState = State.UNKNOWN;

	public Ess(EssNature ess) {
		super(ess);
		setWorkState = ess.setWorkState().required();
		systemState = ess.systemState().required();
		this.minSoc = ess.minSoc().required();
		this.soc = ess.soc().required();
		if (ess instanceof SymmetricEssNature) {
			SymmetricEssNature e = (SymmetricEssNature) ess;
			activePowerChannels.add(e.activePower().required());
		} else if (ess instanceof AsymmetricEssNature) {
			AsymmetricEssNature e = (AsymmetricEssNature) ess;
			activePowerChannels.add(e.activePowerL1().required());
			activePowerChannels.add(e.activePowerL2().required());
			activePowerChannels.add(e.activePowerL3().required());
		}
	}

	public Long getPower() {
		Long power = null;
		for (ReadChannel<Long> powerChannel : activePowerChannels) {
			if (powerChannel.valueOptional().isPresent()) {
				if (power == null) {
					power = powerChannel.valueOptional().get();
				} else {
					power += powerChannel.valueOptional().get();
				}
			}
		}
		return power;
	}

}
