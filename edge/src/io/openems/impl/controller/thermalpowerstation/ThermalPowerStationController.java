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
package io.openems.impl.controller.thermalpowerstation;

import java.util.List;
import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.ThingRepository;

@ThingInfo(title = "Thermal power station")
public class ThermalPowerStationController extends Controller {

	private enum State {
		ON, OFF, SWITCHON, SWITCHOFF, UNDEFINED
	}

	/*
	 * Fields
	 */
	private ThingRepository repo = ThingRepository.getInstance();
	private Long lastTimeAboveProductionlimit = System.currentTimeMillis();
	public WriteChannel<Boolean> outputChannel;
	private int switchOnCount = 0;
	private int switchOffCount = 0;
	private State currentState = State.UNDEFINED;

	/*
	 * Constructors
	 */
	public ThermalPowerStationController() {
		super();
	}

	public ThermalPowerStationController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ConfigInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ConfigInfo(title = "Meters", description = "Meters of power producers (e.g. PV).", type = Meter.class, isArray = true)
	public ConfigChannel<List<Meter>> meters = new ConfigChannel<>("meters", this);

	@ConfigInfo(title = "Min-SOC", description = "If SOC falls below this min-SOC and power production is below production-limit, thermalpowerstation will start.", type = Long.class)
	public ConfigChannel<Long> minSoc = new ConfigChannel<Long>("minSoc", this);

	@ConfigInfo(title = "Max-SOC", description = "If the SOC rises above max-SOC the thermalpowerstation will stop.", type = Long.class)
	public ConfigChannel<Long> maxSoc = new ConfigChannel<Long>("maxSoc", this);

	@ConfigInfo(title = "Production limit", description = "If SOC falls below this min-SOC and power production is below production-limit, thermalpowerstation will start.", type = Long.class)
	public ConfigChannel<Long> productionLimit = new ConfigChannel<>("productionLimit", this);

	@ConfigInfo(title = "Production limit period", description = "Indicates how long the production power must be below production-limit to start the powerstation. Time in minutes.", type = Long.class)
	public ConfigChannel<Long> limitTimeRange = new ConfigChannel<>("limitTimeRange", this);

	@ConfigInfo(title = "Invert Output", description = "True if the digital output should be inverted.", type = Boolean.class)
	public ConfigChannel<Boolean> invertOutput = new ConfigChannel<>("invertOutput", this);

	@SuppressWarnings("unchecked")
	@ConfigInfo(title = "the address of the Digital Output where the generator is connected to.", type = String.class)
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
	/*
	 * Methods
	 */

	@Override
	public void run() {
		try {
			if (getProductionPower() >= productionLimit.value()) {
				lastTimeAboveProductionlimit = System.currentTimeMillis();
			}
			switch (currentState) {
			case OFF:
				if (isOff()) {
					if (ess.value().soc.value() <= minSoc.value()
							&& lastTimeAboveProductionlimit + limitTimeRange.value() <= System.currentTimeMillis()) {
						currentState = State.SWITCHON;
					}
				} else {
					currentState = State.SWITCHOFF;
				}
				break;
			case ON:
				if (isOff()) {
					currentState = State.SWITCHON;
				} else {
					if (ess.value().soc.value() >= maxSoc.value()
							|| lastTimeAboveProductionlimit + limitTimeRange.value() > System.currentTimeMillis()) {
						currentState = State.SWITCHOFF;
					}
				}
				break;
			case SWITCHOFF:
				if (isOff()) {
					currentState = State.OFF;
					switchOffCount = 0;
				} else {
					stopGenerator();
					switchOffCount++;
					if (switchOffCount > 5) {
						log.error("tried " + switchOffCount + " times to switch " + outputChannelAddress.value()
								+ " off without success!");
					}
				}
				break;
			case SWITCHON:
				if (isOff()) {
					startGenerator();
					switchOnCount++;
					if (switchOnCount > 5) {
						log.error("tried " + switchOnCount + " times to switch " + outputChannelAddress.value()
								+ " on without success!");
					}
				} else {
					currentState = State.ON;
					switchOnCount = 0;
				}
				break;
			case UNDEFINED:
				if (isOff()) {
					currentState = State.OFF;
				} else {
					currentState = State.ON;
				}
			default:

				break;
			}
		} catch (InvalidValueException e1) {
			log.error("Failed to read value!", e1);
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

	private boolean isOff() throws InvalidValueException {
		return outputChannel.value() == false ^ invertOutput.value();
	}

	private Long getProductionPower() throws InvalidValueException {
		Long power = 0L;
		for (Meter m : meters.value()) {
			power += m.power.value();
		}
		return power;
	}

}
