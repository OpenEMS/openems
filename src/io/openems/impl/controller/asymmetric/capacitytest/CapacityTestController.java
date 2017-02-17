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
import java.util.List;
import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

@ThingInfo(title = "Battery capacity test (Asymmetric)", description = "Executes a capacity test. For asymmetric Ess.")
public class CapacityTestController extends Controller {

	/*
	 * Config
	 */
	@ConfigInfo(title = "power to discharge ess", type = Integer.class)
	public ConfigChannel<Integer> power = new ConfigChannel<Integer>("power", this).defaultValue(750);

	@ConfigInfo(title = "path to save the logfile", type = String.class)
	public ConfigChannel<String> logPath = new ConfigChannel<String>("logPath", this);

	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this, Ess.class);

	private FileWriter fw;

	public CapacityTestController() {
		super();
		initialize();
	}

	public CapacityTestController(String thingId) {
		super(thingId);
		initialize();
	}

	private void initialize() {
		logPath.addUpdateListener(new ChannelUpdateListener() {

			@Override
			public void channelUpdated(Channel channel, Optional<?> newValue) {
				try {
					if (fw != null) {
						fw.close();
					}
					fw = new FileWriter(logPath.value());
					fw.write(
							"time;activePowerL1;activePowerL2;activePowerL3;batteryCurrent;batteryPower;batteryVoltage;voltageL1;voltageL2;voltageL3;currentL1;currentL2;currentL3;soc;totalBatteryChargeEnergy;totalBatteryDischargeEnergy\n");
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
				if (!ess.empty) {
					ess.setActivePowerL1.pushWrite((long) power.value());
					ess.setActivePowerL2.pushWrite((long) power.value());
					ess.setActivePowerL3.pushWrite((long) power.value());
					ess.setWorkState.pushWriteFromLabel(EssNature.START);
					fw.append(System.currentTimeMillis() + ";" + ess.activePowerL1.value() + ";"
							+ ess.activePowerL2.value() + ";" + ess.activePowerL3.value() + ";"
							+ ess.batteryCurrent.value() + ";" + ess.batteryPower.value() + ";"
							+ ess.batteryVoltage.value() + ";" + ess.voltageL1.value() + ";" + ess.voltageL2.value()
							+ ";" + ess.voltageL3.value() + ";" + ess.currentL1.value() + ";" + ess.currentL2.value()
							+ ";" + ess.currentL3.value() + ";" + ess.soc.value() + ";"
							+ ess.totalBatteryChargeEnergy.value() + ";" + ess.totalBatteryDischargeEnergy.value()
							+ "\n");
					fw.flush();
				}
				if (ess.soc.value() <= ess.minSoc.value()) {
					ess.empty = true;
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

}
