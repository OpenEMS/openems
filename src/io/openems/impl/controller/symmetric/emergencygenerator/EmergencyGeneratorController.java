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
package io.openems.impl.controller.symmetric.emergencygenerator;

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
	@ConfigInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ConfigInfo(title = "Grid-meter", description = "Sets the grid-meter to detect if the system is Off-Grid or On-Grid.", type = Meter.class)
	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ConfigInfo(title = "Min-SOC", description = "If the SOC falls under this value and the system is Off-Grid the generator starts.", type = Long.class)
	public ConfigChannel<Long> minSoc = new ConfigChannel<Long>("minSoc", this);

	@ConfigInfo(title = "Max-SOC", description = "If the system is Off-Grid and the generator is running, the generator stops if the SOC level increases over the Max-SOC.", type = Long.class)
	public ConfigChannel<Long> maxSoc = new ConfigChannel<Long>("maxSoc", this);

	@ConfigInfo(title = "Invert-Output", description = "True if the digital output should be inverted.", type = Boolean.class)
	public ConfigChannel<Boolean> invertOutput = new ConfigChannel<>("invertOutput", this);

	@ConfigInfo(title = "On-Grid output on", description = "This value indicates if the system is On-Grid to start(true) or stop(false) the generator.", type = Boolean.class, isOptional = true)
	public ConfigChannel<Boolean> onGridOutputOn = new ConfigChannel<Boolean>("onGridOutputOn", this)
			.defaultValue(false);

	@ConfigInfo(title = "time to wait before switch output on.", type = Long.class)
	public ConfigChannel<Long> switchDelay = new ConfigChannel<Long>("switchDelay", this).defaultValue(10000L);

	/*
	 * Fields
	 */
	private ThingRepository repo = ThingRepository.getInstance();
	private WriteChannel<Boolean> outputChannel;
	private long timeOnGrid = 0L;
	private long timeOffGrid = 0L;
	private long startTime = System.currentTimeMillis();
	private State currentState = State.UNKNOWN;
	private OffGridState currentOffGridState;

	private enum State {
		GOONGRID1, GOONGRID2, ONGRID, GOOFFGRID, OFFGRID, UNKNOWN
	}

	private enum OffGridState {
		STOPGENERATOR, STARTGENERATOR, STOP, RUNNING
	}

	/*
	 * Methods
	 */
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

	@Override
	public void run() {
		if (startTime + 1000 * 15 <= System.currentTimeMillis()) {
			try {
				switch (currentState) {
				case GOOFFGRID:
					if (isOff()) {
						if (timeOffGrid + switchDelay.value() <= System.currentTimeMillis()) {
							currentState = State.OFFGRID;
						}
					} else {
						stopGenerator();
						timeOffGrid = System.currentTimeMillis();
					}
					break;
				case GOONGRID1:
					if (isOff()) {
						if (timeOnGrid + switchDelay.value() <= System.currentTimeMillis()) {
							currentState = State.GOONGRID2;
						}
					} else {
						stopGenerator();
						timeOnGrid = System.currentTimeMillis();
					}
					break;
				case GOONGRID2:
					// check if outputstate is correct according to onGridOutputOn => !(1^1) = 1, !(1^0) = 0,
					// !(0^1) = 0, !(0^0) = 1
					if (!(onGridOutputOn.value() ^ isOn())) {
						currentState = State.ONGRID;
					} else {
						if (onGridOutputOn.value()) {
							startGenerator();
						} else {
							stopGenerator();
						}
					}
					break;
				case ONGRID:
					if (meter.value().voltage.valueOptional().isPresent()) {
						if (onGridOutputOn.value() ^ isOn()) {
							currentState = State.GOONGRID2;
						}
					} else {
						currentState = State.GOOFFGRID;
					}
					break;
				case OFFGRID:
					if (meter.value().voltage.valueOptional().isPresent()) {
						currentState = State.GOONGRID1;
					} else {
						switch (currentOffGridState) {
						case RUNNING:
							if (!ess.value().soc.valueOptional().isPresent()
									|| ess.value().soc.value() >= maxSoc.value()) {
								currentOffGridState = OffGridState.STOPGENERATOR;
							}
							break;
						case STARTGENERATOR:
							if (isOn()) {
								currentOffGridState = OffGridState.RUNNING;
							} else {
								startGenerator();
							}
							break;
						case STOP:
							if (ess.value().soc.valueOptional().isPresent()
									&& ess.value().soc.value() <= minSoc.value()) {
								currentOffGridState = OffGridState.STARTGENERATOR;
							}
							break;
						case STOPGENERATOR:
							if (isOff()) {
								currentOffGridState = OffGridState.STOP;
							} else {
								stopGenerator();
							}
							break;
						}
					}
				case UNKNOWN:
				default:
					if (meter.value().voltage.valueOptional().isPresent()) {
						currentState = State.GOONGRID2;
					} else {
						currentState = State.GOOFFGRID;
					}
					break;

				}
				// // Check if grid is available
				// if (!meter.value().voltage.valueOptional().isPresent()) {
				// // no meassurable voltage => Off-Grid
				// if (!generatorOn && ess.value().soc.valueOptional().isPresent()
				// && ess.value().soc.value() <= minSoc.value()) {
				// // switch generator on
				// startGenerator();
				// generatorOn = true;
				// } else if (generatorOn && (!ess.value().soc.valueOptional().isPresent()
				// || ess.value().soc.value() >= maxSoc.value())) {
				// // switch generator off
				// stopGenerator();
				// generatorOn = false;
				// } else if (generatorOn) {
				// startGenerator();
				// } else if (!generatorOn) {
				// stopGenerator();
				// }
				// switchedToOnGrid = false;
				// } else {
				// // Grid voltage is in the allowed range
				// if (switchedToOnGrid) {
				// if (timeOnGrid + switchDelay.value() <= System.currentTimeMillis()) {
				// if (onGridOutputOn.value()) {
				// startGenerator();
				// } else {
				// stopGenerator();
				// }
				// }
				// } else {
				// stopGenerator();
				// timeOnGrid = System.currentTimeMillis();
				// switchedToOnGrid = true;
				// }
				// }
			} catch (InvalidValueException e) {
				log.error("Failed to read value!", e);
			} catch (WriteChannelException e) {
				log.error("Error due write to output [" + outputChannelAddress.valueOptional().orElse("<none>") + "]",
						e);
			}
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

	private boolean isOn() throws InvalidValueException {
		return outputChannel.value() == true ^ invertOutput.value();
	}

}
