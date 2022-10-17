//CHECKSTYLE:OFF
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
 * Measurements available in demo mode
 *
 * @deprecated DemoData is only available in COM versions older than 8.0
 */
@Deprecated
public final class DemoData implements DataSet {

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
	 * VECTIS AC Voltage, basic data
	 */
	public final DspFloat uExt;
	/**
	 * Battery voltage (BMS), basic data
	 */
	public final DspFloat uBms;

	/**
	 * Creates a object representing measurements available in demo mode
	 *
	 * @throws java.lang.Exception wrong parameters
	 */
	public DemoData() throws Exception {
		uL1 = new DspFloat("g_sync.u_l_rms[0]", 1, null, 0);
		uL2 = new DspFloat("g_sync.u_l_rms[1]", 1, null, 0);
		uL3 = new DspFloat("g_sync.u_l_rms[2]", 1, null, 0);
		uSg1 = new DspFloat("g_sync.u_sg_avg[0]", 1, null, 0);
		uSg2 = new DspFloat("g_sync.u_sg_avg[1]", 1, null, 0);
		pSg = new DspFloat("g_sync.p_pv_lp", 1, null, 0);
		uBms = new DspFloat("bms.u_total", 1, null, 0);
		uExt = new DspFloat("rs.u_ext", 3, null, 0);
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
	 * Get battery voltage (BMS measurement)
	 *
	 * @return battery voltage [V]
	 */
	public float getBmsVoltage() {
		return uBms.getFloat(0);
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

	@Override
	public void registerData(Client cl) {
		cl.addDspVar(uL1);
		cl.addDspVar(uL2);
		cl.addDspVar(uL3);
		cl.addDspVar(uSg1);
		cl.addDspVar(uSg2);
		cl.addDspVar(pSg);
		cl.addDspVar(uBms);
		cl.addDspVar(uExt);
		refresh();
	}

	@Override
	public void refresh() {
		uL1.refresh();
		uL2.refresh();
		uL3.refresh();
		uSg1.refresh();
		uSg2.refresh();
		pSg.refresh();
		uBms.refresh();
		uExt.refresh();
	}

	@Override
	public boolean dataReady() {
		return ((uL1.refreshTime() > 0) //
				&& (uL2.refreshTime() > 0) //
				&& (uL3.refreshTime() > 0) //
				&& (uSg1.refreshTime() > 0) //
				&& (uSg2.refreshTime() > 0) //
				&& (pSg.refreshTime() > 0) //
				&& (uBms.refreshTime() > 0) //
				&& (uExt.refreshTime() > 0));
	}

	@Override
	public String toString() {
		String rs = "";
		rs = rs.concat(String.format("Grid Voltage  \t\t %.1f \t %.1f \t %.1f V\n", getAcVoltage(0), getAcVoltage(1),
				getAcVoltage(2)));
		rs = rs.concat(String.format("VECTIS Voltage\t\t %.1f \t %.1f \t %.1f V\n", getACVoltageOnGrid(0),
				getACVoltageOnGrid(1), getACVoltageOnGrid(2)));
		rs = rs.concat(String.format("PV Voltage    \t\t %.1f \t %.1f V \t\n", getPvVoltage(0), getPvVoltage(1)));
		rs = rs.concat(String.format("PV Power      \t\t %.1f W\n", getPvPower()));
		rs = rs.concat(String.format("U battery     \t\t %.1f V\n", getBmsVoltage()));
		return rs;
	}
}
//CHECKSTYLE:ON
