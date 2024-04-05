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

/**
 * Float data representation.
 */
public final class DspFloat extends ADspData {

	/**
	 * IEEE 754 Float type size.
	 */
	public static final int DSP_FLOAT_SIZE = 4;

	/**
	 * Class constructor.
	 *
	 * @param name          variable name (according to embedded software)
	 * @param len           array length ('1' for non array)
	 * @param listner       on change listener
	 * @param refreshPeriod required refresh period in milliseconds, '0' - no
	 *                      refresh required.
	 * @throws Exception wrong parameters
	 */
	public DspFloat(String name, int len, DspVarListener listner, long refreshPeriod) throws Exception {
		super(name, len * DSP_FLOAT_SIZE, listner, refreshPeriod);
	}

	/**
	 * Get variable value by index.
	 *
	 * @return Float value
	 */
	@Override
	public Object getValue() {
		return getFloat(0);
	}

	/**
	 * Get float value by index.
	 *
	 * @param ix index in float array ('0' for non array)
	 * @return float value
	 * @throws IndexOutOfBoundsException If index is negative or not smaller than
	 *                                   the limit
	 */
	public synchronized float getFloat(int ix) throws IndexOutOfBoundsException {
		return bufRead.getFloat(ix * DSP_FLOAT_SIZE);
	}

	/**
	 * Set float value by index.
	 *
	 * @param fv new float value
	 * @param ix index in float array
	 * @throws IndexOutOfBoundsException If index is negative or not smaller than
	 *                                   the limit
	 */
	public synchronized void setFloat(float fv, int ix) throws IndexOutOfBoundsException {
		setModifiedNow();
		bufWrite.putFloat(ix * DSP_FLOAT_SIZE, fv);
		setChanged();
	}
}
//CHECKSTYLE:ON
