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
package io.openems.api.device.nature.meter;

import java.util.Optional;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.doc.ChannelInfo;

public interface SymmetricMeterNature extends MeterNature {

	/*
	 * ReadChannels
	 */
	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> activePower();

	@ChannelInfo(title = "maxActivePower", description = "Holds the maximum ever active power.", type = Long.class, defaultValue = "0")
	public ConfigChannel<Long> maxActivePower();

	@ChannelInfo(title = "minActivePower", description = "Holds the minimum ever active power.", type = Long.class, defaultValue = "0")
	public ConfigChannel<Long> minActivePower();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> apparentPower();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> reactivePower();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> frequency();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> voltage();

	public default void updateMinMaxSymmetricActivePower() {
		Optional<Long> activePowerOptional = this.activePower().valueOptional();
		Optional<Long> maxActivePower = this.maxActivePower().valueOptional();
		Optional<Long> minActivePower = this.minActivePower().valueOptional();
		long activePower = activePowerOptional.orElse(0L);
		if (maxActivePower.orElse(Long.MIN_VALUE) < activePower) {
			maxActivePower().updateValue(activePower, true);
		}
		if (minActivePower.orElse(Long.MAX_VALUE) > activePower) {
			minActivePower().updateValue(activePower, true);
		}
	}
}
