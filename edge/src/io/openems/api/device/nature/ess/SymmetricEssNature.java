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
package io.openems.api.device.nature.ess;

import io.openems.api.channel.ReadChannel;
import io.openems.api.doc.ChannelInfo;
import io.openems.core.utilities.power.SymmetricPower;

public interface SymmetricEssNature extends EssNature {
	/*
	 * ReadChannels
	 */
	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> activePower();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> apparentPower();

	@ChannelInfo(type = Long.class)
	public ReadChannel<Long> reactivePower();
	// /*
	// * WriteChannels
	// */
	// @ChannelInfo(type = Long.class)
	// public WriteChannel<Long> setActivePower();
	//
	// @ChannelInfo(type = Long.class)
	// public WriteChannel<Long> setReactivePower();

	public SymmetricPower getPower();

}
