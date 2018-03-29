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
package io.openems.impl.controller.symmetric.capacitytest;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.power.symmetric.PowerException;

@ThingInfo(title = "Battery capacity test (Symmetric)", description = "Executes a capacity test. For symmetric Ess.")
public class CapacityTestController extends Controller {

	private final Logger log = LoggerFactory.getLogger(CapacityTestController.class);

	private ThingStateChannels thingState = new ThingStateChannels(this);
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

	@ChannelInfo(title = "Sleep", description = "Time to sleep after empty ess before start capacityTest.", type = Integer.class, defaultValue = "750")
	public ConfigChannel<Integer> sleep = new ConfigChannel<Integer>("sleep", this);

	@ChannelInfo(title = "Log-File", description = "Path to save the logfile.", type = String.class)
	public ConfigChannel<String> logPath = new ConfigChannel<String>("logPath", this);

	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this);

	private Long start = null;

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
					fw.write("time;activePower;soc\n");
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
		if (start == null) {
			start = System.currentTimeMillis();
		}
		if (start != null && start + 5000 <= System.currentTimeMillis()) {
			try {
				for (Ess ess : esss.value()) {
					ess.setWorkState.pushWriteFromLabel(EssNature.START);
					if (ess.empty) {
						if (ess.timeEmpty + sleep.value() <= System.currentTimeMillis()) {
							// Capacitytest
							if (ess.full) {
								// fully discharge ess
								ess.limit.setP((long)power.value());
								ess.power.applyLimitation(ess.limit);
							} else {
								// fully charge ess
								ess.limit.setP((long) power.value() * -1);
								ess.power.applyLimitation(ess.limit);
								if (ess.allowedCharge.value() >= -100l
										&& ess.systemState.labelOptional().equals(Optional.of(EssNature.START))) {
									ess.full = true;
								}
							}
							fw.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
									+ ";" + ess.activePower.value() + ";" + ess.soc.value() + "\n");
							fw.flush();
						}
					} else {
						// prepare for capacityTest
						// Empty ess
						ess.limit.setP(ess.allowedDischarge.value());
						ess.power.applyLimitation(ess.limit);
						if (ess.soc.value() <= ess.minSoc.value()) {
							ess.empty = true;
							ess.timeEmpty = System.currentTimeMillis();
						}
					}
				}
			} catch (InvalidValueException e) {
				log.error(e.getMessage());
			} catch (IOException e) {
				log.error(e.getMessage());
			} catch (PowerException e) {
				log.error("Failed to set Power!",e);
			} catch (WriteChannelException e) {
				log.error(e.getMessage());
			}
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

}
