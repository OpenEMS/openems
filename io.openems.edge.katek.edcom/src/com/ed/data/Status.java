// CHECKSTYLE:OFF
/*
*   EDCOM 8.1 is a java cross platform library for communication with 10kW
*   hybrid Inverter (Katek Memmingen GmbH).
*   Copyright (C) 2022 Katek Memmingen GmbH
*
*   This program is free software: you can redistribute it and/or modify
*   it under the terms of the GNU Lesser General Public License as published by
*   the Free Software Foundation, either version 3 of the License, or
*   (at your option) any later version.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*   
*   You should have received a copy of the GNU Lesser General Public License
*   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.ed.data;

import java.util.List;

import com.ed.edcom.Client;
import com.ed.edcom.DspVar;
import com.ed.edcom.Util;

/**
 * Inverter status
 */
public final class Status implements DataSet {

	/**
	 * Errors 0, basic data
	 */
	public final DspVar errors0;
	/**
	 * Errors 1, basic data
	 */
	public final DspVar errors1;
	/**
	 * Warnings, basic data
	 */
	public final DspVar warn;
	/**
	 * Inverter status, basic data
	 */
	public final DspVar externStatus;
	/**
	 * Main grid status, basic data
	 */
	public final DspVar gridStatus;
	/**
	 * Main grid communication status, basic data
	 */
	public final DspVar gridComStatus;
	/**
	 * BMS status, basic data
	 */
	public final DspVar bmsStatus;
	/**
	 * Inverter mode, basic data
	 */
	public final DspVar inverterMode;

	/**
	 * Main grid connection mode
	 */
	public final DspVar gridConMode;

	/**
	 * Creates a object representing inverter status
	 *
	 * @throws java.lang.Exception wrong parameters
	 */
	public Status() throws Exception {
		errors0 = new DspVar("fm.error_bits[0]", DspVar.TYPE_UINT32, 0, null, 0);
		errors1 = new DspVar("fm.error_bits[1]", DspVar.TYPE_UINT32, 0, null, 0);
		warn = new DspVar("warn_bf", DspVar.TYPE_UINT32, 0, null, 0);
		externStatus = new DspVar("prime_sm.ext_status", DspVar.TYPE_UINT16, 0, null, 0);
		gridStatus = new DspVar("rs.tb_status", DspVar.TYPE_UINT16, 0, null, 0);
		gridComStatus = new DspVar("rs.dist_board_on", DspVar.TYPE_UINT8, 0, null, 0);
		bmsStatus = new DspVar("bms.Status_BMS.Allbits", DspVar.TYPE_UINT16, 0, null, 0);
		inverterMode = new DspVar("prime_sm.inverter_mode", DspVar.TYPE_UINT32, 0, null, 0);
		gridConMode = new DspVar("rs.act_switch_pos", DspVar.TYPE_UINT8, 0, null, 0);
	}

	/**
	 * Get current inverter status
	 *
	 * @return true - some error occurred
	 */
	public boolean isError() {
		return ((errors0.getLong() != 0) || (errors1.getLong() != 0));
	}

	/**
	 * Get current inverter errors
	 *
	 * @return List of recent error and warning codes
	 */
	public List<String> getErrors() {
		ErrorRecord record = new ErrorRecord(errors0.getLong(), errors1.getLong(), warn.getLong());
		return record.getErrorCodes();
	}

	/**
	 * Get current inverter warnings
	 *
	 * @return List of current warning Codes
	 */
	public List<String> getWarnings() {
		ErrorRecord record = new ErrorRecord(0, 0, warn.getLong());
		return record.getErrorCodes();
	}

	/**
	 * Get Inverter status
	 *
	 * @return status : 0 - Error, 1 - Off/Standby, 2..11,17 - Test, 12,16 -
	 *         Off-Grid / Island mode, 13..14 - Grid mode 15 - Inverter Off
	 */
	public int getInverterStatus() {
		return (int) (externStatus.getLong() & 0xFF);
	}

	/**
	 * Get Battery status
	 *
	 * @return battery status : 0 - Error, 1 - Off/Standby, 2..16 - Tests, 17 - On /
	 *         Active, 18 - Power down, 19 - Software Update.
	 */
	public int getBatteryStatus() {
		return (int) ((externStatus.getLong() >> 8) & 0xFF);
	}

	/**
	 * Get Main grid status
	 *
	 * @deprecated
	 * @return status : -1 - Unknown (Main grid not connected), 0 - On-Grid mode, 1
	 *         - Off-Grid mode.
	 */
	@Deprecated
	public int getVectisStatus() {
		int r = -1, vs = gridStatus.getInteger();
		if (gridComStatus.getInteger() != 1) {
			return r; // rs485 communication off
		}
		switch ((vs & 0x8) >> 3) {
		case 0: // grid
			r = 0;
			break;
		case 1: // off-grid
			r = 1;
			break;
		}
		return r;
	}

	/**
	 * Get Power main grid connection mode
	 *
	 * @return status : 0 - Unknown (Grid measurement not connected) / Switching, 1
	 *         - On-Grid mode, 2 - Off-Grid mode.
	 */
	public int getPowerGridStatus() {
		if (gridComStatus.getInteger() != 1) {
			return 0; // rs485 communication off
		}
		if ((int) ((inverterMode.getLong() >> 22) & 0x1) != 0) { // Emergency mode ?
			return gridConMode.getInteger();
		}
		switch (getInverterStatus()) { // return inverter status when not in Emergency mode
		case 12:
		case 16:
			return 2;
		case 13:
		case 14:
			return 1;
		}
		// return inverter status when not in Emergency mode
		return 0;

	}

	/**
	 * Get battery settings
	 *
	 * @return true - use battery, false - battery is not allowed.
	 */
	public boolean getBatteryConfig() {
		return ((inverterMode.getLong() & 0x1) != 0);
	}

	/**
	 * Get Off-grid (Island mode) configuration
	 *
	 * @return true - Off-Grid mode is enabled, false - disabled.
	 */
	private boolean offGridConfig() {
		return ((inverterMode.getLong() & 0x2) != 0);
	}

	/**
	 * Get Main grid Configuration
	 *
	 * @deprecated
	 * @return 0 - Main grid disabled, 1 - enabled with Internal current sensors, 2
	 *         - enabled with External current sensors, 3 - enabled with Internal
	 *         and External current sensors.
	 */
	@Deprecated
	public int getVectisConfig() {
		return getPowerGridConfig();
	}

	/**
	 * Get Power main grid measurement configuration
	 *
	 * @return 0 - Grid Connector disabled, 1 - enabled with Internal current
	 *         sensors, 2 - enabled with External current sensors, 3 - enabled with
	 *         Internal and External current sensors.
	 */
	public int getPowerGridConfig() {
		return (int) ((inverterMode.getLong() >> 2) & 0xF);
	}

	/**
	 * Get Power Management Configuration
	 *
	 * @return 0 - External Energy Manager, 1 - Battery charging, 2 - Self
	 *         consumption, 3 - Max. Yield.
	 */
	public int getPowerConfig() {
		return (int) ((inverterMode.getLong() >> 8) & 0xF);
	}

	@Override
	public void registerData(Client cl) {
		cl.addDspVar(errors0);
		cl.addDspVar(errors1);
		cl.addDspVar(warn);
		cl.addDspVar(externStatus);
		cl.addDspVar(gridComStatus);
		cl.addDspVar(bmsStatus);
		cl.addDspVar(inverterMode);
		if (Util.communication_ver8x) {
			cl.addDspVar(gridConMode);
		} else {
			cl.addDspVar(gridStatus);
		}
		refresh();
	}

	@Override
	public void refresh() {
		errors0.refresh();
		errors1.refresh();
		warn.refresh();
		externStatus.refresh();
		gridComStatus.refresh();
		bmsStatus.refresh();
		inverterMode.refresh();
		if (Util.communication_ver8x) {
			gridConMode.refresh();
		} else {
			gridStatus.refresh();
		}
	}

	@Override
	public boolean dataReady() {
		if (Util.communication_ver8x) {
			return ((errors0.refreshTime() > 0) //
					&& (errors1.refreshTime() > 0) //
					&& (warn.refreshTime() > 0) //
					&& (externStatus.refreshTime() > 0) //
					&& (gridComStatus.refreshTime() > 0) //
					&& (bmsStatus.refreshTime() > 0) //
					&& (inverterMode.refreshTime() > 0) //
					&& (gridConMode.refreshTime() > 0));
		} else {
			return ((errors0.refreshTime() > 0) //
					&& (errors1.refreshTime() > 0) //
					&& (warn.refreshTime() > 0) //
					&& (externStatus.refreshTime() > 0) //
					&& (gridStatus.refreshTime() > 0) //
					&& (gridComStatus.refreshTime() > 0) //
					&& (bmsStatus.refreshTime() > 0) //
					&& (inverterMode.refreshTime() > 0));
		}
	}

	@Override
	public String toString() {
		String rs = String.format(
				"STATUS:\nInverter \t %d \t (Off-Grid : %b) \nBattery \t %d \t (Enable : %b)\nPower Grid Status \t\t %d \t (Sensors config: %d)\nEnergy mode \t %d \n",
				getInverterStatus(), offGridConfig(), getBatteryStatus(), getBatteryConfig(), getPowerGridStatus(),
				getPowerGridConfig(), getPowerConfig());
		rs += getErrors().toString() + "\n";
		return rs;
	}
}
//CHECKSTYLE:ON
