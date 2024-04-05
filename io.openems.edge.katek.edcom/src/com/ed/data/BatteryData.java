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
import com.ed.edcom.DspVar;

/**
 * Battery measurements
 */
public final class BatteryData implements DataSet {

	/**
	 * Battery power, basic data
	 */
	public final DspFloat pBat;
	/**
	 * Battery voltage (BMS), basic data
	 */
	public final DspFloat uBms;
	/**
	 * Battery state of energy, basic data
	 */
	public final DspFloat soe;
	/**
	 * Battery charge current limit
	 */
	public final DspVar I_lim_charge;
	/**
	 * Battery discharge current limit
	 */
	public final DspVar I_lim_discharge;
	/**
	 * Battery nominal energy
	 */
	public final DspVar bms_energy_nom;

	/**
	 * Battery nominal power
	 */
	public final DspVar bms_power_nom;

	/**
	 * Battery minimum cell voltage
	 */
	public final DspVar bms_u_cell_min_total;

	/**
	 * Battery maximum cell voltage
	 */
	public final DspVar bms_u_cell_max_total;

	/**
	 * Battery mininum cell temperature [°C]
	 */
	public final DspVar bms_Tmin_total;

	/**
	 * Battery maximum cell temperature [°C]
	 */
	public final DspVar bms_Tmax_total;

	/**
	 * Battery total cycles count
	 */
	public final DspVar bms_cycles;

	/**
	 * Creates a object representing battery measurements
	 *
	 * @throws java.lang.Exception wrong parameters
	 */
	public BatteryData() throws Exception {
		pBat = new DspFloat("g_sync.p_accu", 1, null, 0);
		uBms = new DspFloat("bms.u_total", 1, null, 0);
		soe = new DspFloat("bms.SOEpercent_total", 1, null, 0);
		I_lim_charge = new DspVar("bms.i_ch_total", 1, 1, null, 1000L);
		I_lim_discharge = new DspVar("bms.i_disch_total", 1, 1, null, 1000L);
		bms_energy_nom = new DspVar("bms.energy_nom", 1, 1, null, 1000L);
		bms_power_nom = new DspVar("bms.power_nom", 1, 1, null, 1000L);
		bms_u_cell_min_total = new DspVar("bms.U_cell_min_total", 1, 1, null, 1000L);
		bms_u_cell_max_total = new DspVar("bms.U_cell_max_total", 1, 1, null, 1000L);
		bms_Tmin_total = new DspVar("bms.T_cell_min_total", 1, 1, null, 1000L);
		bms_Tmax_total = new DspVar("bms.T_cell_max_total", 1, 1, null, 1000L);
		bms_cycles = new DspVar("bms.cycles", 1, 1, null, 1000L);
	}

	/**
	 * Get current battery power
	 *
	 * @return battery power [W] '+' charging, '-' discharging
	 */
	public float getPower() {
		return pBat.getFloat(0);
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
	 * Get state of energy (state of charge)
	 *
	 * @return state of energy [%]
	 */
	public float getSOE() {
		return soe.getFloat(0);
	}

	/**
	 * Get charge current limit
	 *
	 * @return charge limit [A]
	 */
	public float getCurrentLimitCharge() {
		return I_lim_charge.getFloat();
	}

	/**
	 * Get discharge current limit
	 *
	 * @return discharge limit [A]
	 */
	public float getCurrentLimitDischarge() {
		return I_lim_discharge.getFloat();
	}

	/**
	 * Get nominal energy
	 *
	 * @return nominal energy [KWh]
	 */
	public float getNominalEnergy() {
		return bms_energy_nom.getFloat();
	}

	/**
	 * Get nominal power
	 *
	 * @return nominal power [KW]
	 */
	public float getNominalPower() {
		return bms_power_nom.getFloat();
	}

	/**
	 * Get minimum cell voltage
	 *
	 * @return cell voltage [V]
	 */
	public float getMinCellVoltage() {
		return bms_u_cell_min_total.getFloat();
	}

	/**
	 * Get maximum cell voltage
	 *
	 * @return cell voltage [V]
	 */
	public float getMaxCellVoltage() {
		return bms_u_cell_max_total.getFloat();
	}

	/**
	 * Get minimum cell temperature
	 *
	 * @return cell temperature [°C]
	 */
	public float getMinCellTemp() {
		return bms_Tmin_total.getFloat();
	}

	/**
	 * Get maximum cell temperature
	 *
	 * @return cell temperature [°C]
	 */
	public float getMaxCellTemp() {
		return bms_Tmax_total.getFloat();
	}

	/**
	 * Get cycles count
	 *
	 * @return cycles count
	 */
	public int getCycles() {
		return bms_cycles.getInteger();
	}

	@Override
	public void registerData(Client cl) {
		cl.addDspVar(pBat);
		cl.addDspVar(uBms);
		cl.addDspVar(soe);
		cl.addDspVar(I_lim_charge);
		cl.addDspVar(I_lim_discharge);
		cl.addDspVar(bms_energy_nom);
		cl.addDspVar(bms_power_nom);
		cl.addDspVar(bms_u_cell_min_total);
		cl.addDspVar(bms_u_cell_max_total);
		cl.addDspVar(bms_Tmax_total);
		cl.addDspVar(bms_Tmin_total);
		cl.addDspVar(bms_cycles);
		refresh();
	}

	@Override
	public void refresh() {
		pBat.refresh();
		uBms.refresh();
		soe.refresh();
		I_lim_charge.refresh();
		I_lim_discharge.refresh();
		bms_energy_nom.refresh();
		bms_power_nom.refresh();
		bms_u_cell_min_total.refresh();
		bms_u_cell_max_total.refresh();
		bms_Tmax_total.refresh();
		bms_Tmin_total.refresh();
		bms_cycles.refresh();
	}

	@Override
	public boolean dataReady() {
		return ((pBat.refreshTime() > 0) //
				&& (uBms.refreshTime() > 0) //
				&& (soe.refreshTime() > 0) //
				&& (I_lim_charge.refreshTime() > 0) //
				&& (I_lim_discharge.refreshTime() > 0) //
				&& (bms_energy_nom.refreshTime() > 0) //
				&& (bms_power_nom.refreshTime() > 0) //
				&& (bms_u_cell_min_total.refreshTime() > 0) //
				&& (bms_u_cell_max_total.refreshTime() > 0) //
				&& (bms_Tmin_total.refreshTime() > 0) //
				&& (bms_Tmax_total.refreshTime() > 0) //
				&& (bms_cycles.refreshTime() > 0));
	}

	@Override
	public String toString() {
		return String.format("""
				Battery :
				Power %.1f W
				 U BMS %.1f V
				 SOE \
				%.1f %%
				 I limit charge %.1f A
				 I limit discharge \
				%.1f A\s
				 Nominal energy %.1f kWh
				 Nominal power \
				%.1f KW
				 U cell min %.1f V
				 U cell max %.1f V
				 \
				Cell min Temp %.1f °C
				 Cell max Temp %.1f °C
				 \
				Cycles %d
				""", //
				pBat.getFloat(0), uBms.getFloat(0), soe.getFloat(0), I_lim_charge.getFloat(),
				I_lim_discharge.getFloat(), bms_energy_nom.getFloat(), bms_power_nom.getFloat(),
				bms_u_cell_min_total.getFloat(), bms_u_cell_max_total.getFloat(), bms_Tmin_total.getFloat(),
				bms_Tmax_total.getFloat(), bms_cycles.getInteger());
	}

}
//CHECKSTYLE:ON
