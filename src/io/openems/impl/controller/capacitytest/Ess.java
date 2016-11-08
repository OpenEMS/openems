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
package io.openems.impl.controller.capacitytest;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.impl.device.pro.FeneconProEss;

@IsThingMap(type = FeneconProEss.class)
public class Ess extends ThingMap {
	public ReadChannel<Long> soc;
	public ReadChannel<Long> activePowerL1;
	public ReadChannel<Long> activePowerL2;
	public ReadChannel<Long> activePowerL3;
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
	public ReadChannel<Long> totalBatteryChargeEnergy;
	public ReadChannel<Long> totalBatteryDischargeEnergy;
	public ReadChannel<Long> currentL1;
	public ReadChannel<Long> currentL2;
	public ReadChannel<Long> currentL3;
	public ReadChannel<Long> voltageL1;
	public ReadChannel<Long> voltageL2;
	public ReadChannel<Long> voltageL3;
	public ReadChannel<Long> batteryCurrent;
	public ReadChannel<Long> batteryVoltage;
	public ReadChannel<Long> batteryPower;

	public Ess(FeneconProEss ess) {
		super(ess);
		activePowerL1 = ess.activePowerL1().required();
		activePowerL2 = ess.activePowerL2().required();
		activePowerL3 = ess.activePowerL3().required();
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
		totalBatteryChargeEnergy = ess.totalBatteryChargeEnergy.required();
		totalBatteryDischargeEnergy = ess.totalBatteryDischargeEnergy.required();
		currentL1 = ess.currentL1.required();
		currentL2 = ess.currentL2.required();
		currentL3 = ess.currentL3.required();
		voltageL1 = ess.voltageL1.required();
		voltageL2 = ess.voltageL2.required();
		voltageL3 = ess.voltageL3.required();
		batteryCurrent = ess.batteryCurrent.required();
		batteryPower = ess.batteryPower.required();
		batteryVoltage = ess.batteryVoltage.required();
	}
}
