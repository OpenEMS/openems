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
 * Inverter measurements
 */
public final class InverterData implements DataSet {

	/**
	 * AC voltage L1, basic data
	 */
	public final DspFloat uL1;
	/**
	 * AC voltage L2, basic data
	 */
	public final DspFloat uL2;
	/**
	 * AC voltage L3, basic data
	 */
	public final DspFloat uL3;
	/**
	 * AC power L1, basic data
	 */
	public final DspFloat pL1;
	/**
	 * AC power L2, basic data
	 */
	public final DspFloat pL2;
	/**
	 * AC power L3, basic data
	 */
	public final DspFloat pL3;
	/**
	 * AC reactive power, basic data
	 */
	public final DspFloat qLx;
	/**
	 * PV voltage 1, basic data
	 */
	public final DspFloat uSg1;
	/**
	 * PV voltage 2, basic data
	 */
	public final DspFloat uSg2;
	/**
	 * PV power, basic data
	 */
	public final DspFloat pSg;
	/**
	 * Grid frequency, basic data
	 */
	public final DspFloat fGrid;
	/**
	 * Isolation resistance, basic data
	 */
	public final DspFloat rIso;

	/**
	 * Creates a object representing inverter measurements
	 *
	 * @throws java.lang.Exception wrong parameters
	 */
	public InverterData() throws Exception {
		uL1 = new DspFloat("g_sync.u_l_rms[0]", 1, null, 0);
		uL2 = new DspFloat("g_sync.u_l_rms[1]", 1, null, 0);
		uL3 = new DspFloat("g_sync.u_l_rms[2]", 1, null, 0);
		pL1 = new DspFloat("g_sync.p_ac[0]", 1, null, 0);
		pL2 = new DspFloat("g_sync.p_ac[1]", 1, null, 0);
		pL3 = new DspFloat("g_sync.p_ac[2]", 1, null, 0);
		qLx = new DspFloat("g_sync.q_ac", 3, null, 0);
		uSg1 = new DspFloat("g_sync.u_sg_avg[0]", 1, null, 0);
		uSg2 = new DspFloat("g_sync.u_sg_avg[1]", 1, null, 0);
		pSg = new DspFloat("g_sync.p_pv_lp", 1, null, 0);
		fGrid = new DspFloat("gd[0].f_l_slow", 1, null, 0);
		rIso = new DspFloat("iso.r", 1, null, 0);
	}

	/**
	 * Get AC Voltage (inverter AC input)
	 *
	 * @param phaseIx Phase index 0 (Phase 1), 1 and 2.
	 * @return AC RMS Voltage [V] (~ 20ms, grid synchronized measurement)
	 */
	public float getAcVoltage(int phaseIx) throws RuntimeException {
		float f;
		switch (phaseIx) {
		default:
			throw new RuntimeException("bad index");
		case 0:
			f = uL1.getFloat(0);
			break;
		case 1:
			f = uL2.getFloat(0);
			break;
		case 2:
			f = uL3.getFloat(0);
			break;
		}
		return f;
	}

	/**
	 * Get inverter AC Power
	 *
	 * @param phaseIx Phase index 0 (Phase 1), 1 and 2.
	 * @return AC power [W] (~ 20ms, grid synchronized measurement) '+' feed in, '-'
	 *         consumption
	 */
	public float getAcPower(int phaseIx) throws RuntimeException {
		float f;
		switch (phaseIx) {
		default:
			throw new RuntimeException("bad index");
		case 0:
			f = pL1.getFloat(0);
			break;
		case 1:
			f = pL2.getFloat(0);
			break;
		case 2:
			f = pL3.getFloat(0);
			break;
		}
		return f;
	}

	/**
	 * Get inverter Reactive Power
	 *
	 * @param phaseIx Phase index 0 (Phase 1), 1 and 2.
	 * @return Reactive power [var] (~ 20ms, grid synchronized measurement) '+'
	 *         overexcited, '-' underexcited
	 * @throws RuntimeException wrong parameters
	 */
	public float getReactivPower(int phaseIx) throws RuntimeException {
		return qLx.getFloat(phaseIx);
	}

	/**
	 * Get inverter PV Voltage
	 *
	 * @param pvIx PV input number 0 and 1.
	 * @return PV Voltage [V]
	 */
	public float getPvVoltage(int pvIx) throws RuntimeException {
		float f;
		switch (pvIx) {
		default:
			throw new RuntimeException("wrong index");
		case 0:
			f = uSg1.getFloat(0);
			break;
		case 1:
			f = uSg2.getFloat(0);
			break;
		}
		return f;
	}

	/**
	 * Get inverter PV Power
	 *
	 * @return PV power [W] (~ 20ms)
	 */
	public float getPvPower() {
		return pSg.getFloat(0);
	}

	/**
	 * Get Grid frequency on inverter AC connection
	 *
	 * @return Grid frequency [Hz]
	 */
	public float getGridFrequency() {
		return fGrid.getFloat(0);
	}

	/**
	 * Get isolation resistance of total system: PV, Battery and inverter
	 *
	 * @return Grid frequency [Ohm] 0.0f - isolation measurement not ready.
	 */
	public float getRIso() {
		return rIso.getFloat(0);
	}

	@Override
	public void registerData(Client cl) {
		cl.addDspVar(uL1);
		cl.addDspVar(uL2);
		cl.addDspVar(uL3);
		cl.addDspVar(pL1);
		cl.addDspVar(pL2);
		cl.addDspVar(pL3);
		cl.addDspVar(qLx);
		cl.addDspVar(uSg1);
		cl.addDspVar(uSg2);
		cl.addDspVar(pSg);
		cl.addDspVar(fGrid);
		cl.addDspVar(rIso);
		refresh();
	}

	@Override
	public void refresh() {
		uL1.refresh();
		uL2.refresh();
		uL3.refresh();
		pL1.refresh();
		pL2.refresh();
		pL3.refresh();
		qLx.refresh();
		uSg1.refresh();
		uSg2.refresh();
		pSg.refresh();
		fGrid.refresh();
		rIso.refresh();
	}

	@Override
	public boolean dataReady() {
		return (((uL1.refreshTime() > 0) //
				&& (uL2.refreshTime() > 0) //
				&& (uL3.refreshTime() > 0) //
				&& (pL1.refreshTime() > 0) //
				&& (pL2.refreshTime() > 0) //
				&& (pL3.refreshTime() > 0) //
				&& (qLx.refreshTime() > 0) //
				&& (uSg1.refreshTime() > 0) //
				&& (uSg2.refreshTime() > 0) //
				&& (pSg.refreshTime() > 0) //
				&& (fGrid.refreshTime() > 0) //
				&& (rIso.refreshTime() > 0)));
	}

	@Override
	public String toString() {
		String rs = "";
		if (pL1.refreshTime() > 0) {
			rs = rs.concat(String.format("Inverter :\nPac \t\t %.1f \t %.1f \t %.1f W\n", getAcPower(0), getAcPower(1),
					getAcPower(2)));
		}
		if (qLx.refreshTime() > 0) {
			rs = rs.concat(String.format("Qac \t\t %.1f \t %.1f \t %.1f var\n", getReactivPower(0), getReactivPower(1),
					getReactivPower(2)));
		}
		if (uL1.refreshTime() > 0) {
			rs = rs.concat(String.format("UL \t\t %.1f \t %.1f \t %.1f V\n", getAcVoltage(0), getAcVoltage(1),
					getAcVoltage(2)));
		}
		if (uSg1.refreshTime() > 0) {
			rs = rs.concat(String.format("U PV \t\t %.1f \t %.1f V \t\n", getPvVoltage(0), getPvVoltage(1)));
		}
		if (pSg.refreshTime() > 0) {
			rs = rs.concat(String.format("P PV \t\t %.1f W\n", getPvPower()));
		}
		if (fGrid.refreshTime() > 0) {
			rs = rs.concat(String.format("F grid \t\t %.1f Hz\n", getGridFrequency()));
		}
		if (rIso.refreshTime() > 0) {
			rs = rs.concat(String.format("R isolation \t%.1f Ohm\n", getRIso()));
		}
		return rs;
	}
}
//CHECKSTYLE:ON
