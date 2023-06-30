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
import java.util.LinkedList;
import java.util.List;

/**
 * Inverter error representation
 */
@SuppressWarnings("rawtypes")
public class ErrorRecord implements Comparable {

	/**
	 * Error record size (bytes) in ring buffer
	 */
	public static final int RecordSize = 24;

	// Error codes
	private static final String[][] FAULT_TEXT = { //
			/* 0 */{ "E160", "Fault" }, //
			/* 1 */ { "I901", "Reset" }, //
			/* 2 */ { "E170", "Fault" }, //
			/* 3 */ { "-", "Fault" }, //
			/* 4 */ { "E052", "AC Overcurrent" }, //
			/* 5 */ { "E051", "AC Overcurrent" }, //
			/* 6 */ { "E023", "Fault" }, //
			/* 7 */ { "E022", "Fault" }, //
			/* 8 */ { "E021", "Stoerung" }, //
			/* 9 */ { "E001", "PV Overvoltage 1" }, //
			/* 10 */ { "A010", "Isolation impedance" }, //
			/* 11 */ { "E180", "Initialization error" }, //
			/* 12 */ { "A002", "Grid overvoltage" }, //
			/* 13 */ { "-", "-" }, //
			/* 14 */ { "E200", "Battery overcurrent" }, //
			/* 15 */ { "E080", "Battery Relay: Initialization error" }, //
			/* 16 */ { "E150", "Initialization error" }, //
			/* 17 */ { "E030", "Battery overvoltage" }, //
			/* 18 */ { "A022", "AFI: 300mA" }, //
			/* 19 */ { "A021", "AFI: 30mA" }, //
			/* 20 */ { "E042", "Initialization error" }, //
			/* 21 */ { "E211", "Data storage error" }, //
			/* 22 */ { "A001", "Grid under voltage" }, //
			/* 23 */ { "A005", "Grid overvoltage: phase-to-phase fault" }, //
			/* 24 */ { "A004", "Grid under voltage: phase-to-phase fault" }, //
			/* 25 */ { "A050", "Grid frequency" }, //
			/* 26 */ { "-", "-" }, //
			/* 27 */ { "E002", "PV Overvoltage 2" }, //
			/* 28 */ { "E041", "Initialization error " }, //
			/* 29 */ { "E070", "AC-Relay: Initialization error" }, //
			/* 30 */ { "E090", "Temperature sensor" }, //
			/* 31 */ { "E110", "Fault" }, //
			/* 32 */ { "E101", "Fault" }, //
			/* 33 */ { "E102", "Fault" }, //
			/* 34 */ { "E103", "Fault" }, //
			/* 35 */ { "E104", "Fault" }, //
			/* 36 */ { "E120", "Initialization error" }, //
			/* 37 */ { "E130", "Fault" }, //
			/* 38 */ { "E140", "Fault" }, //
			/* 39 */ { "-", "-" }, //
			/* 40 */ { "-", "-" }, //
			/* 41 */ { "E011", "Grid overvoltage" }, //
			/* 42 */ { "E012", "Grid overvoltage" }, //
			/* 43 */ { "A003", "Grid overvoltage 10%" }, //
			/* 44 */ { "E220", "Fault: Hardware protection" }, //
			/* 45 */ { "E230", "Monitoring communication" }, //
			/* 46 */ { "A031", "Under-temperature" }, //
			/* 47 */ { "A032", "Over-temperature" }, //
			/* 48 */ { "-", "-" }, //
			/* 49 */ { "E060", "Fault" }, //
			/* 50 */ { "A060", "DC current injection into grid" }, //
			/* 51 */ { "N000", "Island: AC Voltage offset" }, //
			/* 52 */ { "N000", "Island: Over load" }, //
			/* 53 */ { "A100", "VECTIS: connection error" }, //
			/* 54 */ { "A110", "VECTIS: PE connection" }, //
			/* 55 */ { "-", "-" }, //
			/* 56 */ { "A260", "Battery not detected" }, //
			/* 57 */ { "E182", "Battery filter over voltage" }, //
			/* 58 */ { "E181", "Battery converter" }, //
			/* 59 */ { "-", "-" }, //
			/* 60 */ { "E191", "Battery Service" }, //
			/* 61 */ { "A240", "Battery Communication" }, //
			/* 62 */ { "-", "-" }, //
			/* 63 */ { "-", "-" }, //
			/* 64 0 */ { "A120", "No parameters found, start is not allowed" }, //
			/* 65 1 */ { "A071", "Start condition (Grid voltage)" }, //
			/* 66 2 */ { "A072", "Start condition (Grid frequency)" }, //
			/* 67 3 */ { "A131", "Power limit: P(t°)" }, //
			/* 68 4 */ { "A003", "Grid: Overvoltage 10%" }, //
			/* 69 5 */ { "A133", "Power limit: P(f)" }, //
			/* 70 6 */ { "A134", "Power limit: energy suppliers" }, //
			/* 71 7 */ { "A121", "Inverter data storage: supply is not stable" }, //
			/* 72 8 */ { "I142", "Battery can't be activated" }, //
			/* 73 9 */ { "I141", "VECTIS not detected" }, //
			/* 74 10 */ { "I031", "Centurio OFF-Button" }, //
			/* 75 11 */ { "I040", "Battery in initial charging mode" }, //
			/* 76 12 */ { "A122", "Inverter data storage: supply is not stable" }, //
			/* 77 13 */ { "A123", "Inverter data storage" }, //
			/* 78 14 */ { "A124", "Inverter data storage" }, //
			/* 79 15 */ { "A125", "Inverter settings has been changed" }, //
			/* 80 16 */ { "I032", "Pontos OFF-Button" }, //
			/* 81 17 */ { "-", "-" }, //
			/* 82 18 */ { "-", "-" }, //
			/* 83 19 */ { "-", "-" }, //
			/* 84 20 */ { "-", "-" }, //
			/* 85 21 */ { "-", "-" }, //
			/* 86 22 */ { "-", "-" }, //
			/* 87 23 */ { "-", "-" }, //
			/* 88 24 */ { "-", "-" }, //
			/* 89 25 */ { "-", "-" }, //
			/* 90 26 */ { "-", "-" }, //
			/* 91 27 */ { "-", "-" }, //
			/* 92 28 */ { "-", "-" }, //
			/* 93 29 */ { "-", "-" }, //
			/* 94 30 */ { "-", "-" }, //
			/* 95 31 */ { "-", "-" } //
	};

	private long bf1, bf2, bw1;
	private int ringBufCnt;
	private Date d;
	private boolean logEntry;

	/**
	 * Create object representing single error record
	 *
	 * @param bf1 error bit field 1
	 * @param bf2 error bit field 2
	 * @param wf1 warning bit field
	 */
	public ErrorRecord(long bf1, long bf2, long wf1) {
		d = Calendar.getInstance().getTime();
		ringBufCnt = -1;
		this.bf1 = bf1;
		this.bf1 = bf2;
		this.bw1 = wf1;
	}

	/**
	 * Create object representing single error record from byte array
	 *
	 * @param buf byte buffer (raw embedded data)
	 * @param six start index
	 * @param len error record length
	 */
	public ErrorRecord(byte[] buf, int six, int len) throws RuntimeException {
		if ((six + len) >= buf.length || len != RecordSize) {
			throw new RuntimeException("bad entry");
		}
		int cs_calc = 0;
		for (int i = (six + 4); i < (six + len); i++) {
			cs_calc = (cs_calc + (buf[i] & 0xFF)) & 0xFFFF;
		}
		int cs = (buf[six + 2] & 0xFF) | ((int) (buf[six + 3] & 0xFF) << 8);
		if (cs != cs_calc || cs_calc == 0) {
			throw new RuntimeException("bad entry");
		}
		Calendar cal = Calendar.getInstance();
		ringBufCnt = (buf[six] & 0xFF) | ((int) (buf[six + 1] & 0xFF) << 8);
		cal.clear();
		cal.set(Calendar.YEAR, ((int) (buf[six + 4] & 0xFF) | ((int) (buf[six + 5] & 0xFF) << 8)));
		cal.set(Calendar.MONTH, ((int) (buf[six + 6] & 0xFF)) - 1);
		cal.set(Calendar.DAY_OF_MONTH, ((int) (buf[six + 7] & 0xFF)));
		cal.set(Calendar.HOUR_OF_DAY, ((int) (buf[six + 8] & 0xFF)));
		cal.set(Calendar.MINUTE, ((int) (buf[six + 9] & 0xFF)));
		cal.set(Calendar.SECOND, ((int) (buf[six + 10] & 0xFF)));
		d = cal.getTime();
		bf1 = (long) (buf[six + 12] & 0xFF) | ((long) (buf[six + 13] & 0xFF) << 8)
				| ((long) (buf[six + 14] & 0xFF) << 16) | ((long) (buf[six + 15] & 0xFF) << 24);
		bf2 = (long) (buf[six + 16] & 0xFF) | ((long) (buf[six + 17] & 0xFF) << 8)
				| ((long) (buf[six + 18] & 0xFF) << 16) | ((long) (buf[six + 19] & 0xFF) << 24);
		bw1 = (long) (buf[six + 20] & 0xFF) | ((long) (buf[six + 21] & 0xFF) << 8)
				| ((long) (buf[six + 22] & 0xFF) << 16) | ((long) (buf[six + 23] & 0xFF) << 24);
		logEntry = true;
	}

	/**
	 * Get errors records list
	 *
	 * @return list of error codes (example : "E160")
	 */
	public List<String> getErrorCodes() {
		List<String> rl = new LinkedList<>();
		for (int i = 0, f = 1; i < 32; i++) {
			if ((bf1 & f) != 0) {
				if (FAULT_TEXT[i][0].length() > 1) { // not empty string ?
					rl.add(FAULT_TEXT[i][0]);
				}
			}
			f = f << 1;
		}
		for (int i = 32, f = 1; i < 64; i++) {
			if ((bf2 & f) != 0) {
				if (FAULT_TEXT[i][0].length() > 1) { // not empty string ?
					rl.add(FAULT_TEXT[i][0]);
				}
			}
			f = f << 1;
		}
		for (int i = 64, f = 1; i < 96; i++) {
			if ((bw1 & f) != 0) {
				if (FAULT_TEXT[i][0].length() > 1) { // not empty string ?
					rl.add(FAULT_TEXT[i][0]);
				}
			}
			f = f << 1;
		}
		return rl;
	}

	/**
	 * Get error record time stamp
	 *
	 * @return error time
	 */
	public Date getDTime() {
		return d;
	}

	/**
	 * Get record index (embedded ring buffer)
	 *
	 * @return error index
	 */
	public int getRingBufIndex() {
		return ringBufCnt;
	}

	@Override
	public int compareTo(Object t) {
		return ((ErrorRecord) t).getDTime().compareTo(d);
	}

	@Override
	public String toString() {
		String rs = "";
		if (logEntry) {
			rs = getDTime().toString() + "\t - ";
		}
		List<String> el = getErrorCodes();
		for (String e : el) {
			rs = rs.concat(e + " ");
		}
		return rs;
	}
}
//CHECKSTYLE:ON
