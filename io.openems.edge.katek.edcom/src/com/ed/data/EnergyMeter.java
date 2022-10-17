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
import com.ed.edcom.DspVar;

/**
 * Inverter internal energy meter representation
 */
public final class EnergyMeter implements DataSet {

	public static final int DAY = 0;
	public static final int MONTH = 1;
	public static final int YEAR = 2;

	private final DspVar e_inverter_feed_in;
	private final DspVar e_inverter_cons;
	private final DspVar e_grid_feed_in;
	private final DspVar e_consumption_from_grid;
	private final DspVar e_self_consumption;
	private final DspVar ah_battery;

	/**
	 * Creates a object representing inverter energy meter
	 *
	 * @throws java.lang.Exception wrong parameters
	 */
	public EnergyMeter() throws Exception {
		e_inverter_feed_in = new DspVar("dd.e_inverter_inj", DspVar.TYPE_UINT32, 3, null, 0);
		e_inverter_cons = new DspVar("dd.e_inverter_cons", DspVar.TYPE_UINT32, 3, null, 0);
		e_grid_feed_in = new DspVar("dd.e_grid_inj", DspVar.TYPE_UINT32, 3, null, 0);
		e_consumption_from_grid = new DspVar("dd.e_grid_cons", DspVar.TYPE_UINT32, 3, null, 0);
		e_self_consumption = new DspVar("dd.e_compensation", DspVar.TYPE_UINT32, 3, null, 0);
		ah_battery = new DspVar("dd.q_acc", DspVar.TYPE_UINT32, 3, null, 0);
	}

	/**
	 * Get Inverter Feed In energy counter
	 *
	 * @param timePeriod to calculate energy counter (use static definitions
	 *                   EnergyMeter.DAY, EnergyMeter.MONTH or EnergyMeter.YEAR)
	 * @return Energy [Wh]
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public float getEInverterFeedIn(int timePeriod) throws Exception {
		float r = 0.0f;
		if (e_inverter_feed_in.refreshTime() == 0) {
			throw new RuntimeException("no data");
		}
		switch (timePeriod) {
		default:
			throw new RuntimeException("wrong parameters");
		case EnergyMeter.DAY:
			r = qXToFloat(e_inverter_feed_in.getLong(timePeriod), 16, 240000.0f);
			break;
		case EnergyMeter.MONTH:
			r = qXToFloat(e_inverter_feed_in.getLong(timePeriod), 16, 7200000.0f);
			break;
		case EnergyMeter.YEAR:
			r = qXToFloat(e_inverter_feed_in.getLong(timePeriod), 32, 87600000.0f);
			break;
		}
		return r;
	}

	/**
	 * Get Inverter Consumption energy counter
	 *
	 * @param timePeriod to calculate energy counter (use static definitions
	 *                   EnergyMeter.DAY, EnergyMeter.MONTH or EnergyMeter.YEAR)
	 * @return Energy [Wh]
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public float getEInverterCons(int timePeriod) throws Exception {
		float r = 0.0f;
		if (e_inverter_cons.refreshTime() == 0) {
			throw new RuntimeException("no data");
		}
		switch (timePeriod) {
		default:
			throw new RuntimeException("wrong parameters");
		case EnergyMeter.DAY:
			r = qXToFloat(e_inverter_cons.getLong(timePeriod), 16, 240000.0f);
			break;
		case EnergyMeter.MONTH:
			r = qXToFloat(e_inverter_cons.getLong(timePeriod), 16, 7200000.0f);
			break;
		case EnergyMeter.YEAR:
			r = qXToFloat(e_inverter_cons.getLong(timePeriod), 32, 87600000.0f);
			break;
		}
		return r;
	}

	/**
	 * Get Grid Feed In energy counter
	 *
	 * @param timePeriod to calculate energy counter (use static definitions
	 *                   EnergyMeter.DAY, EnergyMeter.MONTH or EnergyMeter.YEAR)
	 * @return Energy [Wh]
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public float getEGridFeedIn(int timePeriod) throws Exception {
		float r = 0.0f;
		if (e_grid_feed_in.refreshTime() == 0) {
			throw new RuntimeException("no data");
		}
		switch (timePeriod) {
		default:
			throw new RuntimeException("wrong parameters");
		case EnergyMeter.DAY:
			r = qXToFloat(e_grid_feed_in.getLong(timePeriod), 16, 2400000.0f);
			break;
		case EnergyMeter.MONTH:
			r = qXToFloat(e_grid_feed_in.getLong(timePeriod), 16, 72000000.0f);
			break;
		case EnergyMeter.YEAR:
			r = qXToFloat(e_grid_feed_in.getLong(timePeriod), 32, 876000000.0f);
			break;
		}
		return r;
	}

	/**
	 * Get Grid Consumption energy counter
	 *
	 * @param timePeriod to calculate energy counter (use static definitions
	 *                   EnergyMeter.DAY, EnergyMeter.MONTH or EnergyMeter.YEAR)
	 * @return Energy [Wh]
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public float getEConsFromGrid(int timePeriod) throws Exception {
		float r = 0.0f;
		if (e_consumption_from_grid.refreshTime() == 0) {
			throw new RuntimeException("no data");
		}
		switch (timePeriod) {
		default:
			throw new RuntimeException("wrong parameters");
		case EnergyMeter.DAY:
			r = qXToFloat(e_consumption_from_grid.getLong(timePeriod), 16, 2400000.0f);
			break;
		case EnergyMeter.MONTH:
			r = qXToFloat(e_consumption_from_grid.getLong(timePeriod), 16, 72000000.0f);
			break;
		case EnergyMeter.YEAR:
			r = qXToFloat(e_consumption_from_grid.getLong(timePeriod), 32, 876000000.0f);
			break;
		}
		return r;
	}

	/**
	 * Get Self Consumption energy counter
	 *
	 * @param timePeriod to calculate energy counter (use static definitions
	 *                   EnergyMeter.DAY, EnergyMeter.MONTH or EnergyMeter.YEAR)
	 * @return Energy [Wh]
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public float getESelfConsumption(int timePeriod) throws Exception {
		float r = 0.0f;
		if (e_self_consumption.refreshTime() == 0) {
			throw new RuntimeException("no data");
		}
		switch (timePeriod) {
		default:
			throw new RuntimeException("wrong parameters");
		case EnergyMeter.DAY:
			r = qXToFloat(e_self_consumption.getLong(timePeriod), 16, 240000.0f);
			break;
		case EnergyMeter.MONTH:
			r = qXToFloat(e_self_consumption.getLong(timePeriod), 16, 7200000.0f);
			break;
		case EnergyMeter.YEAR:
			r = qXToFloat(e_self_consumption.getLong(timePeriod), 32, 87600000.0f);
			break;
		}
		return r;
	}

	/**
	 * Get battery Ampere-hour counter
	 *
	 * @param timePeriod to calculate Ampere-hour counter (use static definitions
	 *                   EnergyMeter.DAY, EnergyMeter.MONTH or EnergyMeter.YEAR)
	 * @return Battery Ampere-hour [Ah]
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public float getAhBattery(int timePeriod) throws Exception {
		float r = 0.0f;
		if (ah_battery.refreshTime() == 0) {
			throw new RuntimeException("no data");
		}
		switch (timePeriod) {
		default:
			throw new RuntimeException("wrong parameters");
		case EnergyMeter.DAY:
			r = qXToFloat(ah_battery.getLong(timePeriod), 16, 600.0f);
			break;
		case EnergyMeter.MONTH:
			r = qXToFloat(ah_battery.getLong(timePeriod), 16, 18000.0f);
			break;
		case EnergyMeter.YEAR:
			r = qXToFloat(ah_battery.getLong(timePeriod), 32, 219000.0f);
			break;
		}
		return r;
	}

	protected static float qXToFloat(long qx, int fractBitsCnt, float fMax) {
		float f = (float) (((double) fMax * qx) / (float) ((long) 1 << fractBitsCnt));
		return f;
	}

	@Override
	public void registerData(Client cl) {
		cl.addDspVar(e_inverter_feed_in);
		cl.addDspVar(e_inverter_cons);
		cl.addDspVar(e_grid_feed_in);
		cl.addDspVar(e_consumption_from_grid);
		cl.addDspVar(e_self_consumption);
		cl.addDspVar(ah_battery);
		refresh();
	}

	@Override
	public void refresh() {
		e_inverter_feed_in.refresh();
		e_inverter_cons.refresh();
		e_grid_feed_in.refresh();
		e_consumption_from_grid.refresh();
		e_self_consumption.refresh();
		ah_battery.refresh();
	}

	@Override
	public boolean dataReady() {
		return (e_inverter_feed_in.refreshTime() > 0) //
				&& (e_inverter_cons.refreshTime() > 0) //
				&& (e_grid_feed_in.refreshTime() > 0) //
				&& (e_consumption_from_grid.refreshTime() > 0) //
				&& (e_self_consumption.refreshTime() > 0) //
				&& (ah_battery.refreshTime() > 0);
	}

	@Override
	public String toString() {
		String s;
		try {
			s = "Energy meter: \n";
			s = s + String.format("Inverter Feed In     \t\t%.1f kWh    \t%.1f kWh    \t%.1f kWh",
					getEInverterFeedIn(EnergyMeter.DAY) / 1000, getEInverterFeedIn(EnergyMeter.MONTH) / 1000,
					getEInverterFeedIn(EnergyMeter.YEAR) / 1000);
			s = s + String.format("\nInverter Cons.     \t\t%.1f kWh    \t%.1f kWh    \t%.1f kWh",
					getEInverterCons(EnergyMeter.DAY) / 1000, getEInverterCons(EnergyMeter.MONTH) / 1000,
					getEInverterCons(EnergyMeter.YEAR) / 1000);
			s = s + String.format("\nGrid Feed In       \t\t%.1f kWh    \t%.1f kWh    \t%.1f kWh",
					getEGridFeedIn(EnergyMeter.DAY) / 1000, getEGridFeedIn(EnergyMeter.MONTH) / 1000,
					getEGridFeedIn(EnergyMeter.YEAR) / 1000);
			s = s + String.format("\nCons. from grid    \t\t%.1f kWh    \t%.1f kWh    \t%.1f kWh",
					getEConsFromGrid(EnergyMeter.DAY) / 1000, getEConsFromGrid(EnergyMeter.MONTH) / 1000,
					getEConsFromGrid(EnergyMeter.YEAR) / 1000);
			s = s + String.format("\nSelf consumption   \t\t%.1f kWh    \t%.1f kWh    \t%.1f kWh",
					getESelfConsumption(EnergyMeter.DAY) / 1000, getESelfConsumption(EnergyMeter.MONTH) / 1000,
					getESelfConsumption(EnergyMeter.YEAR) / 1000);
			s = s + String.format("\nAh Battery         \t\t%.1f Ah     \t%.1f Ah     \t%.1f Ah\n",
					getAhBattery(EnergyMeter.DAY), getAhBattery(EnergyMeter.MONTH), getAhBattery(EnergyMeter.YEAR));
		} catch (Exception e) {
			s = "no data";
		}
		return s;
	}
}
//CHECKSTYLE:ON
