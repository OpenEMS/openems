//CHECKSTYLE:OFF
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

import com.ed.edcom.Client;

/**
 * General functions of data set
 */
public interface DataSet {
	/**
	 * Register all internal data
	 * 
	 * @param cl client connected to inverter
	 */
	public void registerData(Client cl);

	/**
	 * Put refresh request
	 */
	public void refresh();

	/**
	 * Get data status
	 * 
	 * @return true if internal data was read after last refresh request
	 */
	public boolean dataReady();

}
//CHECKSTYLE:ON
