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
package io.openems.impl.controller.symmetric.cosphicharacteristic;

import java.util.ArrayList;
import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;
import io.openems.core.utilities.Point;

@ThingInfo(title = "Cos-Phi Characteristics (Symmetric)")
public class CosPhiCharacteristicController extends Controller {

	/*
	 * Constructors
	 */
	public CosPhiCharacteristicController() {
		super();
	}

	public CosPhiCharacteristicController(String id) {
		super(id);
	}

	/*
	 * Config
	 */
	@ConfigInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<>("ess", this);

	@ConfigInfo(title = "Cos-Phi characteristic", description = "The points of the characteristic (x = PowerRatio, y = cosPhi).", type = Long[].class, isArray = true)
	public ConfigChannel<List<Long[]>> cosPhiPoints = new ConfigChannel<List<Long[]>>("cosPhiPoints", this)
			.addChangeListener((channel, newValue, oldValue) -> {
				List<Point> points = new ArrayList<>();
				if (newValue.isPresent()) {
					List<Long[]> cosPhiPoints = (List<Long[]>) newValue.get();
					for (Long[] arr : cosPhiPoints) {
						points.add(new Point(arr[0], arr[1]));
					}
				} else {
					log.error("found no cosPhiPoints!");
				}
				cosPhiCharacteristic = points;
			});

	/*
	 * Fields
	 */
	public List<Point> cosPhiCharacteristic;

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			if (ess.value().setActivePower.peekWrite().isPresent()) {
				double pRatio = (double) ess.value().setActivePower.peekWrite().get()
						/ (double) ess.value().nominalPower.value() * 100;
				double cosPhi = ControllerUtils.getValueOfLine(cosPhiCharacteristic, pRatio) / 100;
				ess.value().power.setReactivePower(
						ControllerUtils.calculateReactivePower(ess.value().setActivePower.peekWrite().get(), cosPhi));
				ess.value().power.writePower();
				log.info("Set reactive power [{}] to get cosPhi [{}]",
						new Object[] { ess.value().power.getReactivePower(), cosPhi });
			} else {
				log.error(ess.id() + " no ActivePower is Set.");
			}
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		}
	}

}
