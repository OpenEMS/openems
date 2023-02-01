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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * Basis class for all history data types
 */
public abstract class ADataItem {

	int id;
	int cs;
	byte[] bytes;
	Calendar cal;

	public ADataItem() {
		cal = Calendar.getInstance();
	}

	/**
	 * Load data from byte array
	 *
	 * @param ba raw bytes (embedded data format)
	 */
	protected final void initDataItem(byte[] ba) {
		bytes = new byte[ba.length];
		System.arraycopy(ba, 0, bytes, 0, ba.length);
		id = (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8);
		cs = (bytes[2] & 0xFF) | ((bytes[3] & 0xFF) << 8);
		cal.set(Calendar.YEAR, ((int) (bytes[4] & 0xFF) | ((int) (bytes[5] & 0xFF) << 8)));
		cal.set(Calendar.MONTH, ((int) (bytes[6] & 0xFF)) - 1);
		cal.set(Calendar.DAY_OF_MONTH, ((int) (bytes[7] & 0xFF)));
		cal.set(Calendar.HOUR_OF_DAY, ((int) (bytes[8] & 0xFF)));
		cal.set(Calendar.MINUTE, ((int) (bytes[9] & 0xFF)));
		cal.set(Calendar.SECOND, ((int) (bytes[10] & 0xFF)));
	}

	/**
	 * Compare dates
	 *
	 * @param t Date to compare with data block time stamp
	 * @return 0 - same time period, -1 - parameter is older than current data block
	 *         1 - parameter is younger than current data block
	 */
	abstract public int compareTo(Date t);

	/**
	 * Add time
	 *
	 * @param amount of time periods to add
	 * @return new date
	 */
	abstract public Date setNextTimePeriod(int amount);

	/**
	 * Get start of current time period
	 *
	 * @return time period start, time stamp [ms]
	 */
	abstract public long getTsStart();

	/**
	 * Get end of current time period
	 *
	 * @return time period start, time stamp [ms]
	 */
	abstract public long getTsEnd();

	/**
	 * Get data block name
	 *
	 * @return name
	 */
	abstract public String getTabName();

	/**
	 * Factory, create object
	 *
	 * @param b raw data
	 * @return current data item
	 */
	abstract public ADataItem getInstance(byte[] b);

	/**
	 * Factory, create empty object
	 *
	 * @param d by time
	 * @return current data item
	 */
	abstract public ADataItem getInstance(Date d);

	/**
	 * Check data block consistency (CS)
	 *
	 * @return true if data block consistent
	 */
	public boolean isValid() {
		int cs, csCalc = 0;
		if (bytes == null) {
			return false;
		}
		for (int i = 4; i < bytes.length; i++) {
			csCalc = (csCalc + (bytes[i] & 0xFF)) & 0xFFFF;
		}
		cs = (bytes[2] & 0xFF) | ((bytes[3] & 0xFF) << 8);
		if (cs == csCalc && cs != 0) {
			return true;
		}
		return false;
	}

	/**
	 * Get ID
	 *
	 * @return data block id (embedded ring buffer index)
	 */
	public int getId() {
		return id;
	}

	/**
	 * Get data block time
	 *
	 * @return data block time stamp [ms]
	 */
	public long getTimestamp() {
		return cal.getTimeInMillis();
	}

	/**
	 * Get data block time
	 *
	 * @return data block time stamp
	 */
	public Date getTime() {
		return cal.getTime();
	}

	/**
	 * Check data
	 *
	 * @return true if check sum is valid
	 * @throws Exception
	 */
	boolean isCS() throws Exception {
		boolean r = false;
		int cs, cs_calc = 0;
		if (bytes.length < 4) {
			throw new Exception("byte array is to small");
		}
		for (int i = 4; i < bytes.length; i++) {
			cs_calc = (cs_calc + (bytes[i] & 0xFF)) & 0xFFFF;
		}
		cs = (bytes[2] & 0xFF) | ((bytes[3] & 0xFF) << 8);
		if (cs == cs_calc && cs != 0) {
			r = true;
		}
		return r;
	}

	@Override
	public boolean equals(Object o) {
		boolean b = false;
		if (o instanceof ADataItem) {
			b = Arrays.equals(((ADataItem) o).bytes, this.bytes);
		}
		return b;
	}

	@Override
	public String toString() {
		String s = new String();
		for (byte b : bytes) {
			s = s + String.format("%02X ", b & 0xFF);
		}
		s = s.concat("\n");
		return s;
	}

	/**
	 * Get raw bytes
	 *
	 * @return raw bytes
	 */
	public byte[] getDlByts() {
		return bytes;
	}

	/**
	 * Fixed point format conversion
	 *
	 * @param qx           fixed point value, Qx
	 * @param fractBitsCnt fractional part bit count
	 * @param fMax         maximal value
	 * @return floating point value
	 */
	public static float qXToFloat(long qx, int fractBitsCnt, float fMax) {
		float f = (float) (((double) fMax * qx) / (float) ((long) 1 << fractBitsCnt));
		return f;
	}

	/**
	 * Read signed 8 bit value from byte array
	 *
	 * @param ba input byte array
	 * @param ix start index
	 * @return value
	 * @throws Exception Index Out Of Bounds
	 */
	public static byte getS8Value(byte[] ba, int ix) throws Exception {
		if (ix >= ba.length) {
			throw new IndexOutOfBoundsException("byte array is to small");
		}
		return (byte) (ba[ix]);
	}

	/**
	 * Read unsigned 8 bit value from byte array
	 *
	 * @param ba input byte array
	 * @param ix start index
	 * @return value
	 * @throws Exception Index Out Of Bounds
	 */
	public static short getU8Value(byte[] ba, int ix) throws Exception {
		if (ix >= ba.length) {
			throw new IndexOutOfBoundsException("byte array is to small");
		}
		return (short) ((ba[ix] & 0xFF));
	}

	/**
	 * Read signed 16 bit value from byte array
	 *
	 * @param ba input byte array
	 * @param ix start index
	 * @return value
	 * @throws Exception Index Out Of Bounds
	 */
	public static short getS16Value(byte[] ba, int ix) throws Exception {
		if (ix + 1 >= ba.length) {
			throw new IndexOutOfBoundsException("byte array is to small");
		}
		return (short) ((ba[ix] & 0xFF) | ((ba[ix + 1] & 0xFF) << 8));
	}

	/**
	 * Read unsigned 16 bit value from byte array
	 *
	 * @param ba input byte array
	 * @param ix start index
	 * @return value
	 * @throws Exception Index Out Of Bounds
	 */
	public static int getU16Value(byte[] ba, int ix) throws Exception {
		if (ix + 1 >= ba.length) {
			throw new IndexOutOfBoundsException("byte array is to small");
		}
		return (int) ((ba[ix] & 0xFF) | ((ba[ix + 1] & 0xFF) << 8));
	}

	/**
	 * Read signed 32 bit value from byte array
	 *
	 * @param ba input byte array
	 * @param ix start index
	 * @return value
	 * @throws Exception Index Out Of Bounds
	 */
	public static int getS32Value(byte[] ba, int ix) throws Exception {
		if (ix + 3 >= ba.length) {
			throw new IndexOutOfBoundsException("byte array is to small");
		}
		return (int) ((ba[ix] & 0xFF) | ((ba[ix + 1] & 0xFF) << 8) | ((ba[ix + 2] & 0xFF) << 16)
				| ((ba[ix + 3] & 0xFF) << 24));
	}

	/**
	 * Read unsigned 16 bit value from byte array
	 *
	 * @param ba input byte array
	 * @param ix start index
	 * @return value
	 * @throws Exception Index Out Of Bounds
	 */
	public static long getU32Value(byte[] ba, int ix) throws Exception {
		if (ix + 3 >= ba.length) {
			throw new IndexOutOfBoundsException("byte array is to small");
		}
		return (long) ((ba[ix] & 0xFF) | ((ba[ix + 1] & 0xFF) << 8) | ((ba[ix + 2] & 0xFF) << 16)
				| (((long) (ba[ix + 3] & 0xFF)) << 24));
	}

	/**
	 * Write valid check sum (byte 2 and 3)
	 *
	 * @param ba destination byte buffer
	 */
	public static void writeCs(byte[] ba) {
		int cs_calc = 0;
		for (int i = 4; i < ba.length; i++) {
			cs_calc = (cs_calc + (ba[i] & 0xFF)) & 0xFFFF;
		}
		ba[2] = (byte) (cs_calc & 0xFF);
		ba[3] = (byte) ((cs_calc >> 8) & 0xFF);
	}
}
//CHECKSTYLE:ON
