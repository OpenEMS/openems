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

import java.util.Calendar;
import java.util.Date;

import com.ed.edcom.Client;
import com.ed.edcom.DspFloat;
import com.ed.edcom.DspVar;

/**
 * Inverter settings
 */
public final class Settings implements DataSet {

	/**
	 * Power limit energy provider, basic data
	 */
	public final DspVar powerLimitEp;
	/**
	 * Power limit, basic data
	 */
	public final DspVar powerLimit;
	/**
	 * AC Power set point, basic data
	 */
	public final DspFloat pacSetPoint;

	/**
	 * Battery Charging management
	 */
	public final DspVar bcumng_opt_reg;

	/**
	 * Battery Discharge Disable SOC
	 */
	public final DspVar bcumng_level_ds;

	/**
	 * Battery Discharge Enable SOC
	 */
	public final DspVar bcumng_level_ba;

	/**
	 * Battery prefered charge 10% SOC set point
	 */
	public final DspVar bcu_t1_prefch;

	/**
	 * Battery prefered charge 100% SOC set point
	 */
	public final DspVar bcu_t2_prefch;

	/**
	 * Battery actual prefered charge SOC set point
	 */
	public final DspVar bcu_soc_prefch;

	/**
	 * Creates a object representing inverter settings
	 *
	 * @throws java.lang.Exception wrong parameters
	 */
	public Settings() throws Exception {
		pacSetPoint = new DspFloat("inv.p_ac_house_set_new", 1, null, 0);
		powerLimitEp = new DspVar("nsm.p_lim_proz_es", DspVar.TYPE_UINT16, 0, null, 0);
		powerLimit = new DspVar("nsm.p_lim_proz", DspVar.TYPE_UINT16, 0, null, 0);
		bcumng_opt_reg = new DspVar("bcu.bcumng_opt_reg", DspVar.TYPE_UINT8, 1, null, 1000);
		bcumng_level_ds = new DspVar("bcu.bcumng_level_ds", DspVar.TYPE_FLOAT, 1, null, 1000);
		bcumng_level_ba = new DspVar("bcu.bcumng_level_ba", DspVar.TYPE_FLOAT, 1, null, 1000);
		bcu_t1_prefch = new DspVar("bcu.t1_prefch", DspVar.TYPE_UINT32, 0, null, 1000);
		bcu_t2_prefch = new DspVar("bcu.t2_prefch", DspVar.TYPE_UINT32, 0, null, 1000);
		bcu_soc_prefch = new DspVar("bcu.soc_prefch", DspVar.TYPE_FLOAT, 1, null, 1000);
	}

	/**
	 * Get inverter AC Power set point
	 *
	 * @return current AC power set point [W] '+' feed in, '-' consumption 0.0f -
	 *         compensator normal operation (if enabled)
	 */
	public float getPacSetPoint() {
		return pacSetPoint.getFloat(0);
	}

	/**
	 * Set inverter AC Power set point (no refresh required)
	 *
	 * @param pac new set point for inverter AC Power '+' feed in, '-' consumption
	 *            0.0f - compensator normal operation (if enabled)
	 */
	public void setPacSetPoint(float pac) {
		pacSetPoint.setFloat(pac, 0);
	}

	/**
	 * Get Energy provider Power Limit
	 *
	 * @return current power limit 0..100 [%]
	 */
	public float getEPLimit() {
		return (float) powerLimitEp.getLong();
	}

	/**
	 * Set Energy provider Power Limit (no refresh required)
	 *
	 * @param limit new power limit 0..100 [%]
	 */
	public void setEPLimit(float limit) {
		powerLimitEp.setValue(Math.round(limit));
	}

	/**
	 * Get grid power limit
	 *
	 * @return current power limit [W]
	 */
	public float getGridPowerLimit() {
		return 100.0f * Math.min(powerLimit.getLong(0), powerLimitEp.getLong(0));
	}

	/**
	 * Get Battery Discharge Disable Option
	 * 
	 * @return true if option is on
	 */
	public boolean getBatDischargeDisabledOn() {
		return (bcumng_opt_reg.getInteger() & 1) != 0;
	}

	/**
	 * Get Battery Discharge Enable Option
	 * 
	 * @return true if option is on
	 */
	public boolean getBatDischargeEnabledOn() {
		return (bcumng_opt_reg.getInteger() & (1 << 1)) != 0;
	}

	public boolean isPreferredChargeEnabled() {
		return (bcumng_opt_reg.getInteger() & (1 << 2)) != 0;
	}

	/**
	 * Get SOC at which further discharge of Battery shall be disabled
	 * 
	 * @return SOC [%]
	 */
	public float getBatDischargeDisabledSOC() {
		return bcumng_level_ds.getFloat();
	}

	/**
	 * Get SOC at which discharge of Battery shall be enabled
	 * 
	 * @return SOC [%]
	 */
	public float getBatDischargeEnabledSOC() {
		return bcumng_level_ba.getFloat();
	}

	/**
	 * Get actual SOC set point of preferred charge
	 * 
	 * @return SOC [%]
	 */
	public float getPrefChargeActualSOC() {
		return bcu_soc_prefch.getFloat();
	}

	/**
	 * Set battery discharge disable option
	 * 
	 * @param set true: option on, false: option off.
	 */
	public void setBatDischargeDisabledOn(boolean set) {
		int tmp = bcumng_opt_reg.getInteger();
		if (!set && ((tmp & 1) != 0)) {
			tmp &= ~1;
		}
		if (set && ((tmp & 1) == 0)) {
			tmp |= 1;
		}
		bcumng_opt_reg.setValue(tmp);
	}

	/**
	 * Set battery discharge enable option
	 * 
	 * @param set true: option on, false: option off.
	 */
	public void setBatDischargeEnabledOn(boolean set) {
		int tmp = bcumng_opt_reg.getInteger();
		if (!set && ((tmp & 2) != 0)) {
			tmp &= ~2;
		}
		if (set && ((tmp & 2) == 0)) {
			tmp |= 2;
		}
		bcumng_opt_reg.setValue(tmp);
	}

	/**
	 * Enable / Disable preferred battery charging
	 * 
	 * @param set true: option on, false: option off.
	 */
	public void setPrefChargeEnabled(boolean set) {
		int tmp = bcumng_opt_reg.getInteger();
		if (!set && ((tmp & (1 << 2)) != 0)) {
			tmp &= ~(1 << 2);
		}
		if (set && ((tmp & (1 << 2)) == 0)) {
			tmp |= (1 << 2);
		}
		bcumng_opt_reg.setValue(tmp);
	}

	/**
	 * Get preferred Charge start time
	 * 
	 * @return start time (HH:mm)
	 */
	public Date getPrefChargeStartPoint() {
		return this.getPreferredChargeTime((Long) bcu_t1_prefch.getValue());
	}

	/**
	 * Get preferred Charge end time
	 * 
	 * @return end time (HH:mm)
	 */
	public Date getPrefChargeEndPoint() {
		return this.getPreferredChargeTime((Long) bcu_t2_prefch.getValue());
	}

	/**
	 * Set start time for preferred charge
	 *
	 * @param start start time as Date format (HH:mm). Start time shall always be
	 *              before end time.
	 */
	public void setPrefChargeStartPoint(Date start) {
		this.bcu_t1_prefch.setValue(this.formatPrefChargeSetPoint(start));
	}

	/**
	 * Set end time for preferred charge
	 *
	 * @param end end time as Date format (HH:mm). End time shall always be after
	 *            start time.
	 */
	public void setPrefChargeEndPoint(Date end) {
		this.bcu_t2_prefch.setValue(this.formatPrefChargeSetPoint(end));
	}

	/**
	 * Set SOC value at which further battery discharging shall be disabled. This
	 * value only takes effect when Battery discharge disable option is on.
	 * 
	 * @param socLim the SOC Limit [%]
	 */
	public void setBatDischargeDisabledSOC(float socLim) {
		bcumng_level_ds.setValue(socLim);
	}

	/**
	 * Set SOC value at which battery discharging shall be enabled. This value only
	 * takes effect when Battery discharge enable option is on.
	 * 
	 * @param socLim the SOC Limit [%]
	 */
	public void setBatDischargeEnabledSOC(float socLim) {
		bcumng_level_ba.setValue(socLim);
	}

	@Override
	public void registerData(Client cl) {
		cl.addDspVar(powerLimit);
		cl.addDspVar(powerLimitEp);
		cl.addDspVar(pacSetPoint);
		cl.addDspVar(bcumng_opt_reg);
		cl.addDspVar(bcumng_level_ds);
		cl.addDspVar(bcumng_level_ba);
		cl.addDspVar(bcu_t1_prefch);
		cl.addDspVar(bcu_t2_prefch);
		cl.addDspVar(bcu_soc_prefch);
		refresh();
	}

	@Override
	public void refresh() {
		pacSetPoint.refresh();
		powerLimitEp.refresh();
		powerLimit.refresh();
		bcumng_opt_reg.refresh();
		bcumng_level_ds.refresh();
		bcumng_level_ba.refresh();
		bcu_t1_prefch.refresh();
		bcu_t2_prefch.refresh();
		bcu_soc_prefch.refresh();
	}

	@Override
	public boolean dataReady() {
		return ((pacSetPoint.refreshTime() > 0) //
				&& (powerLimitEp.refreshTime() > 0) //
				&& (powerLimit.refreshTime() > 0) //
				&& (bcumng_opt_reg.refreshTime() > 0) //
				&& (bcumng_level_ds.refreshTime() > 0) //
				&& (bcumng_level_ba.refreshTime() > 0) //
				&& (bcu_soc_prefch.refreshTime() > 0) //
				&& (bcu_t1_prefch.refreshTime() > 0) //
				&& (bcu_t2_prefch.refreshTime() > 0));
	}

	@Override
	public String toString() {
		String rs = String.format("Pac SET \t\t\t %.1f W \nEnergy provider Limit \t\t %.1f %%\n", getPacSetPoint(),
				getEPLimit());
		rs = rs.concat(String.format("Max. grid power\t\t\t %.1f W\n", getGridPowerLimit()));
		rs = rs.concat("Battery Discharge Disable Limit\t\t\t " + getBatDischargeDisabledSOC() + " %\n");
		rs = rs.concat("Battery Discharge Enable Limit\t\t\t " + getBatDischargeEnabledSOC() + " %\n");

		return rs;
	}

	private Date getPreferredChargeTime(Long setPoint) {
		if (setPoint.equals(0L) || setPoint == null) {
			return new Date();
		}
		java.util.Date date = new java.util.Date();
		java.util.Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.clear();
		c.set(Calendar.MINUTE, (int) ((setPoint >> 16) & 0xFF));
		c.set(Calendar.HOUR, (int) ((setPoint >> 8) & 0xFF));
		date = c.getTime();
		return date;
	}

	private Long formatPrefChargeSetPoint(Date d) {
		Long tmp;
		java.util.Calendar c = Calendar.getInstance();
		c.setTime(d);
		tmp = (((long) c.get(Calendar.MINUTE)) << 16);
		tmp |= (((long) c.get(Calendar.HOUR_OF_DAY)) << 8);
		return tmp;
	}
}
//CHECKSTYLE:ON
