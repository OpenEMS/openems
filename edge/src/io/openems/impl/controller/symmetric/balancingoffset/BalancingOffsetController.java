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
package io.openems.impl.controller.symmetric.balancingoffset;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

/*
 * this Controller calculates the power consumption of the house and charges or discharges the storages to reach zero power consumption from the grid
 */
@ThingInfo(title = "Balancing offset (Symmetric)", description = "Tries to keep the grid meter within an offset. For symmetric Ess.")
public class BalancingOffsetController extends Controller {

	/*
	 * Constructors
	 */
	public BalancingOffsetController() {
		super();
		// init();
	}

	public BalancingOffsetController(String thingId) {
		super(thingId);
		// init();
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<>("ess", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<>("meter", this);

	@ChannelInfo(title = "Offset ActivePower", description = "The offset of the active power from zero to hold on the grid meter.", type = Integer.class)
	public final ConfigChannel<Integer> activePowerOffset = new ConfigChannel<>("activePowerOffset", this);

	@ChannelInfo(title = "Offset ReactivePower", description = "The offset of the reactive power from zero to hold on the grid meter.", type = Integer.class)
	public final ConfigChannel<Integer> reactivePowerOffset = new ConfigChannel<>("reactivePowerOffset", this);

	@ChannelInfo(title = "Enable ActivePower", description = "Indicates if active power is enabled.", type = Boolean.class, defaultValue = "true")
	public final ConfigChannel<Boolean> activePowerActivated = new ConfigChannel<Boolean>("activePowerActivated", this);

	@ChannelInfo(title = "Enable ReactivePower", description = "Indicates if reactive power is enabled.", type = Boolean.class, defaultValue = "true")
	public final ConfigChannel<Boolean> reactivePowerActivated = new ConfigChannel<Boolean>("reactivePowerActivated",
			this);

	/*
	 * Methods
	 */
	@Override
	public void init() {
		activePowerActivated.addChangeListener(new ChannelChangeListener() {

			@Override
			public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
				try {
					if (!activePowerActivated.value()) {
						ess.value().setActivePower.pushWrite(0L);
					}
				} catch (WriteChannelException | InvalidValueException e) {
					log.error(e.getMessage());
				}
			}
		});
		reactivePowerActivated.addChangeListener(new ChannelChangeListener() {

			@Override
			public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
				try {
					if (!reactivePowerActivated.value()) {
						ess.value().setReactivePower.pushWrite(0L);
					}
				} catch (WriteChannelException | InvalidValueException e) {
					log.error(e.getMessage());
				}
			}
		});
	}

	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			// Calculate required sum values
			long calculatedPower = meter.value().activePower.value() + ess.activePower.value()
					- activePowerOffset.value();
			long calculatedReactivePower = meter.value().reactivePower.value() + ess.reactivePower.value()
					- reactivePowerOffset.value();
			if (reactivePowerActivated.value()) {
				ess.power.setReactivePower(calculatedReactivePower);
			}
			if (activePowerActivated.value()) {
				ess.power.setActivePower(calculatedPower);
			}
			ess.power.writePower();
			// print info message to log
			String message = ess.id();
			if (activePowerActivated.value()) {
				message = message + " Set ActivePower [" + ess.power.getActivePower() + "]";
			}
			if (reactivePowerActivated.value()) {
				message = message + " Set ReactivePower [" + ess.power.getReactivePower() + "]";
			}
			log.info(message);
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}
	}

}
