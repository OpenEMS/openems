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

import java.util.List;

import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMapping;
import io.openems.api.exception.WriteChannelException;

public class BalancingController extends Controller {
	@IsThingMapping
	public List<EssMap> esss = null;

	@IsThingMapping
	public MeterMap meter;

	public boolean isOnGrid() {
		// for (EssMap ess : esss) {
		// if (!ess.isOnGrid()) {
		// return false;
		// }
		// }
		return true;
	}

	@Override
	public void run() {
		if (isOnGrid()) {
			for (EssMap ess : esss) {
				try {
					// lastValue = lastValue.add(Long.valueOf(100));
					ess.setActivePower.pushWriteValue(0);
				} catch (WriteChannelException e) {
					log.error(e.getMessage());
				}
			}

			// int calculatedPower = meter.activePower.getValue().intValue();
			// int maxChargePower = 0;
			// int maxDischargePower = 0;
			// int useableSoc = 0;
			// for (EssMap ess : esss) {
			// calculatedPower += ess.activePower.getValue().intValue();
			// maxChargePower += ess.allowedCharge.getValue().intValue();
			// maxDischargePower += ess.allowedDischarge.getValue().intValue();
			// useableSoc += ess.getUseableSoc();
			// }
			// if (calculatedPower > 0) {
			// if (calculatedPower > maxDischargePower) {
			// calculatedPower = maxChargePower;
			// }
			// Collections.sort(esss, (e1, e2) -> e1.getUseableSoc() - e2.getUseableSoc());
			// for (int i = 0; i < esss.size(); i++) {
			// EssMap ess = esss.get(i);
			// int minP = calculatedPower;
			// for (int j = i + 1; j < esss.size(); j++) {
			// if (esss.get(j).getUseableSoc() > 0) {
			// minP -= esss.get(j).activePower.getMinWriteValue().asInt();
			// }
			// }
			// if (minP < 0) {
			// minP = 0;
			// }
			// int maxP = e.activePower.getMinWriteValue().asInt();
			// if (calculatedPower < maxP) {
			// maxP = calculatedPower;
			// }
			// double diff = maxP - minP;
			// // if (e.getUseableSoc() >= 0) {
			// int p = (int) Math.ceil((minP + diff / useableSoc * e.getUseableSoc()) / 100) * 100;
			// e.setActivePower(p);
			// calculatedPower -= p;
			// // }
			// }
			// } else {
			// if (calculatedPower < maxChargePower) {
			// calculatedPower = maxChargePower;
			// }
			// Collections.sort(ess, (a, b) -> (100 - a.getUseableSoc()) - (100 - b.getUseableSoc()));
			// for (int i = 0; i < ess.size(); i++) {
			// EssContainer e = ess.get(i);
			// int minP = calculatedPower;
			// for (int j = i + 1; j < ess.size(); j++) {
			// minP -= ess.get(j).activePower.getMinWriteValue().asInt();
			// }
			// if (minP > 0) {
			// minP = 0;
			// }
			// int maxP = e.activePower.getMinWriteValue().asInt();
			// if (calculatedPower > maxP) {
			// maxP = calculatedPower;
			// }
			// double diff = maxP - minP;
			// int p = (int) Math.floor((minP + diff / useableSoc * (100 - e.getUseableSoc())) / 100) * 100;
			// e.setActivePower(p);
			// calculatedPower -= p;
			// }
			// }
		}
	}

}
