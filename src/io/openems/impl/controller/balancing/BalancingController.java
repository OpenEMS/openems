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
package io.openems.impl.controller.balancing;

import java.util.Collections;
import java.util.List;

import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMapping;
import io.openems.api.device.nature.EssNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class BalancingController extends Controller {
	@IsThingMapping
	public List<Ess> esss = null;

	@IsThingMapping
	public Meter meter;

	@Override
	public void run() {
		try {
			if (isOnGrid()) {
				long calculatedPower = meter.activePower.getValue();
				long maxChargePower = 0;
				long maxDischargePower = 0;
				long useableSoc = 0;
				for (Ess ess : esss) {
					calculatedPower += ess.activePower.getValue();
					maxChargePower += ess.allowedCharge.getValue();
					maxDischargePower += ess.allowedDischarge.getValue();
					useableSoc += ess.useableSoc();
				}
				if (calculatedPower > 0) {
					/*
					 * Discharge
					 */
					if (calculatedPower > maxDischargePower) {
						calculatedPower = maxChargePower;
					}
					Collections.sort(esss, (a, b) -> {
						try {
							return (int) (a.useableSoc() - b.useableSoc());
						} catch (InvalidValueException e) {
							log.error(e.getMessage());
							return 0;
						}
					});
					for (int i = 0; i < esss.size(); i++) {
						Ess ess = esss.get(i);
						long minP = calculatedPower;
						for (int j = i + 1; j < esss.size(); j++) {
							if (esss.get(j).useableSoc() > 0) {
								minP -= esss.get(j).allowedCharge.getValue();
							}
						}
						if (minP < 0) {
							minP = 0;
						}
						long maxP = ess.allowedCharge.getValue();
						if (calculatedPower < maxP) {
							maxP = calculatedPower;
						}
						double diff = maxP - minP;
						// if (e.getUseableSoc() >= 0) {
						long p = (long) (Math.ceil((minP + diff / useableSoc * ess.useableSoc()) / 100) * 100);
						ess.setActivePower.pushWriteValue(p);
						calculatedPower -= p;
						// }
					}
				} else {
					/*
					 * Charge
					 */
					if (calculatedPower < maxChargePower) {
						calculatedPower = maxChargePower;
					}
					Collections.sort(esss, (a, b) -> {
						try {
							return (int) ((100 - a.useableSoc()) - (100 - b.useableSoc()));
						} catch (InvalidValueException e) {
							log.error(e.getMessage());
							return 0;
						}
					});
					for (int i = 0; i < esss.size(); i++) {
						Ess ess = esss.get(i);
						long minP = calculatedPower;
						for (int j = i + 1; j < esss.size(); j++) {
							minP -= esss.get(j).allowedCharge.getValue();
						}
						if (minP > 0) {
							minP = 0;
						}
						long maxP = ess.allowedCharge.getValue();
						if (calculatedPower > maxP) {
							maxP = calculatedPower;
						}
						double diff = maxP - minP;
						long p = (long) Math.floor((minP + diff / useableSoc * (100 - ess.useableSoc())) / 100) * 100;
						ess.setActivePower.pushWriteValue(p);
						calculatedPower -= p;
					}
				}

			}
		} catch (InvalidValueException | WriteChannelException e) {
			log.error(e.getMessage());
		}
	}

	private boolean isOnGrid() {
		for (Ess ess : esss) {
			String gridMode = ess.gridMode.getValueLabelOrNull();
			if (gridMode != null && gridMode != EssNature.ON_GRID) {
				return false;
			}
		}
		return true;
	}

}
