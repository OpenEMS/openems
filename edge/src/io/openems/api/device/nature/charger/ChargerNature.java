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
package io.openems.api.device.nature.charger;

import java.util.Optional;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;

public interface ChargerNature extends DeviceNature {

	@ChannelInfo(title = "maxActualPower", description = "Holds the maximum ever actual power.", type = Long.class, defaultValue = "0")
	public ConfigChannel<Long> maxActualPower();

	@ChannelInfo(type = Long.class)
	public WriteChannel<Long> setMaxPower();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> getActualPower();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> getNominalPower();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> getInputVoltage();

	public default void updateMaxChargerActualPower() {
		Optional<Long> actualPowerOptional = this.getActualPower().valueOptional();
		Optional<Long> maxActualPower = this.maxActualPower().valueOptional();
		long actualPower = actualPowerOptional.orElse(0L);
		if (maxActualPower.orElse(Long.MIN_VALUE) < actualPower) {
			maxActualPower().updateValue(actualPower, true);
		}
	}

}
