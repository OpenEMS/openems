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
 * Callback.
 */
public interface ClientListener {

	/**
	 * Callback function.
	 */
	public default int getData(byte[] src, int spos, int len, byte[] key, byte[] dest, int dpos) {
		byte[] tmp = new byte[len];
		System.arraycopy(src, spos, tmp, 0, len);
		// apply key
		for (int i = 0; i < tmp.length && i < key.length; i++) {
			tmp[i] += key[i];
		}
		// simple mix
		for (int i = 0; i < 99; i++) {
			tmp[i % len] += 1;
			tmp[i % len] += tmp[(i + 10) % len];
			tmp[(i + 3) % len] *= tmp[(i + 11) % len];
			tmp[i % len] += tmp[(i + 7) % len];
		}
		System.arraycopy(tmp, 0, dest, dpos, len);
		return 1;
	}

	/**
	 * Frequently called by Client Class to get new encrypted Ident Key.
	 *
	 * @param randomKey the random Key frequently created by inverter.
	 * @return new encrypted Ident Key.
	 */
	public byte[] updateIdentKey(byte[] randomKey);

}
//CHECKSTYLE:ON
