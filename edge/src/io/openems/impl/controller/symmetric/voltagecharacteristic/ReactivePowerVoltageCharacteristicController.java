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
package io.openems.impl.controller.symmetric.voltagecharacteristic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;
import io.openems.core.utilities.Point;
import io.openems.core.utilities.power.PowerException;

@ThingInfo(title = "Voltage characteristics (Symmetric)")
public class ReactivePowerVoltageCharacteristicController extends Controller {

	/*
	 * Constructors
	 */
	public ReactivePowerVoltageCharacteristicController() {
		super();
		initialize();
	}

	public ReactivePowerVoltageCharacteristicController(String thingId) {
		super(thingId);
		initialize();
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Meter", description = "The meter to measure the Voltage.", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ChannelInfo(title = "Nominal voltage", description = "The nominal voltage of the grid.", type = Integer.class)
	public final ConfigChannel<Integer> uNenn = new ConfigChannel<>("UNenn", this);

	@ChannelInfo(title = "ReactivePower characteristics", description = "Characteristic points for reactive power.", type = Long[].class, isArray = true)
	public final ConfigChannel<List<Long[]>> qByUCharacteristicPoints = new ConfigChannel<>("qByUCharacteristicPoints",
			this);

	/*
	 * Fields
	 */
	private List<Point> qCharacteristic;

	/*
	 * Methods
	 */
	private void initialize() {
		qByUCharacteristicPoints.addChangeListener(new ChannelChangeListener() {

			@Override
			public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
				try {
					List<Point> points = new ArrayList<>();
					for (Long[] arr : qByUCharacteristicPoints.value()) {
						points.add(new Point(arr[0], arr[1]));
					}
					qCharacteristic = points;
				} catch (InvalidValueException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			double uRatio = (double) meter.value().voltage.value() / (double) uNenn.value() * 100.0;
			long nominalReactivePower = ess.maxNominalPower.value();
			ess.reactivePowerLimit.setQ((long) (nominalReactivePower / 100.0 * ControllerUtils.getValueOfLine(qCharacteristic, uRatio)));
			ess.power.applyLimitation(ess.reactivePowerLimit);
		} catch (InvalidValueException e) {
			log.error("Failed to read Value.", e);
		} catch (PowerException e) {
			log.error("Failed to set Power!",e);
		}
	}

}
