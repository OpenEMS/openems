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
package io.openems.impl.device.keba;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonObject;

import io.openems.api.device.nature.evcs.EvcsNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.common.utils.JsonUtils;
import io.openems.impl.protocol.keba.KebaDevice;
import io.openems.impl.protocol.keba.KebaDeviceNature;
import io.openems.impl.protocol.keba.KebaReadChannel;

@ThingInfo(title = "KEBA KeContact EVCS")
public class KebaEvcs extends KebaDeviceNature implements EvcsNature {

	/*
	 * Constructors
	 */
	public KebaEvcs(String thingId, KebaDevice parent) throws ConfigException {
		super(thingId, parent);
	}

	/*
	 * Inherited Channels
	 */
	// private KebaReadChannel<Long> current = new KebaReadChannel<Long>("Current", this);

	// @Override
	// public ReadChannel<Long> current() {
	// return current;
	// }

	/*
	 * This Channels
	 */
	// general

	// report 1
	public KebaReadChannel<String> product = new KebaReadChannel<String>("Product", this); // Model name (variant)
	public KebaReadChannel<String> serial = new KebaReadChannel<String>("Serial", this); // Serial number
	public KebaReadChannel<String> firmware = new KebaReadChannel<String>("Firmware", this); // Firmware version
	public KebaReadChannel<String> comModule = new KebaReadChannel<String>("ComModule",
			this); /* Communication module is installed; KeContact P30 only */

	// report 2
	public KebaReadChannel<Integer> state = new KebaReadChannel<Integer>("Integer", this)
			/* Current state of the charging station */
			.label(0, "starting") //
			.label(1, "not ready for charging")
			/* e.g. unplugged, X1 or "ena" not enabled, RFID not enabled,... */
			.label(2, "ready for charging") /* waiting for EV charging request (S2) */
			.label(3, "charging") //
			.label(4, "error") //
			.label(5, "authorization rejected");
	public KebaReadChannel<Integer> error1 = new KebaReadChannel<Integer>("Error1", this);
	/* Detail code for state 4; exceptions see FAQ on www.kecontact.com */
	public KebaReadChannel<Integer> error2 = new KebaReadChannel<Integer>("Error2", this);
	/* Detail code for state 4 exception #6 see FAQ on www.kecontact.com */
	public KebaReadChannel<Integer> plug = new KebaReadChannel<Integer>("Plug", this).label(0, "unplugged")
			/* Current condition of the loading connection */
			.label(1, "plugged on charging station") //
			.label(3, "plugged on charging station + locked") //
			.label(5, "plugged on charging station + plugged on EV") //
			.label(7, "plugged on charging station + locked + plugged on EV");
	public KebaReadChannel<Boolean> enableSys = new KebaReadChannel<Boolean>("EnableSys",
			this); /* Enable state for charging (contains Enable input, RFID, UDP,..). */
	public KebaReadChannel<Boolean> enableUser = new KebaReadChannel<Boolean>("EnableUser",
			this); /* Enable condition via UDP. */
	public KebaReadChannel<Integer> maxCurr = new KebaReadChannel<Integer>("MaxCurr",
			this); /* Current preset value via Control pilot in milliampere. */
	public KebaReadChannel<Integer> maxCurrPercent = new KebaReadChannel<Integer>("MaxCurrPercent",
			this); /* Current preset value via Control pilot in 0,1% of the PWM value */
	public KebaReadChannel<Integer> currHardware = new KebaReadChannel<Integer>("CurrHardware",
			this); /*
			 * Highest possible charging current of the charging connection. Contains device maximum, DIP-switch
			 * setting, cable coding and temperature reduction.
			 */
	public KebaReadChannel<Integer> currUser = new KebaReadChannel<Integer>("CurrUser",
			this); /* Current preset value of the user via UDP; Default = 63000mA. */
	public KebaReadChannel<Integer> currFailsafe = new KebaReadChannel<Integer>("CurrFailsafe",
			this); /* Current preset value for the Failsafe function. */
	public KebaReadChannel<Integer> timeoutFailsafe = new KebaReadChannel<Integer>("TimeoutFailsafe",
			this); /* Communication timeout before triggering the Failsafe function. */
	public KebaReadChannel<Integer> currTimer = new KebaReadChannel<Integer>("CurrTimer",
			this); /* Shows the current preset value of currtime */
	public KebaReadChannel<Integer> timeoutCT = new KebaReadChannel<Integer>("TimeoutCT",
			this); /* Shows the remaining time until the current value is accepted. */
	public KebaReadChannel<Integer> energyLimit = new KebaReadChannel<Integer>("EnergyLimit",
			this); /* Shows the set energy limit. */
	public KebaReadChannel<Boolean> output = new KebaReadChannel<Boolean>("Output", this); /* State of the output X2 */
	public KebaReadChannel<Boolean> input = new KebaReadChannel<Boolean>("Input",
			this); /*
			 * State of the potential free Enable input X1.
			 * When using the input, please pay attention to the information
			 * in the installation manual.
			 */

	// report 3
	public KebaReadChannel<Integer> voltageL1 = new KebaReadChannel<Integer>("VoltageL1",
			this); /* Current voltage in V */
	public KebaReadChannel<Integer> voltageL2 = new KebaReadChannel<Integer>("VoltageL2",
			this); /* Current voltage in V */
	public KebaReadChannel<Integer> voltageL3 = new KebaReadChannel<Integer>("VoltageL3",
			this); /* Current voltage in V */
	public KebaReadChannel<Integer> currentL1 = new KebaReadChannel<Integer>("CurrentL1",
			this); /* Current current value of in mA. */
	public KebaReadChannel<Integer> currentL2 = new KebaReadChannel<Integer>("CurrentL2",
			this); /* Current current value of in mA. */
	public KebaReadChannel<Integer> currentL3 = new KebaReadChannel<Integer>("CurrentL3",
			this); /* Current current value of in mA. */
	public KebaReadChannel<Integer> actualPower = new KebaReadChannel<Integer>("ActualPower",
			this); /* Current power in mW (Real Power). */
	public KebaReadChannel<Integer> cosPhi = new KebaReadChannel<Integer>("CosPhi",
			this); /* Power factor in 0,1% (cosphi) */
	public KebaReadChannel<Integer> energySession = new KebaReadChannel<Integer>("EnergySession",
			this); /*
			 * Power consumption of the current loading session in 0,1Wh;
			 * Reset with new loading session (state = 2).
			 */
	public KebaReadChannel<Long> energyTotal = new KebaReadChannel<Long>("EnergyTotal",
			this); /*
			 * Total power consumption (persistent) without current loading
			 * session 0,1Wh; Is summed up after each completed charging
			 * session (state = 0).
			 */

	@Override
	protected List<String> getWriteMessages() {
		// TODO Auto-generated method stub
		return new ArrayList<String>();
	}

	@Override
	protected void receive(JsonObject jMessage) {
		Optional<String> idOpt = JsonUtils.getAsOptionalString(jMessage, "ID");
		if (idOpt.isPresent()) {
			// message with ID
			String id = idOpt.get();
			if (id.equals("1")) {
				/*
				 * Reply to report 1
				 */
				this.product.updateValue(JsonUtils.getAsOptionalString(jMessage, "Product").orElse(null));
				this.serial.updateValue(JsonUtils.getAsOptionalString(jMessage, "Serial").orElse(null));
				this.firmware.updateValue(JsonUtils.getAsOptionalString(jMessage, "Firmware").orElse(null));
				this.comModule.updateValue(JsonUtils.getAsOptionalString(jMessage, "COM-module").orElse(null));

			} else if (id.equals("2")) {
				/*
				 * Reply to report 2
				 */
				this.state.updateValue(JsonUtils.getAsOptionalInt(jMessage, "State").orElse(null));
				this.error1.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Error1").orElse(null));
				this.error2.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Error2").orElse(null));
				this.plug.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Plug").orElse(null));
				Optional<Integer> enableSysOpt = JsonUtils.getAsOptionalInt(jMessage, "Enable sys");
				if (enableSysOpt.isPresent()) {
					this.enableSys.updateValue(enableSysOpt.get() == 1);
				} else {
					this.enableSys.updateValue(null);
				}
				Optional<Integer> enableUserOpt = JsonUtils.getAsOptionalInt(jMessage, "Enable user");
				if (enableUserOpt.isPresent()) {
					this.enableUser.updateValue(enableUserOpt.get() == 1);
				} else {
					this.enableUser.updateValue(null);
				}
				this.maxCurr.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Max curr").orElse(null));
				this.maxCurrPercent.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Max curr %").orElse(null));
				this.currHardware.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Curr HW").orElse(null));
				this.currUser.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Curr user").orElse(null));
				this.currFailsafe.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Curr FS").orElse(null));
				this.timeoutFailsafe.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Tmo FS").orElse(null));
				this.currTimer.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Curr timer").orElse(null));
				this.timeoutCT.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Tmo CT").orElse(null));
				this.energyLimit.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Setenergy").orElse(null));
				Optional<Integer> outputOpt = JsonUtils.getAsOptionalInt(jMessage, "Output");
				if (outputOpt.isPresent()) {
					this.output.updateValue(outputOpt.get() == 1);
				} else {
					this.output.updateValue(null);
				}
				Optional<Integer> inputOpt = JsonUtils.getAsOptionalInt(jMessage, "Input");
				if (inputOpt.isPresent()) {
					this.input.updateValue(inputOpt.get() == 1);
				} else {
					this.input.updateValue(null);
				}

			} else if (id.equals("3")) {
				/*
				 * Reply to report 3
				 */
				this.voltageL1.updateValue(JsonUtils.getAsOptionalInt(jMessage, "U1").orElse(null));
				this.voltageL2.updateValue(JsonUtils.getAsOptionalInt(jMessage, "U2").orElse(null));
				this.voltageL3.updateValue(JsonUtils.getAsOptionalInt(jMessage, "U3").orElse(null));
				this.currentL1.updateValue(JsonUtils.getAsOptionalInt(jMessage, "I1").orElse(null));
				this.currentL2.updateValue(JsonUtils.getAsOptionalInt(jMessage, "I2").orElse(null));
				this.currentL3.updateValue(JsonUtils.getAsOptionalInt(jMessage, "I3").orElse(null));
				this.actualPower.updateValue(JsonUtils.getAsOptionalInt(jMessage, "P").orElse(null));
				this.cosPhi.updateValue(JsonUtils.getAsOptionalInt(jMessage, "PF").orElse(null));
				this.energySession.updateValue(JsonUtils.getAsOptionalInt(jMessage, "E pres").orElse(null));
				this.energyTotal.updateValue(JsonUtils.getAsOptionalLong(jMessage, "E total").orElse(null));

			}
		} else {
			// message without ID -> UDP broadcast
			if (jMessage.has("State")) {
				this.state.updateValue(JsonUtils.getAsOptionalInt(jMessage, "State").orElse(null));
			}
			if (jMessage.has("Plug")) {
				this.plug.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Plug").orElse(null));
			}
			if (jMessage.has("Input")) {
				Optional<Integer> inputOpt = JsonUtils.getAsOptionalInt(jMessage, "Input");
				if (inputOpt.isPresent()) {
					this.input.updateValue(inputOpt.get() == 1);
				} else {
					this.input.updateValue(null);
				}
			}
			if (jMessage.has("Enable sys")) {
				Optional<Integer> enableSysOpt = JsonUtils.getAsOptionalInt(jMessage, "Enable sys");
				if (enableSysOpt.isPresent()) {
					this.enableSys.updateValue(enableSysOpt.get() == 1);
				} else {
					this.enableSys.updateValue(null);
				}
			}
			if (jMessage.has("Max curr")) {
				this.maxCurr.updateValue(JsonUtils.getAsOptionalInt(jMessage, "Max curr").orElse(null));
			}
			if (jMessage.has("E pres")) {
				this.energySession.updateValue(JsonUtils.getAsOptionalInt(jMessage, "E pres").orElse(null));
			}
		}
	}

	@Override
	public String toString() {
		return "KebaEvcs [state=" + state.format() + ", plug=" + plug.format() + ", actualPower=" + actualPower.format()
		+ ", energySession=" + energySession.format() + ", energyTotal=" + energyTotal.format() + "]";
	}
}
