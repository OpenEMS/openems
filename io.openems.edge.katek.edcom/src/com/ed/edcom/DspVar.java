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
package com.ed.edcom;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Embedded Data Representations.
 */
public final class DspVar extends ADspData {

	/**
	 * Float IEEE 754, 32 bit
	 */
	public static final int TYPE_FLOAT = 1;
	/**
	 * Unsigned integer, 32 bit
	 */
	public static final int TYPE_UINT32 = 2;
	/**
	 * Signed integer, 32 bit
	 */
	public static final int TYPE_INT32 = 3;
	/**
	 * Unsigned integer, 16 bit
	 */
	public static final int TYPE_UINT16 = 4;
	/**
	 * Signed integer, 16 bit
	 */
	public static final int TYPE_INT16 = 5;
	/**
	 * Unsigned integer, 8 bit
	 */
	public static final int TYPE_UINT8 = 6;
	/**
	 * Signed integer, 8 bit
	 */
	public static final int TYPE_INT8 = 7;

	private final int type;
	private final int arrayLen;

	/**
	 * Class constructor.
	 *
	 * @param name          variable name (according to embedded software)
	 * @param type          variable type (TYPE_FLOAT, TYPE_UINT32 ... etc.)
	 * @param len           array length ('1' for non array)
	 * @param listner       on change listener
	 * @param refreshPeriod required refresh period in milliseconds, '0' - no
	 *                      refresh required.
	 * @throws Exception wrong parameters
	 */
	public DspVar(String name, int type, int len, DspVarListener listner, long refreshPeriod) throws Exception {
		super(name, getVarLen(type, len), listner, refreshPeriod);
		this.type = type;
		if (len <= 0) {
			len = 1;
		}
		this.arrayLen = len;
	}

	/**
	 * Get value.
	 *
	 * @return Float, Long, Integer or List (according to selected type)
	 */
	@Override
	public synchronized Object getValue() {
		if (this.arrayLen > 1) { // is array?
			List<Object> v = new ArrayList<Object>();
			for (int ix = 0; ix < this.arrayLen; ix++) {
				v.add(getValue(ix));
			}
			return v;
		} else {
			return getValue(0);
		}
	}

	/**
	 * Get value.
	 *
	 * @param ix index
	 * @return Float or Long (according to selected type)
	 */
	public Object getValue(int ix) {
		if (canReadAfterModify(3000)) {
			if (type == TYPE_FLOAT) {
				return Float.valueOf(getFloat(data, ix));
			} else {
				return Long.valueOf(getLong(data, ix));
			}
		} else {
			if (type == TYPE_FLOAT) {
				return Float.valueOf(getFloat(data_set, ix));
			} else {
				return Long.valueOf(getLong(data_set, ix));
			}
		}
	}

	/**
	 * Get c string
	 *
	 * @return string
	 */
	public String getCString() {
		String s = new String(data);
		s = s.replace(" ", "");
		return s;
	}

	/**
	 * Convert one array entry to string.
	 *
	 * @param format example "%.1f"
	 * @param ix     array index
	 * @return string
	 */
	public String toString(String format, int ix) {
		String r = "";
		if (!isValid()) {
			return "-";
		}
		switch (type) {
		case TYPE_FLOAT:
			if (ix < this.arrayLen) {
				r = String.format(format, getFloat(data, ix));
			}
			break;
		case TYPE_UINT32:
		case TYPE_INT32:
		case TYPE_UINT16:
		case TYPE_INT16:
		case TYPE_UINT8:
		case TYPE_INT8:
			if (ix < this.arrayLen) {
				r = String.format(format, getLong(data, ix));
			}
			break;
		}
		return r;
	}

	/**
	 * Convert to string
	 *
	 * @param format example "%.1f"
	 * @return string
	 */
	public String toString(String format) {
		String r = "";
		if (!isValid()) {
			return "-";
		}
		switch (type) {
		case TYPE_FLOAT:
			for (int i = 0; i < arrayLen; i++) {
				r = r.concat(String.format(format, getFloat(data, i)));
				if (i + 1 < arrayLen) {
					r = r + " ";
				}
			}
			break;
		case TYPE_UINT32:
		case TYPE_INT32:
		case TYPE_UINT16:
		case TYPE_INT16:
		case TYPE_UINT8:
		case TYPE_INT8:
			for (int i = 0; i < arrayLen; i++) {
				r = r.concat(String.format(format, getLong(data, i)));
				if (i + 1 < arrayLen) {
					r = r + " ";
				}
			}
			break;
		}
		return r;
	}

	/**
	 * Get long value
	 *
	 * @param ix index in array
	 * @return 64bit signed long
	 */
	public long getLong(int ix) {
		return getLong(data, ix);
	}

	/**
	 * Get long value
	 *
	 * @return 64bit signed long
	 */
	public long getLong() {
		return getLong(data, 0);
	}

	/**
	 * Get integer
	 *
	 * @return integer
	 */
	public int getInteger() {
		return (int) getLong(data, 0);
	}

	/**
	 * Get float value
	 *
	 * @param ix index in array
	 * @return float value
	 */
	public float getFloat(int ix) {
		return getFloat(data, ix);
	}

	/**
	 * Get float value
	 *
	 * @return float value
	 */
	public float getFloat() {
		return getFloat(data, 0);
	}

	/**
	 * Get float value with filter.
	 *
	 * @param ix  position in array
	 * @param old previous value
	 * @param fk  filter settings
	 * @return output float value
	 */
	public float getFloatValue(int ix, float old, float fk) {
		old += (getFloat(data, ix) - old) * fk;
		return old;
	}

	/**
	 * Get array of float
	 *
	 * @param f float array to fill
	 */
	public void getFloatArray(float f[]) {
		for (int i = 0; i < f.length && i < this.arrayLen; i++) {
			f[i] = getFloat(data, i);
		}
	}

	/**
	 * Get array of float with filter.
	 *
	 * @param f  output float array
	 * @param fk filter settings
	 */
	public void getFloatArray(float f[], float fk) {
		for (int i = 0; i < f.length && i < this.arrayLen; i++) {
			f[i] += (getFloat(data, i) - f[i]) * fk;
		}
	}

	/**
	 * Get array of long.
	 *
	 * @param la output long array
	 */
	public void getUint32Array(long la[]) {
		for (int i = 0; i < la.length && i < this.arrayLen; i++) {
			la[i] = getLong(data, i);
		}
	}

	/**
	 * Set value
	 *
	 * @param in Float, Long, Integer, ByteBuffer or String
	 */
	public synchronized void setValue(Object in) {
		setValue(in, 0);
	}

	/**
	 * Set value
	 *
	 * @param in Float, Long, Integer, ByteBuffer or String
	 * @param n  index in array
	 */
	public synchronized void setValue(Object in, int n) {
		String str, sa[];
		long t;
		int s = 0, ix = 0;
		if (in == null) {
			return;
		}
		setModifiedNow();
		if (in instanceof Float) {
			bufWrite.putFloat(n * 4, ((Float) in).floatValue());
		}
		if (in instanceof Long) {
			switch (type) {
			case TYPE_UINT32:
			case TYPE_INT32:
				bufWrite.putInt(n * 4, ((Long) in).intValue());
				break;
			case TYPE_UINT16:
			case TYPE_INT16:
				bufWrite.putShort(n * 2, ((Long) in).shortValue());
				break;
			case TYPE_UINT8:
			case TYPE_INT8:
				bufWrite.put(n, ((Long) in).byteValue());
				break;
			}
		}
		if (in instanceof Integer) {
			switch (type) {
			case TYPE_UINT32:
			case TYPE_INT32:
				bufWrite.putInt(n * 4, ((Integer) in).intValue());
				break;
			case TYPE_UINT16:
			case TYPE_INT16:
				bufWrite.putShort(n * 2, ((Integer) in).shortValue());
				break;
			case TYPE_UINT8:
			case TYPE_INT8:
				bufWrite.put(n, ((Integer) in).byteValue());
				break;
			}
		}
		if (in instanceof ByteBuffer) {
			ByteBuffer bf = (ByteBuffer) in;
			for (int i = 0; i < data_set.length && i < bf.capacity(); i++) {
				data_set[i] = bf.get(i);
			}
		}
		if (in instanceof String) {
			str = (String) in;
			str = str.replace("[", "");
			str = str.replace("]", "");
			str = str.replace(" ", "");
			str = str.replace("/", "");
			sa = str.split(",");
			for (ix = 0; ix < this.arrayLen && ix < sa.length; ix++) {
				switch (type) {
				default:
					break;
				case TYPE_FLOAT:
					s = ix * 4;
					bufWrite.putFloat(s, Float.parseFloat(sa[ix]));
					break;
				case TYPE_UINT32:
					s = ix * 4;
					t = Long.parseLong(sa[ix]);
					data_set[s + 0] = (byte) (t & 0xFF);
					data_set[s + 1] = (byte) ((t >> 8) & 0xFF);
					data_set[s + 2] = (byte) ((t >> 16) & 0xFF);
					data_set[s + 3] = (byte) ((t >> 24) & 0xFF);
					break;
				case TYPE_INT32:
					s = ix * 4;
					bufWrite.putInt(s, Integer.parseInt(sa[ix]));
					break;
				case TYPE_UINT16:
					s = ix * 2;
					t = Long.parseLong(sa[ix]);
					data_set[s + 0] = (byte) (t & 0xFF);
					data_set[s + 1] = (byte) ((t >> 8) & 0xFF);
					break;
				case TYPE_INT16:
					s = ix * 2;
					bufWrite.putShort(s, Short.parseShort(sa[ix]));
					break;
				case TYPE_UINT8:
					s = ix;
					t = Long.parseLong(sa[ix]);
					data_set[s] = (byte) (t & 0xFF);
					break;
				case TYPE_INT8:
					s = ix;
					bufWrite.put(s, Byte.parseByte(sa[ix]));
					break;
				}
			}
		}
		setChanged();
	}

	private synchronized long getLong(byte b[], int ix) throws IndexOutOfBoundsException {
		long l = 0;
		int s = 0;
		switch (type) {
		default:
			break;
		case TYPE_UINT32:
			s = ix * 4;
			if (s + 3 >= b.length) {
				throw new IndexOutOfBoundsException();
			}
			l = (b[s + 0] & 0xFF) | (b[s + 1] & 0xFF) << 8 | (b[s + 2] & 0xFF) << 16 | (b[s + 3] & 0xFF) << 24;
			break;
		case TYPE_INT32:
			s = ix * 4;
			l = bufRead.getInt(s);
			break;
		case TYPE_UINT16:
			s = ix * 2;
			if (s + 1 >= b.length) {
				throw new IndexOutOfBoundsException();
			}
			l = (b[s + 0] & 0xFF) | (b[s + 1] & 0xFF) << 8;
			break;
		case TYPE_INT16:
			s = ix * 2;
			l = bufRead.getShort(s);
			break;
		case TYPE_UINT8:
			s = ix;
			if (s >= b.length) {
				throw new IndexOutOfBoundsException();
			}
			l = (b[s + 0] & 0xFF);
			break;
		case TYPE_INT8:
			s = ix;
			l = bufRead.get(s);
			break;
		}
		return l;
	}

	synchronized float getFloat(byte b[], int ix) throws IndexOutOfBoundsException {
		return bufRead.getFloat(ix * 4);
	}

	private static int getVarLen(int vType, int vArrayLen) throws RuntimeException {
		int vLen = 0;
		if (vArrayLen == 0) {
			vArrayLen = 1;
		}
		switch (vType) {
		default:
			throw new RuntimeException("wrong variable type");
		case TYPE_FLOAT:
		case TYPE_UINT32:
		case TYPE_INT32:
			vLen = 4 * vArrayLen;
			break;
		case TYPE_UINT16:
		case TYPE_INT16:
			vLen = 2 * vArrayLen;
			break;
		case TYPE_UINT8:
		case TYPE_INT8:
			vLen = vArrayLen;
			break;
		}
		return vLen;
	}
}
//CHECKSTYLE:ON
