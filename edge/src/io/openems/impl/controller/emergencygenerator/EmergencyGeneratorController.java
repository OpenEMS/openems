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
package io.openems.impl.controller.emergencygenerator;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.ThingRepository;

@ThingInfo(title = "External generator control", description = "Starts an external generator in case of emergency.")
public class EmergencyGeneratorController extends Controller {

	/*
	 * Constructors
	 */
	public EmergencyGeneratorController() {
		super();
	}

	public EmergencyGeneratorController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Grid-meter", description = "Sets the grid-meter to detect if the system is Off-Grid or On-Grid.", type = Meter.class)
	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ChannelInfo(title = "Min-SOC", description = "If the SOC falls under this value and the system is Off-Grid the generator starts.", type = Long.class)
	public ConfigChannel<Long> minSoc = new ConfigChannel<Long>("minSoc", this);

	@ChannelInfo(title = "Max-SOC", description = "If the system is Off-Grid and the generator is running, the generator stops if the SOC level increases over the Max-SOC.", type = Long.class)
	public ConfigChannel<Long> maxSoc = new ConfigChannel<Long>("maxSoc", this);

	@ChannelInfo(title = "Invert-Output", description = "True if the digital output should be inverted.", type = Boolean.class)
	public ConfigChannel<Boolean> invertOutput = new ConfigChannel<>("invertOutput", this);

	@ChannelInfo(title = "On-Grid output on", description = "This value indicates if the system is On-Grid to start(true) or stop(false) the generator.", type = Boolean.class, isOptional = true)
	public ConfigChannel<Boolean> onGridOutputOn = new ConfigChannel<Boolean>("onGridOutputOn", this)
			.defaultValue(false);

	/*
	 * Fields
	 */
	private ThingRepository repo = ThingRepository.getInstance();
	private WriteChannel<Boolean> outputChannel;
	private boolean generatorOn = false;
	private long lastPower = 0l;
	private Long cooldownStartTime = null;

	/*
	 * Methods
	 */
	@SuppressWarnings("unchecked")
	@ChannelInfo(title = "the address of the Digital Output where the generator is connected to.", type = String.class)
	public ConfigChannel<String> outputChannelAddress = new ConfigChannel<String>("outputChannelAddress", this)
			.addChangeListener((channel, newValue, oldValue) -> {
				Optional<String> channelAddress = (Optional<String>) newValue;
				if (channelAddress.isPresent()) {
					Optional<Channel> ch = repo.getChannelByAddress(channelAddress.get());
					if (ch.isPresent()) {
						outputChannel = (WriteChannel<Boolean>) ch.get();
					} else {
						log.error("Channel " + channelAddress.get() + " not found");
					}
				} else {
					log.error("'outputChannelAddress' is not configured!");
				}
			});

	@Override
	public void run() {
		try {
			// Check if grid is available
			if (!meter.value().voltage.valueOptional().isPresent()) {
				// no meassurable voltage => Off-Grid
				if (ess.value().gridMode.labelOptional().equals(Optional.of(EssNature.OFF_GRID)) && !generatorOn
						&& ess.value().soc.value() <= minSoc.value()) {
					// switch generator on
					startGenerator();
					generatorOn = true;
					System.out.println("1: storage is empty. Start generator.");
				} else if (ess.value().gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID)) && generatorOn
						&& ess.value().soc.value() >= maxSoc.value()) {
					// switch generator off
					if (cooldownStartTime == null) {
						cooldownStartTime = System.currentTimeMillis();
						ess.value().setActivePowerL1.pushWrite(0l);
						ess.value().setActivePowerL2.pushWrite(0l);
						ess.value().setActivePowerL3.pushWrite(0l);
						log.info("Start cooldownphase.");
					} else if (cooldownStartTime + 1000 * 60 < System.currentTimeMillis()) {
						stopGenerator();
						generatorOn = false;
						lastPower = 0l;
						cooldownStartTime = null;
						System.out.println("Storage is full. Stop generator.");
					}
				} else if (generatorOn) {
					startGenerator();
					if (ess.value().gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
						if (lastPower > -1000) {
							lastPower -= 20l;
						}
						ess.value().setActivePowerL1.pushWrite(lastPower);
						ess.value().setActivePowerL2.pushWrite(lastPower);
						ess.value().setActivePowerL3.pushWrite(lastPower);
						System.out.println("Charge with " + lastPower * 3 + " kW");
					}
					System.out.println("3: ");
				} else if (!generatorOn) {
					stopGenerator();
					lastPower = 0l;
					System.out.println("4: ");
				}
			} else {
				// Grid voltage is in the allowed range
				if (onGridOutputOn.value()) {
					startGenerator();
				} else {
					stopGenerator();
				}
			}
		} catch (InvalidValueException e) {
			log.error("Failed to read value!", e);
		} catch (WriteChannelException e) {
			log.error("Error due write to output [" + outputChannelAddress.valueOptional().orElse("<none>") + "]", e);
		}
	}

	private void startGenerator() throws WriteChannelException, InvalidValueException {
		if (outputChannel.value() != true ^ invertOutput.value()) {
			outputChannel.pushWrite(true ^ invertOutput.value());
		}
	}

	private void stopGenerator() throws InvalidValueException, WriteChannelException {
		if (outputChannel.value() != false ^ invertOutput.value()) {
			outputChannel.pushWrite(false ^ invertOutput.value());
		}
	}

}
