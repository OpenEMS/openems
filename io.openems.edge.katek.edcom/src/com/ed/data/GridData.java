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

import com.ed.edcom.Client;
import com.ed.edcom.DspFloat;

/**
 * Power main grid measurements
 */
public final class GridData implements DataSet {

	/**
	 * main grid connector Power (internal sensors), basic data
	 */
	public final DspFloat pInt;
	/**
	 * main grid connector Power (external sensors), basic data
	 */
	public final DspFloat pExt;
	/**
	 * main grid connector AC Voltage, basic data
	 */
	public final DspFloat uExt;
	/**
	 * main grid connector Grid frequency, basic data
	 */
	public final DspFloat fExt;
	/**
	 * main grid connector reactive power (internal sensors), basic data
	 */
	public final DspFloat qInt;
	/**
	 * main grid connector reactive power (external sensors), basic data
	 */
	public final DspFloat qExt;

	/**
	 * Creates a object representing main grid connector measurements
	 *
	 * @throws java.lang.Exception wrong parameters
	 */
	public GridData() throws Exception {
		pInt = new DspFloat("rs.p_int", 3, null, 0);
		pExt = new DspFloat("rs.p_ext", 3, null, 0);
		uExt = new DspFloat("rs.u_ext", 3, null, 0);
		fExt = new DspFloat("rs.f_ext", 3, null, 0);
		qInt = new DspFloat("rs.q_int", 3, null, 0);
		qExt = new DspFloat("rs.q_ext", 3, null, 0);
	}

	/**
	 * Get main grid connector AC Power
	 *
	 * @param phaseIx Phase index 0 (Phase 1), 1 and 2.
	 * @return AC power [W] (~ 20ms, grid synchronized measurement) '+' consumption,
	 *         '-' feed in
	 */
	public float getACPower(int phaseIx) throws RuntimeException {
		return pInt.getFloat(phaseIx);
	}

	/**
	 * Get main grid connector AC Power, external current sensors
	 *
	 * @param phaseIx Phase index 0 (Phase 1), 1 and 2.
	 * @return AC power [W] (~ 20ms, grid synchronized measurement) '+' consumption,
	 *         '-' feed in
	 */
	public float getACPowerExt(int phaseIx) throws RuntimeException {
		return pExt.getFloat(phaseIx);
	}

	/**
	 * Get main grid connector AC Reactive Power
	 *
	 * @param phaseIx Phase index 0 (Phase 1), 1 and 2.
	 * @return Reactive Power [var] (~ 20ms, grid synchronized measurement) '+'
	 *         inductive, '-' capacitive
	 */
	public float getReactivePower(int phaseIx) throws RuntimeException {
		return qInt.getFloat(phaseIx);
	}

	/**
	 * Get main grid connector AC Reactive Power, external current sensors
	 *
	 * @param phaseIx Phase index 0 (Phase 1), 1 and 2.
	 * @return Reactive Power [var] (~ 20ms, grid synchronized measurement) '+'
	 *         inductive, '-' capacitive
	 */
	public float getReactivePowerExt(int phaseIx) throws RuntimeException {
		return qExt.getFloat(phaseIx);
	}

	/**
	 * Get main grid connector On-grid Voltage (external)
	 *
	 * @param phaseIx Phase index 0 (Phase 1), 1 and 2.
	 * @return AC voltage [V] (~ 20ms RMS, grid synchronized measurement)
	 */
	public float getACVoltageOnGrid(int phaseIx) throws RuntimeException {
		return uExt.getFloat(phaseIx);
	}

	/**
	 * Get main grid connector On-grid Frequency (external)
	 *
	 * @return AC Frequency [Hz] (grid synchronized measurement)
	 */
	public float getFrequencyExt() throws RuntimeException {
		return fExt.getFloat(0);
	}

	@Override
	public void registerData(Client cl) {
		cl.addDspVar(pInt);
		cl.addDspVar(pExt);
		cl.addDspVar(uExt);
		cl.addDspVar(fExt);
		cl.addDspVar(qInt);
		cl.addDspVar(qExt);
		refresh();
	}

	@Override
	public void refresh() {
		pInt.refresh();
		pExt.refresh();
		uExt.refresh();
		fExt.refresh();
		qInt.refresh();
		qExt.refresh();
	}

	@Override
	public boolean dataReady() {
		return ((pInt.refreshTime() > 0) //
				&& (pExt.refreshTime() > 0) //
				&& (uExt.refreshTime() > 0) //
				&& (fExt.refreshTime() > 0) //
				&& (qInt.refreshTime() > 0) //
				&& (qExt.refreshTime() > 0));
	}

	@Override
	public String toString() {
		String rs = String.format("GRID CONNECTOR :\nP internal \t\t %.1f \t %.1f \t %.1f W\n", getACPower(0),
				getACPower(1), getACPower(2))
				+ String.format("P external   \t\t %.1f \t %.1f \t %.1f W\n", getACPowerExt(0), getACPowerExt(1),
						getACPowerExt(2))
				+ String.format("Q internal   \t\t %.1f \t %.1f \t %.1f var\n", getReactivePower(0),
						getReactivePower(1), getReactivePower(2))
				+ String.format("Q external   \t\t %.1f \t %.1f \t %.1f var\n", getReactivePowerExt(0),
						getReactivePowerExt(1), getReactivePowerExt(2))
				+ String.format("U On Grid   \t\t %.1f \t %.1f \t %.1f V\n", getACVoltageOnGrid(0),
						getACVoltageOnGrid(1), getACVoltageOnGrid(2))
				+ String.format("Grid Frequency   \t\t %.2f Hz\n", getFrequencyExt());
		return rs;
	}

}
//CHECKSTYLE:ON
