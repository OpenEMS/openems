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
import io.openems.api.channel.WriteChannel;

public interface AsymmetricEssNature extends EssNature {
	/*
	 * ReadChannels
	 */
	public ReadChannel<Long> activePowerL1();

	public ReadChannel<Long> activePowerL2();

	public ReadChannel<Long> activePowerL3();

	public ReadChannel<Long> reactivePowerL1();

	public ReadChannel<Long> reactivePowerL2();

	public ReadChannel<Long> reactivePowerL3();

	/*
	 * WriteChannels
	 */
	public WriteChannel<Long> setActivePowerL1();

	public WriteChannel<Long> setActivePowerL2();

	public WriteChannel<Long> setActivePowerL3();

	public WriteChannel<Long> setReactivePowerL1();

	public WriteChannel<Long> setReactivePowerL2();

	public WriteChannel<Long> setReactivePowerL3();
}
