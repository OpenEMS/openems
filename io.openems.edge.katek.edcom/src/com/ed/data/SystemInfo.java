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

import com.ed.edcom.Client;
import com.ed.edcom.DspVar;

/**
 * Inverter general information
 */
public final class SystemInfo implements DataSet {

	/**
	 * Controller Software version, basic data
	 */
	public final DspVar dspSoftVersion;
	/**
	 * Com Software version, basic data
	 */
	public final DspVar comSoftVersion;
	/**
	 * VECTIS Software version, basic data
	 */
	public final DspVar vecSoftVersion;
	/**
	 * Serial number, basic data
	 */
	public final DspVar serialNumber;
	/**
	 * Inverter configuration, basic data
	 */
	public final DspVar invConfig;
	/**
	 * Inverter MAC address
	 */
	public final DspVar pic_mac;

	/**
	 * Creates a object representing inverter general information
	 *
	 * @throws java.lang.Exception wrong parameters
	 */
	public SystemInfo() throws Exception {
		dspSoftVersion = new DspVar("sw_version", DspVar.TYPE_UINT16, 0, null, 0);
		comSoftVersion = new DspVar("pic_version", DspVar.TYPE_UINT16, 0, null, 0);
		vecSoftVersion = new DspVar("rs.db_version", DspVar.TYPE_UINT16, 0, null, 0);
		serialNumber = new DspVar("dev_serial_num", DspVar.TYPE_UINT8, 20, null, 0);
		invConfig = new DspVar("dev_config_txt", DspVar.TYPE_UINT8, 20, null, 0);
		pic_mac = new DspVar("pic_mac", DspVar.TYPE_UINT8, 6, null, 30000L);
	}

	/**
	 * Get Controller version
	 *
	 * @return version as text major.minor
	 */
	public String getControllerVersion() {
		return String.format("%1d.%1d", dspSoftVersion.getByte(1) & 0xFF, dspSoftVersion.getByte(0) & 0xFF);
	}

	/**
	 * Get Com version
	 *
	 * @return version as text major.minor
	 */
	public String getComVersion() {
		return String.format("%1d.%1d", comSoftVersion.getByte(0) & 0xFF, comSoftVersion.getByte(1) & 0xFF);
	}

	/**
	 * Get VECTIS version
	 *
	 * @deprecated
	 * @return version as text major.minor
	 */
	@Deprecated
	public String getVectisVersion() {
		return getGridConVersion();
	}

	/**
	 * Get Grid connector version
	 *
	 * @return version as text major.minor
	 */
	public String getGridConVersion() {
		return String.format("%1d.%1d", vecSoftVersion.getByte(1) & 0xFF, vecSoftVersion.getByte(0) & 0xFF);
	}

	/**
	 * Get serial number
	 *
	 * @return serial number as text
	 */
	public String getSerialNumber() {
		return serialNumber.getCString();
	}

	/**
	 * Get configuration text
	 *
	 * @return inverter configuration as text
	 */
	public String getConfigTxt() {
		return invConfig.getCString();
	}

	/**
	 * Get inverter MAC address
	 *
	 * @return inverter MAC address as String (XX-XX-XX-XX-XX-XX)
	 */
	public String getMacAddress() {
		StringBuilder sb = new StringBuilder();
		byte[] mac = this.pic_mac.getBytes();
		sb.append(String.format("%02X-", mac[0] & 255));
		sb.append(String.format("%02X-", mac[1] & 255));
		sb.append(String.format("%02X-", mac[2] & 255));
		sb.append(String.format("%02X-", mac[3] & 255));
		sb.append(String.format("%02X-", mac[4] & 255));
		sb.append(String.format("%02X", mac[5] & 255));
		return sb.toString();
	}

	@Override
	public void registerData(Client cl) {
		cl.addDspVar(dspSoftVersion);
		cl.addDspVar(comSoftVersion);
		cl.addDspVar(vecSoftVersion);
		cl.addDspVar(serialNumber);
		cl.addDspVar(invConfig);
		cl.addDspVar(pic_mac);
		refresh();
	}

	@Override
	public void refresh() {
		dspSoftVersion.refresh();
		comSoftVersion.refresh();
		vecSoftVersion.refresh();
		serialNumber.refresh();
		invConfig.refresh();
		pic_mac.refresh();
	}

	@Override
	public boolean dataReady() {
		return ((dspSoftVersion.refreshTime() > 0) //
				&& (comSoftVersion.refreshTime() > 0) //
				&& (vecSoftVersion.refreshTime() > 0) //
				&& (serialNumber.refreshTime() > 0) //
				&& (invConfig.refreshTime() > 0) //
				&& (pic_mac.refreshTime() > 0));
	}

	@Override
	public String toString() {
		String rs = "System general information: \n";
		rs = rs.concat("\tController :        " + getControllerVersion() + "\n");
		rs = rs.concat("\tCom :               " + getComVersion() + "\n");
		rs = rs.concat("\tVectis :            " + getVectisVersion() + "\n");
		rs = rs.concat("\tSerial Nr.:         " + getSerialNumber() + "\n");
		rs = rs.concat("\tConfig :            " + getConfigTxt() + "\n");
		return rs;
	}
}
//CHECKSTYLE:ON
