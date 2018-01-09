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
import io.openems.api.channel.thingstate.ThingStateChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;
import io.openems.core.utilities.Point;
import io.openems.core.utilities.SymmetricPower;

@ThingInfo(title = "Voltage characteristics (Symmetric)")
public class VoltageCharacteristicController extends Controller {

	private ThingStateChannel thingState = new ThingStateChannel(this);
	/*
	 * Constructors
	 */
	public VoltageCharacteristicController() {
		super();
		initialize();
	}

	public VoltageCharacteristicController(String thingId) {
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

	@ChannelInfo(title = "ActivePower characteristics", description = "Characteristic points for active power.", type = Long[].class, isArray = true)
	public final ConfigChannel<List<Long[]>> pByUCharacteristicPoints = new ConfigChannel<>("pByUCharacteristicPoints",
			this);

	@ChannelInfo(title = "ReactivePower characteristics", description = "Characteristic points for reactive power.", type = Long[].class, isArray = true)
	public final ConfigChannel<List<Long[]>> qByUCharacteristicPoints = new ConfigChannel<>("qByUCharacteristicPoints",
			this);

	@ChannelInfo(title = "Enable ActivePower", description = "Indicates if active power characteristic is enabled.", type = Boolean.class, defaultValue = "true")
	public final ConfigChannel<Boolean> activePowerActivated = new ConfigChannel<>("activePowerActivated", this);

	@ChannelInfo(title = "Enable ReactivePower", description = "Indicates if reactive power characteristic is enabled.", type = Boolean.class, defaultValue = "true")
	public final ConfigChannel<Boolean> reactivePowerActivated = new ConfigChannel<>("reactivePowerActivated", this);

	/*
	 * Fields
	 */
	private List<Point> pCharacteristic;
	private List<Point> qCharacteristic;

	/*
	 * Methods
	 */
	private void initialize() {
		pByUCharacteristicPoints.addChangeListener(new ChannelChangeListener() {

			@Override
			public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
				try {
					List<Point> points = new ArrayList<>();
					for (Long[] arr : pByUCharacteristicPoints.value()) {
						points.add(new Point(arr[0], arr[1]));
					}
					pCharacteristic = points;
				} catch (InvalidValueException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
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
			SymmetricPower power = ess.value().power;
			double uRatio = (double) meter.value().voltage.value() / (double) uNenn.value() * 100.0;
			long nominalActivePower = ess.value().maxNominalPower.value();
			long nominalReactivePower = ess.value().maxNominalPower.value();
			power.setActivePower(
					(long) (nominalActivePower / 100.0 * ControllerUtils.getValueOfLine(pCharacteristic, uRatio)));
			power.setReactivePower(
					(long) (nominalReactivePower / 100.0 * ControllerUtils.getValueOfLine(qCharacteristic, uRatio)));
			power.writePower();
			log.info(ess.id() + " Set ActivePower [" + power.getActivePower() + "], ReactivePower ["
					+ power.getReactivePower() + "]");
		} catch (InvalidValueException e) {
			log.error("Failed to read Value.", e);
		}
	}

	@Override
	public ThingStateChannel getStateChannel() {
		return this.thingState;
	}

}
