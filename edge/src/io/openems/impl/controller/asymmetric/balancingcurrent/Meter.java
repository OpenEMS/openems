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
package io.openems.impl.controller.asymmetric.balancingcurrent;

import io.openems.api.channel.ReadChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.exception.InvalidValueException;

@IsThingMap(type = AsymmetricMeterNature.class)
public class Meter extends ThingMap {

	public final ReadChannel<Long> currentL1;
	public final ReadChannel<Long> currentL2;
	public final ReadChannel<Long> currentL3;
	public final ReadChannel<Long> voltageL1;
	public final ReadChannel<Long> voltageL2;
	public final ReadChannel<Long> voltageL3;
	public final ReadChannel<Long> activePowerL1;
	public final ReadChannel<Long> reactivePowerL1;
	public final ReadChannel<Long> activePowerL2;
	public final ReadChannel<Long> reactivePowerL2;
	public final ReadChannel<Long> activePowerL3;
	public final ReadChannel<Long> reactivePowerL3;

	public Meter(AsymmetricMeterNature meter) {
		super(meter);
		currentL1 = meter.currentL1().required();
		currentL2 = meter.currentL2().required();
		currentL3 = meter.currentL3().required();
		voltageL1 = meter.voltageL1().required();
		voltageL2 = meter.voltageL2().required();
		voltageL3 = meter.voltageL3().required();
		activePowerL1 = meter.activePowerL1();
		activePowerL2 = meter.activePowerL2();
		activePowerL3 = meter.activePowerL3();
		reactivePowerL1 = meter.reactivePowerL1();
		reactivePowerL2 = meter.reactivePowerL2();
		reactivePowerL3 = meter.reactivePowerL3();
	}

	public Long getActivePower() throws InvalidValueException {
		return activePowerL1.value() + activePowerL2.value() + activePowerL3.value();
	}

	public Long getReactivePower() throws InvalidValueException {
		return reactivePowerL1.value() + reactivePowerL2.value() + reactivePowerL3.value();
	}

}
