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
package io.openems.impl.controller.teststuder;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class StuderTestController extends Controller {
	@ConfigInfo(title = "Charger", type = Charger.class)
	public ConfigChannel<Charger> charger = new ConfigChannel<>("charger", this);

	@Override
	public void run() {
		try {
			Charger charger = this.charger.value();
			log.info("Charger[" + charger.batteryChargeCurrentValue.valueOptional() + "]");
			log.info("Charger[" + charger.batteryChargeCurrentUnsavedValue.valueOptional() + "]");
			charger.batteryChargeCurrentUnsavedValue.pushWrite(15f);
		} catch (WriteChannelException | InvalidValueException e) {
			e.printStackTrace();
		}
	}
}
