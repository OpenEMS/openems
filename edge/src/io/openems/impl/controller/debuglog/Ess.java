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
package io.openems.impl.controller.debuglog;

import java.util.List;
import java.util.stream.Collectors;

import io.openems.api.channel.ThingStateChannel;
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

		ess.allowedCharge().required();
		ess.allowedDischarge().required();
		ess.minSoc().required();
		ess.soc().required();
		ess.systemState().required();

		if (ess instanceof AsymmetricEssNature) {
			AsymmetricEssNature e = (AsymmetricEssNature) ess;
			e.activePowerL1().required();
			e.activePowerL2().required();
			e.activePowerL3().required();
			e.reactivePowerL1().required();
			e.reactivePowerL2().required();
			e.reactivePowerL3().required();
		}
		if (ess instanceof SymmetricEssNature) {
			SymmetricEssNature e = (SymmetricEssNature) ess;
			e.activePower().required();
			e.reactivePower().required();
		}
	}

	@Override public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(ess.id() + " [" + //
				"SOC:" + ess.soc().format() + "|");
		if (ess instanceof SymmetricEssNature) {
			SymmetricEssNature e = (SymmetricEssNature) ess;
			b.append("L:" + e.activePower().format() + ";" + e.reactivePower().format());
		}
		if (ess instanceof AsymmetricEssNature && ess instanceof SymmetricEssNature) {
			b.append("|");
		}
		if (ess instanceof AsymmetricEssNature) {
			AsymmetricEssNature e = (AsymmetricEssNature) ess;
			b.append("L1:" + e.activePowerL1().format() + ";" + e.reactivePowerL1().format() + "|" + //
					"L2:" + e.activePowerL2().format() + ";" + e.reactivePowerL2().format() + "|" + //
					"L3:" + e.activePowerL3().format() + ";" + e.reactivePowerL3().format());
		}
		b.append("|" + //
				"Allowed:" + ess.allowedCharge().format() + ";" + ess.allowedDischarge().format());
		b.append("|" + //
				"GridMode:" + ess.gridMode().labelOptional().orElse("unknown"));
		List<ThingStateChannel> warningChannels = ess.getStateChannel().getWarningChannels().stream().filter(c -> c.isValuePresent() && c.getValue()).collect(Collectors.toList());
		List<ThingStateChannel> faultChannels = ess.getStateChannel().getFaultChannels().stream().filter(c -> c.isValuePresent() && c.getValue()).collect(Collectors.toList());
		if(warningChannels.size() > 0) {
			b.append("|Warn:");
			b.append(warningChannels.stream().map(c -> c.name()).collect(Collectors.joining(",")));
		}
		if(faultChannels.size() > 0) {
			b.append("|Fault:");
			b.append(faultChannels.stream().map(c -> c.name()).collect(Collectors.joining(",")));
		}
		b.append("]");
		return b.toString();
	}
}
