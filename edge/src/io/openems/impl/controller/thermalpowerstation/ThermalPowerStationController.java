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
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
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
	public Optional<WriteChannel<Boolean>> outputChannelOpt = Optional.empty();
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
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Meters", description = "Meters of power producers (e.g. PV).", type = Meter.class, isArray = true)
	public ConfigChannel<List<Meter>> meters = new ConfigChannel<>("meters", this);

	@ChannelInfo(title = "Min-SOC", description = "If SOC falls below this min-SOC and power production is below production-limit, thermalpowerstation will start.", type = Long.class)
	public ConfigChannel<Long> minSoc = new ConfigChannel<Long>("minSoc", this);

	@ChannelInfo(title = "Max-SOC", description = "If the SOC rises above max-SOC the thermalpowerstation will stop.", type = Long.class)
	public ConfigChannel<Long> maxSoc = new ConfigChannel<Long>("maxSoc", this);

	@ChannelInfo(title = "Production limit", description = "If SOC falls below this min-SOC and power production is below production-limit, thermalpowerstation will start.", type = Long.class)
	public ConfigChannel<Long> productionLimit = new ConfigChannel<>("productionLimit", this);

	@ChannelInfo(title = "Production limit period", description = "Indicates how long the production power must be below production-limit to start the powerstation. Time in minutes.", type = Long.class)
	public ConfigChannel<Long> limitTimeRange = new ConfigChannel<>("limitTimeRange", this);

	@ChannelInfo(title = "Invert Output", description = "True if the digital output should be inverted.", type = Boolean.class)
	public ConfigChannel<Boolean> invertOutput = new ConfigChannel<>("invertOutput", this);

	@SuppressWarnings("unchecked")
	@ChannelInfo(title = "the address of the Digital Output where the generator is connected to.", type = String.class)
	public ConfigChannel<String> outputChannelAddress = new ConfigChannel<String>("outputChannelAddress", this)
	.addChangeListener((channel, newValue, oldValue) -> {
		Optional<String> channelAddress = (Optional<String>) newValue;
		if (channelAddress.isPresent()) {
			Optional<Channel> channelOpt = repo.getChannelByAddress(channelAddress.get());
			if (channelOpt.isPresent()) {
				this.outputChannelOpt = Optional.of( //
						((WriteChannel<Boolean>) channelOpt.get()).required());
				// TODO should not be necessary to set outputChannel as required
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
		// Get all required values - or abort with error
		long productionPower;
		long productionLimit;
		boolean isOff;
		long soc;
		long minSoc;
		long maxSoc;
		long limitTimeRange;
		boolean invertOutput;
		long allowedCharge;
		long allowedDischarge;
		try {
			isOff = this.isOff();
			productionPower = this.getProductionPower();
			productionLimit = this.productionLimit.value();
			soc = this.ess.value().soc.value();
			minSoc = this.minSoc.value();
			maxSoc = this.maxSoc.value();
			allowedCharge = this.ess.value().allowedCharge.value();
			allowedDischarge = this.ess.value().allowedDischarge.value();
			limitTimeRange = this.limitTimeRange.value();
			invertOutput = this.invertOutput.value();
		} catch (InvalidValueException | ConfigException e) {
			log.error(e.getMessage());
			return;
		}

		try {
			if (productionPower >= productionLimit) {
				lastTimeAboveProductionlimit = System.currentTimeMillis();
			}
			switch (currentState) {
			case OFF:
				if (isOff) {
					if ((soc <= minSoc || allowedDischarge == 0) && lastTimeAboveProductionlimit + limitTimeRange <= System.currentTimeMillis()) {
						currentState = State.SWITCHON;
					}
				} else {
					currentState = State.SWITCHOFF;
				}
				break;
			case ON:
				if (isOff) {
					currentState = State.SWITCHON;
				} else {
					if (soc >= maxSoc || allowedCharge == 0 || lastTimeAboveProductionlimit + limitTimeRange > System.currentTimeMillis()) {
						currentState = State.SWITCHOFF;
					}
				}
				break;
			case SWITCHOFF:
				if (isOff) {
					currentState = State.OFF;
					switchOffCount = 0;
				} else {
					stopGenerator(invertOutput);
					switchOffCount++;
					if (switchOffCount > 5) {
						log.error("tried " + switchOffCount + " times to switch " + outputChannelAddress
								+ " off without success!");
					}
				}
				break;
			case SWITCHON:
				if (isOff) {
					startGenerator(invertOutput);
					switchOnCount++;
					if (switchOnCount > 5) {
						log.error("tried " + switchOnCount + " times to switch " + outputChannelAddress
								+ " on without success!");
					}
				} else {
					currentState = State.ON;
					switchOnCount = 0;
				}
				break;
			case UNDEFINED:
				if (isOff) {
					currentState = State.OFF;
				} else {
					currentState = State.ON;
				}
			default:

				break;
			}

		} catch (WriteChannelException | ConfigException e) {
			log.error("Error writing [" + outputChannelAddress + "]: " + e.getMessage());
		}
	}

	private WriteChannel<Boolean> getOutputChannel() throws ConfigException {
		if (this.outputChannelOpt.isPresent()) {
			return this.outputChannelOpt.get();
		} else {
			throw new ConfigException("outputChannel is not available.");
		}
	}

	private void startGenerator(boolean invertOutput) throws WriteChannelException, ConfigException {
		WriteChannel<Boolean> outputChannel = getOutputChannel();
		Optional<Boolean> output = outputChannel.valueOptional();
		if (!output.isPresent() || output.get() != true ^ invertOutput) {
			outputChannel.pushWrite(true ^ invertOutput);
		}
	}

	private void stopGenerator(boolean invertOutput) throws WriteChannelException, ConfigException {
		WriteChannel<Boolean> outputChannel = getOutputChannel();
		Optional<Boolean> output = outputChannel.valueOptional();
		if (!output.isPresent() || output.get() != false ^ invertOutput) {
			outputChannel.pushWrite(false ^ invertOutput);
		}
	}

	private boolean isOff() throws InvalidValueException, ConfigException {
		WriteChannel<Boolean> outputChannel = getOutputChannel();
		return outputChannel.value() == false ^ invertOutput.value();
	}

	private long getProductionPower() throws InvalidValueException {
		Long power = 0L;
		for (Meter m : meters.value()) {
			power += m.power.value();
		}
		return power;
	}

}
