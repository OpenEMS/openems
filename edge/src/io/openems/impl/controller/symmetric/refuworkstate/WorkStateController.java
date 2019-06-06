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
package io.openems.impl.controller.symmetric.refuworkstate;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.DebugChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

/**
 * @author matthias.rossmann
 */
@ThingInfo(title = "REFU Workstate (Symmetric)", description = "Sends the Ess to Standby if no power is required. Do not use if Off-Grid functionality is required. For symmetric Ess.")
public class WorkStateController extends Controller {

	private final Logger log = LoggerFactory.getLogger(WorkStateController.class);

	private ThingStateChannels thingState = new ThingStateChannels(this);
	/*
	 * Constructors
	 */
	public WorkStateController() {
		super();
	}

	public WorkStateController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess device.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<>("ess", this);

	@ChannelInfo(title = "Start/Stop", description = "Indicates if the Ess should be started (true) or stopped (false).", type = Boolean.class)
	public final ConfigChannel<Boolean> start = new ConfigChannel<>("start", this);

	/*
	 * DebugChannel
	 */
	private DebugChannel<Long> systemstate = new DebugChannel<>("SystemState", this);

	/*
	 * Fields
	 */
	private long lastReset = 0L;
	private Long errorOccured = 0L;
	private State currentState = State.GOSTART;

	private enum State {
		START, STOP, GOSTART, ERROR, RESETERRORON, RESETERROROFF
	}

	/*
	 * Methods
	 */

	/*
	 * public static ThingDoc getDescription() {
	 * return new ThingDoc("WorkStateController",
	 * "Handles if the storage system should go to standby or stay in running mode. This is indicated by the configchannel 'start'. Has an error occoured tries this controller to reset the error three times. If the tries to reset the error failed the controller sleep for 30 minutes till it tries another three times to reset the error. This is repeated thill the error disapears."
	 * );
	 * }
	 */
	@Override
	public void run() {
		Ess ess;
		try {
			ess = this.ess.value();
			// if (start.value()) {
			// if () {
			// ess.setWorkState.pushWriteFromLabel(EssNature.START);
			// log.info("start Refu");
			// lastStart = System.currentTimeMillis();
			// isError = false;
			// } else if (!ess.systemState.labelOptional().equals(Optional.of(EssNature.START))) {
			// ess.setWorkState.pushWriteFromLabel(EssNature.STOP);
			// if (ess.systemState.labelOptional().equals(Optional.of("Error")) && !isError) {
			// timeErrorOccured = System.currentTimeMillis();
			// isError = true;
			// }
			// }
			// if (ess.systemState.labelOptional().equals(Optional.of("Error"))
			// && lastReset <= System.currentTimeMillis() - 1000 * 60 * 5 && resetCount < 2
			// && timeErrorOccured <= System.currentTimeMillis() - 1000 * 3) {
			// if (!reset) {
			// ess.setSystemErrorReset.pushWriteFromLabel(EssNature.ON);
			// reset = true;
			// log.info("Reset Refu error");
			// lastReset = System.currentTimeMillis();
			// resetCount++;
			// }
			// }
			// if (reset) {
			// ess.setSystemErrorReset.pushWriteFromLabel(EssNature.OFF);
			// reset = false;
			// }
			// if (lastReset <= System.currentTimeMillis() - 2 * 60 * 60 * 1000
			// || (lastStart <= System.currentTimeMillis() - 1000 * 60 && timeErrorOccured < lastStart)) {
			// resetCount = 0;
			// }
			// } else {
			// ess.setWorkState.pushWriteFromLabel(EssNature.STOP);
			// log.info("stop Refu");
			// }
			switch (currentState) {
			case ERROR:
				log.info("Error");
				systemstate.setValue(1L);
				if (errorOccured == null) {
					errorOccured = System.currentTimeMillis();
				}
				ess.setWorkState.pushWriteFromLabel(EssNature.STOP);
				if (errorOccured != null && errorOccured <= System.currentTimeMillis() - 30 * 1000 // errorhandling since 30 seconds
						&& lastReset <= System.currentTimeMillis() - 2 * 60 * 60 * 1000) { // last reset more than 2 hours
					currentState = State.RESETERRORON;
					errorOccured = null;
				}
				break;
			case GOSTART:
				log.info("Go Start");
				if (ess.systemState.labelOptional().equals(Optional.of(EssNature.STANDBY))) {
					ess.setWorkState.pushWriteFromLabel(EssNature.START);
				} else if (ess.systemState.labelOptional().equals(Optional.of(EssNature.START))) {
					currentState = State.START;
				} else if (ess.systemState.labelOptional().equals(Optional.of(EssNature.FAULT))) {
					currentState = State.ERROR;
				} else {
					currentState = State.STOP;
				}
				break;
			case RESETERROROFF:
				log.info("RESETERROROFF");
				systemstate.setValue(3L);
				if (ess.setSystemErrorReset.labelOptional().equals(Optional.of(EssNature.OFF))) {
					currentState = State.GOSTART;
					lastReset = System.currentTimeMillis();
				} else {
					ess.setSystemErrorReset.pushWriteFromLabel(EssNature.OFF);
				}
				break;
			case RESETERRORON:
				log.info("RESETERRORON");
				systemstate.setValue(4L);
				if (ess.setSystemErrorReset.labelOptional().equals(Optional.of(EssNature.ON))) {
					currentState = State.RESETERROROFF;
				} else {
					ess.setSystemErrorReset.pushWriteFromLabel(EssNature.ON);
				}
				break;
			case START:
				log.info("START");
				systemstate.setValue(5L);
				if (ess.systemState.labelOptional().equals(Optional.of(EssNature.START))) {
					if (!start.value()) {
						currentState = State.STOP;
					}
				} else if (ess.systemState.labelOptional().equals(Optional.of(EssNature.FAULT))) {
					currentState = State.ERROR;
				} else {
					currentState = State.GOSTART;
				}
				break;
			case STOP:
				log.info("STOP");
				systemstate.setValue(6L);
				if (ess.systemState.labelOptional().equals(Optional.of(EssNature.STOP))
						|| ess.systemState.labelOptional().equals(Optional.of(EssNature.STANDBY))
						|| ess.systemState.labelOptional().equals(Optional.of("Init"))
						|| ess.systemState.labelOptional().equals(Optional.of("Pre-operation"))) {
					if (start.value()) {
						currentState = State.GOSTART;
					}
				} else if (ess.systemState.labelOptional().equals(Optional.of(EssNature.FAULT))) {
					currentState = State.ERROR;
				} else {
					ess.setWorkState.pushWriteFromLabel(EssNature.STOP);
				}
				break;
			default:
				break;

			}
		} catch (InvalidValueException | WriteChannelException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

}
