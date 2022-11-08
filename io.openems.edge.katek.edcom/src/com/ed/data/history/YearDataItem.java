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
package com.ed.data.history;

import java.util.Calendar;
import java.util.Date;

/**
 * History Year data block
 */
public class YearDataItem extends ADataItem implements Comparable<YearDataItem> {

	public static final int byteArrayLen = 36;

	/**
	 * Create object representing year History data
	 * 
	 * @param ba embedded data (raw)
	 */
	public YearDataItem(byte[] ba) {
		initDataItem(ba);
	}

	/**
	 * Create object representing empty year data
	 * 
	 * @param d time
	 */
	public YearDataItem(Date d) {
		cal.setTime(d);
	}

	@Override
	public int compareTo(YearDataItem t) {
		return compareTo(t.getTime());
	}

	@Override
	public int compareTo(Date t) {
		Calendar c = Calendar.getInstance();
		c.setTime(t);
		if (cal.get(Calendar.YEAR) == c.get(Calendar.YEAR)) {
			return 0;
		} else {
			if (cal.getTime().after(c.getTime())) {
				return 1;
			} else {
				return -1;
			}
		}
	}

	@Override
	public Date setNextTimePeriod(int amount) {
		cal.add(Calendar.YEAR, amount);
		return cal.getTime();
	}

	/**
	 * Get Inverter Feed In energy
	 * 
	 * @return Energy [Wh]
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public float getInvInjEnergy() throws Exception {
		return qXToFloat(getU32Value(bytes, 12), 32, 87600000.0f);
	}

	/**
	 * Get Inverter consumption energy
	 * 
	 * @return Energy [Wh]
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public float getInvConsEnergy() throws Exception {
		return qXToFloat(getU32Value(bytes, 16), 32, 87600000.0f);
	}

	/**
	 * Get Grid Feed In energy counter
	 * 
	 * @return Energy [Wh]
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public float getGridInjEnergy() throws Exception {
		return qXToFloat(getU32Value(bytes, 20), 32, 876000000.0f);
	}

	/**
	 * Get Grid Consumption energy counter
	 * 
	 * @return Energy [Wh]
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public float getGridConsEnergy() throws Exception {
		return qXToFloat(getU32Value(bytes, 24), 32, 876000000.0f);
	}

	/**
	 * Get Self Consumption (compensation) energy
	 * 
	 * @return Energy [Wh]
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public float getCompensationEnergy() throws Exception {
		return qXToFloat(getU32Value(bytes, 28), 32, 87600000.0f);
	}

	/**
	 * Get battery Ampere-hour counter
	 * 
	 * @return Battery Ampere-hour [Ah]
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public float getQAcc() throws Exception {
		return qXToFloat(getU32Value(bytes, 32), 32, 219000.0f);
	}

	@Override
	public long getTsStart() {
		Calendar c = Calendar.getInstance();
		c.setTime(cal.getTime());
		c.set(Calendar.MONTH, 0);
		c.set(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTimeInMillis();
	}

	@Override
	public long getTsEnd() {
		Calendar c = Calendar.getInstance();
		c.setTime(cal.getTime());
		c.set(Calendar.MONTH, 0);
		c.set(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		c.add(Calendar.YEAR, 1);
		return c.getTimeInMillis();
	}

	@Override
	public String getTabName() {
		return "TYEAR";
	}

	@Override
	public ADataItem getInstance(byte[] b) {
		return new YearDataItem(b);
	}

	@Override
	public ADataItem getInstance(Date d) {
		return new YearDataItem(d);
	}
}
//CHECKSTYLE:ON
