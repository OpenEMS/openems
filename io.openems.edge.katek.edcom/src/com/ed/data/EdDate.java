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
import com.ed.edcom.DspVar;

/**
 * Inverter date and time
 */
public final class EdDate implements DataSet {

	private final DspVar dt1;
	private final DspVar dt2;
	private final Calendar cal;

	/**
	 * Creates a object representing inverter time
	 *
	 * @param refreshPeriod auto refresh time period [ms], 0(zero) for request based
	 *                      refresh
	 * @throws java.lang.Exception wrong parameters
	 */
	public EdDate(int refreshPeriod) throws Exception {
		dt1 = new DspVar("rtc.SecMinHourDay", DspVar.TYPE_UINT32, 0, null, refreshPeriod);
		dt2 = new DspVar("rtc.DaWeMonthYear", DspVar.TYPE_UINT32, 0, null, refreshPeriod);
		cal = Calendar.getInstance();
	}

	/**
	 * Get inverter time
	 *
	 * @return inverter time
	 */
	public Date getDate() {
		cal.clear();
		long smhd = dt1.getLong();
		long dwmy = dt2.getLong();
		cal.set(Calendar.SECOND, (int) ((smhd >> 24) & 0xFF));
		cal.set(Calendar.MINUTE, (int) ((smhd >> 16) & 0xFF));
		cal.set(Calendar.HOUR, (int) ((smhd >> 8) & 0xFF));
		cal.set(Calendar.DAY_OF_MONTH, (int) ((smhd) & 0xFF));
		cal.set(Calendar.DAY_OF_WEEK, (int) ((dwmy >> 24) & 0xFF));
		cal.set(Calendar.MONTH, (int) ((dwmy >> 16) & 0xFF));
		cal.set(Calendar.YEAR, (int) ((dwmy) & 0xFFFF));
		return cal.getTime();
	}

	@Override
	public void registerData(Client cl) {
		cl.addDspVar(dt1);
		cl.addDspVar(dt2);
		refresh();
	}

	@Override
	public void refresh() {
		dt1.refresh();
		dt2.refresh();
	}

	@Override
	public boolean dataReady() {
		return (dt1.refreshTime() > 0 && dt2.refreshTime() > 0);
	}

	@Override
	public String toString() {
		String s = "-";
		Date d = getDate();
		if (d != null) {
			s = "Inverter Time: " + d.toString() + "\n";
		}
		return s;
	}
}
//CHECKSTYLE:ON
