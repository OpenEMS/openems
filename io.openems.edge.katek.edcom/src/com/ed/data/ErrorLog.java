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

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.ed.edcom.Client;
import com.ed.edcom.DspVar;

/**
 * Errors and Warnings log
 */
public final class ErrorLog implements DataSet {

	/**
	 * Error log, basic data
	 */
	private final DspVar errors;

	/**
	 * Creates a object representing errors log
	 *
	 * @throws java.lang.Exception wrong parameters
	 */
	public ErrorLog() throws Exception {
		errors = new DspVar("error_buf", DspVar.TYPE_UINT8, 480, null, 5000);
	}

	/**
	 * Get error records from inverter storage
	 *
	 * @return list of records
	 */
	@SuppressWarnings("unchecked")
	public List<ErrorRecord> getErrorsList() {
		List<ErrorRecord> rl = new LinkedList<>();
		byte b[] = errors.getBytes();
		for (int i = 0; i < b.length; i += ErrorRecord.RecordSize) {
			try {
				rl.add(new ErrorRecord(b, i, ErrorRecord.RecordSize));
			} catch (Exception e) {
			}
		}
		Collections.sort(rl);
		return rl;
	}

	/**
	 * Get error log
	 *
	 * @return Error codes that appeared on corresponding dates in ascending order.
	 */
	public SortedMap<Date, List<String>> getErrorLog() {
		List<ErrorRecord> records = getErrorsList();

		SortedMap<Date, List<String>> map = new TreeMap<>();

		records.forEach(error -> {
			map.put(error.getDTime(), error.getErrorCodes());
		});

		return map;
	}

	@Override
	public void registerData(Client cl) {
		cl.addDspVar(errors);
		refresh();
	}

	@Override
	public void refresh() {
		errors.refresh();
	}

	@Override
	public boolean dataReady() {
		return (errors.refreshTime() > 0);
	}

	@Override
	public String toString() {
		String rs = "ERROR LOG:\n";

		SortedMap<Date, List<String>> elog = getErrorLog();
		Iterator<Date> itr = elog.keySet().iterator();
		while (itr.hasNext()) {
			Date date = itr.next();
			rs += date.toString() + "\n";
			List<String> elist = elog.get(date);
			for (String error : elist) {
				rs += error + "\n";
			}
			rs += "\n";

		}

		return rs;
	}
}
//CHECKSTYLE:ON
