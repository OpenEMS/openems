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
 * History Hour data block
 */
public class HourDataItem extends ADataItem implements Comparable<HourDataItem> {

	public static final int byteArrayLen = 276;
	public static final int cnt5Min = 12;
	public static final int cnt2Min = 30;

	/**
	 * Create object representing a hour History data
	 * 
	 * @param ba embedded data (raw)
	 */
	public HourDataItem(byte[] ba) {
		initDataItem(ba);
	}

	/**
	 * Create object representing empty hour data
	 * 
	 * @param d time
	 */
	public HourDataItem(Date d) {
		cal.setTime(d);
	}

	@Override
	public int compareTo(HourDataItem t) {
		return compareTo(t.getTime());
	}

	@Override
	public int compareTo(Date t) {
		Calendar c = Calendar.getInstance();
		c.setTime(t);
		if ((cal.get(Calendar.YEAR) == c.get(Calendar.YEAR)) && (cal.get(Calendar.MONTH) == c.get(Calendar.MONTH))
				&& (cal.get(Calendar.DAY_OF_MONTH) == c.get(Calendar.DAY_OF_MONTH))
				&& (cal.get(Calendar.HOUR_OF_DAY) == c.get(Calendar.HOUR_OF_DAY))) {
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
		cal.add(Calendar.HOUR, amount);
		return cal.getTime();
	}

	/**
	 * Get PV power [W], 2 minutes average values
	 * 
	 * @param dest    destination buffer to write, required length is 30
	 * @param destPos start index im destination buffer
	 * @return new destination position
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public int getPvPower(float[] dest, int destPos) throws Exception {
		int i, srcIx;
		for (i = destPos, srcIx = 12; (i < dest.length) && (srcIx < 72); i++, srcIx += 2) {
			dest[i] = qXToFloat(getU16Value(bytes, srcIx), 16, 10000.0f);
		}
		return destPos + 30;
	}

	/**
	 * Get House power [W], 2 minutes average values
	 * 
	 * @param dest    destination buffer to write, required length is 30
	 * @param destPos start index im destination buffer
	 * @return new destination position
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public int getHousePower(float[] dest, int destPos) throws Exception {
		int i, srcIx;
		for (i = destPos, srcIx = 72; (i < dest.length) && (srcIx < 132); i++, srcIx += 2) {
			dest[i] = qXToFloat(getS16Value(bytes, srcIx), 15, 100000.0f);
		}
		return destPos + 30;
	}

	/**
	 * Get Grid power [W], 2 minutes average values
	 * 
	 * @param dest    destination buffer to write, required length is 30
	 * @param destPos start index im destination buffer
	 * @return new destination position
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public int getGridPower(float[] dest, int destPos) throws Exception {
		int i, srcIx;
		for (i = destPos, srcIx = 132; (i < dest.length) && (srcIx < 192); i++, srcIx += 2) {
			dest[i] = qXToFloat(getS16Value(bytes, srcIx), 15, 100000.0f);
		}
		return destPos + 30;
	}

	/**
	 * Get battery State of Energy [%], 5 minutes average values
	 * 
	 * @param dest    destination buffer to write, required length is 12
	 * @param destPos start index im destination buffer
	 * @return new destination position
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public int getSOC(float[] dest, int destPos) throws Exception {
		int i, srcIx;
		for (i = destPos, srcIx = 192; (i < dest.length) && (srcIx < 204); i++, srcIx += 1) {
			dest[i] = qXToFloat(getU8Value(bytes, srcIx), 8, 100.0f);
		}
		return destPos + 12;
	}

	/**
	 * Get inverter AC Voltage phase L1 [V], 5 minutes average values
	 * 
	 * @param dest    destination buffer to write, required length is 12
	 * @param destPos start index im destination buffer
	 * @return new destination position
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public int getUL1(float[] dest, int destPos) throws Exception {
		int i, srcIx;
		for (i = destPos, srcIx = 204; (i < dest.length) && (srcIx < 216); i++, srcIx += 1) {
			float ftmp = (qXToFloat(getU8Value(bytes, srcIx), 8, 255.0f) * (2.0f / 2.55f) + 100.0f);
			if (ftmp <= 100.0f)
				ftmp = 0.0f;
			dest[i] = ftmp;
		}
		return destPos + 12;
	}

	/**
	 * Get inverter AC Voltage phase L2 [V], 5 minutes average values
	 * 
	 * @param dest    destination buffer to write, required length is 12
	 * @param destPos start index im destination buffer
	 * @return new destination position
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public int getUL2(float[] dest, int destPos) throws Exception {
		int i, srcIx;
		for (i = destPos, srcIx = 216; (i < dest.length) && (srcIx < 228); i++, srcIx += 1) {
			float ftmp = (qXToFloat(getU8Value(bytes, srcIx), 8, 255.0f) * (2.0f / 2.55f) + 100.0f);
			if (ftmp <= 100.0f)
				ftmp = 0.0f;
			dest[i] = ftmp;
		}
		return destPos + 12;
	}

	/**
	 * Get inverter AC Voltage phase L3 [V], 5 minutes average values
	 * 
	 * @param dest    destination buffer to write, required length is 12
	 * @param destPos start index im destination buffer
	 * @return new destination position
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public int getUL3(float[] dest, int destPos) throws Exception {
		int i, srcIx;
		for (i = destPos, srcIx = 228; (i < dest.length) && (srcIx < 240); i++, srcIx += 1) {
			float ftmp = (qXToFloat(getU8Value(bytes, srcIx), 8, 255.0f) * (2.0f / 2.55f) + 100.0f);
			if (ftmp <= 100.0f)
				ftmp = 0.0f;
			dest[i] = ftmp;
		}
		return destPos + 12;
	}

	/**
	 * Get inverter PV Voltage 1 [V], 5 minutes average values
	 * 
	 * @param dest    destination buffer to write, required length is 12
	 * @param destPos start index im destination buffer
	 * @return new destination position
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public int getUPV1(float[] dest, int destPos) throws Exception {
		int i, srcIx;
		for (i = destPos, srcIx = 240; (i < dest.length) && (srcIx < 252); i++, srcIx += 1) {
			float ftmp = (qXToFloat(getU8Value(bytes, srcIx), 8, 255.0f) * (9.0f / 2.55f) + 100.0f);
			if (ftmp <= 100.0f)
				ftmp = 0.0f;
			dest[i] = ftmp;
		}
		return destPos + 12;
	}

	/**
	 * Get inverter PV Voltage 2 [V], 5 minutes average values
	 * 
	 * @param dest    destination buffer to write, required length is 12
	 * @param destPos start index im destination buffer
	 * @return new destination position
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public int getUPV2(float[] dest, int destPos) throws Exception {
		int i, srcIx;
		for (i = destPos, srcIx = 252; (i < dest.length) && (srcIx < 264); i++, srcIx += 1) {
			float ftmp = (qXToFloat(getU8Value(bytes, srcIx), 8, 255.0f) * (9.0f / 2.55f) + 100.0f);
			if (ftmp <= 100.0f)
				ftmp = 0.0f;
			dest[i] = ftmp;
		}
		return destPos + 12;
	}

	/**
	 * Get battery Voltage [V], 5 minutes average values
	 * 
	 * @param dest    destination buffer to write, required length is 12
	 * @param destPos start index im destination buffer
	 * @return new destination position
	 * @throws Exception wrong parameters or no inverter data available
	 */
	public int getUBat(float[] dest, int destPos) throws Exception {
		int i, srcIx;
		for (i = destPos, srcIx = 264; (i < dest.length) && (srcIx < bytes.length); i++, srcIx += 1) {
			dest[i] = qXToFloat(getU8Value(bytes, srcIx), 8, 500.0f);
		}
		return destPos + 12;
	}

	@Override
	public long getTsStart() {
		Calendar c = Calendar.getInstance();
		c.setTime(cal.getTime());
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTimeInMillis();
	}

	@Override
	public long getTsEnd() {
		Calendar c = Calendar.getInstance();
		c.setTime(cal.getTime());
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		c.add(Calendar.HOUR, 1);
		return c.getTimeInMillis();
	}

	@Override
	public String getTabName() {
		return "THOUR";
	}

	@Override
	public ADataItem getInstance(byte[] b) {
		return new HourDataItem(b);
	}

	@Override
	public ADataItem getInstance(Date d) {
		return new HourDataItem(d);
	}

}
//CHECKSTYLE:ON
