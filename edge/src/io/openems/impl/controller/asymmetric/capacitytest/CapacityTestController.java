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
package io.openems.impl.controller.asymmetric.capacitytest;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

@ThingInfo(title = "Battery capacity test (Asymmetric)", description = "Executes a capacity test. For asymmetric Ess.")
public class CapacityTestController extends Controller {

	private ThingStateChannel thingState = new ThingStateChannel(this);
	/*
	 * Constructors
	 */
	public CapacityTestController() {
		super();
		initialize();
	}

	public CapacityTestController(String thingId) {
		super(thingId);
		initialize();
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Power", description = "Discharge power of Ess.", type = Integer.class, defaultValue = "750")
	public ConfigChannel<Integer> power = new ConfigChannel<Integer>("power", this);

	@ChannelInfo(title = "Log-File", description = "Path to save the logfile.", type = String.class)
	public ConfigChannel<String> logPath = new ConfigChannel<String>("logPath", this);

	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this);

	/*
	 * Fields
	 */
	private FileWriter fw;

	/*
	 * Methods
	 */
	private void initialize() {
		logPath.addUpdateListener(new ChannelUpdateListener() {

			@Override
			public void channelUpdated(Channel channel, Optional<?> newValue) {
				try {
					if (fw != null) {
						fw.close();
					}
					fw = new FileWriter(logPath.value());
					fw.write("time;activePowerL1;activePowerL2;activePowerL3;soc\n");
				} catch (IOException e) {
					log.error(e.getMessage());
				} catch (InvalidValueException e) {
					log.error(e.getMessage());
				}

			}
		});
	}

	@Override
	public void run() {
		try {
			for (Ess ess : esss.value()) {
				ess.setWorkState.pushWriteFromLabel(EssNature.START);
				if (ess.empty) {
					// Capacitytest
					if (ess.full) {
						// fully discharge ess
						ess.setActivePowerL1.pushWrite((long) power.value());
						ess.setActivePowerL2.pushWrite((long) power.value());
						ess.setActivePowerL3.pushWrite((long) power.value());
					} else {
						// fully charge ess
						ess.setActivePowerL1.pushWrite((long) power.value() * -1);
						ess.setActivePowerL2.pushWrite((long) power.value() * -1);
						ess.setActivePowerL3.pushWrite((long) power.value() * -1);
						if (ess.allowedCharge.value() <= 100l
								&& ess.systemState.labelOptional().equals(Optional.of(EssNature.START))) {
							ess.full = true;
						}
					}
					fw.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ";"
							+ ess.activePowerL1.value() + ";" + ess.activePowerL2.value() + ";"
							+ ess.activePowerL3.value() + ";" + ess.soc.value() + "\n");
					fw.flush();
				} else {
					// prepare for capacityTest
					// Empty ess
					ess.setActivePowerL1.pushWrite(ess.allowedDischarge.value() / 3);
					ess.setActivePowerL2.pushWrite(ess.allowedDischarge.value() / 3);
					ess.setActivePowerL3.pushWrite(ess.allowedDischarge.value() / 3);
					if (ess.allowedDischarge.value() <= 100l
							&& ess.systemState.labelOptional().equals(Optional.of(EssNature.START))) {
						ess.empty = true;
					}
				}
			}
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		} catch (WriteChannelException e) {
			log.error(e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public ThingStateChannel getStateChannel() {
		return this.thingState;
	}

}
