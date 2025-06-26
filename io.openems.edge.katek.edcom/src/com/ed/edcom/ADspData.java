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
import java.nio.ByteOrder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the base class for representation of embedded data.
 */
public abstract class ADspData {

	/**
	 * Identifier 1
	 */
	public int externalKey1;
	/**
	 * Identifier 2
	 */
	public int externalKey2;

	/**
	 * Internal byte arrays, r/w
	 */
	protected final byte data[], data_set[];
	/**
	 * Internal byte buffer, r/w
	 */
	protected final ByteBuffer bufRead, bufWrite;

	private final String dspSwName;
	private final int hashCode;

	private long refreshTime;
	private long refrechPeriod;
	private boolean refreshRequired;
	private long validTimeAfterRefresh;

	private boolean changed;
	private long timeLastChange;

	private ADspData synchReadDspValue;
	private boolean bOptionEnable;
	private int requestValue;

	private final List<DspVarListener> changeListenesrs;

	private boolean readEnable = false;
	private boolean writeEnable = false;

	// DSP variable name, read & write permission
	private static final String PermList[][] = { { "g_sync.u_l_rms[0]", "1", "5" }, { "g_sync.u_l_rms[1]", "1", "5" },
			{ "g_sync.u_l_rms[2]", "1", "5" }, { "g_sync.u_sg_avg[0]", "1", "5" }, { "g_sync.u_sg_avg[1]", "1", "5" },
			{ "g_sync.p_ac[0]", "2", "5" }, { "g_sync.p_ac[1]", "2", "5" }, { "g_sync.p_ac[2]", "2", "5" },
			{ "rtc.SecMinHourDay", "1", "5" }, { "rtc.DaWeMonthYear", "1", "5" }, { "g_sync.p_pv_lp", "1", "5" },
			{ "g_sync.p_accu", "2", "5" }, { "bms.u_total", "1", "5" }, { "bms.SOEpercent_total", "2", "5" },
			{ "rtc.SecMinHourDay", "2", "5" }, { "rtc.DaWeMonthYear", "2", "5" }, { "dd.e_inverter_inj", "2", "5" },
			{ "dd.e_inverter_cons", "2", "5" }, { "dd.e_grid_inj", "2", "5" }, { "dd.e_grid_cons", "2", "5" },
			{ "dd.e_compensation", "2", "5" }, { "dd.q_acc", "2", "5" }, { "dd.db_asc", "2", "5" },
			{ "dd.hour_block_ext", "2", "5" }, { "dd.day_block_ext", "2", "5" }, { "dd.month_block_ext", "2", "5" },
			{ "dd.year_block_ext", "2", "5" }, { "g_sync.u_l_rms[0]", "2", "5" }, { "g_sync.u_l_rms[1]", "2", "5" },
			{ "g_sync.u_l_rms[2]", "2", "5" }, { "g_sync.p_ac[0]", "2", "5" }, { "g_sync.p_ac[1]", "2", "5" },
			{ "g_sync.p_ac[2]", "2", "5" }, { "g_sync.q_ac", "2", "5" }, { "g_sync.u_sg_avg[0]", "2", "5" },
			{ "g_sync.u_sg_avg[1]", "2", "5" }, { "g_sync.p_pv_lp", "2", "5" }, { "gd[0].f_l_slow", "2", "5" },
			{ "iso.r", "2", "5" }, { "inv.p_ac_house_set_new", "2", "2" }, { "nsm.p_lim_proz_es", "2", "2" },
			{ "nsm.p_lim_proz", "2", "2" }, { "fm.error_bits[0]", "2", "5" }, { "fm.error_bits[1]", "2", "5" },
			{ "warn_bf", "2", "5" }, { "prime_sm.ext_status", "2", "5" }, { "rs.tb_status", "2", "5" },
			{ "rs.dist_board_on", "2", "5" }, { "bms.Status_BMS.Allbits", "2", "5" },
			{ "prime_sm.inverter_mode", "2", "5" }, { "sw_version", "2", "5" }, { "pic_version", "2", "5" },
			{ "rs.db_version", "2", "5" }, { "dev_serial_num", "2", "5" }, { "dev_config_txt", "2", "5" },
			{ "rs.p_int", "2", "5" }, { "rs.p_ext", "2", "5" }, { "rs.u_ext", "1", "5" }, { "rs.f_ext", "2", "5" },
			{ "rs.q_int", "2", "5" }, { "rs.q_ext", "2", "5" }, { "error_buf", "2", "5" } };

	/**
	 * Class constructor.
	 *
	 * @param name          variable name (according to embedded software)
	 * @param varSize       bytes length
	 * @param listner       on change listener
	 * @param refreshPeriod required refresh period in milliseconds, '0' - no
	 *                      periodic refresh required.
	 * @throws Exception wrong parameters
	 */
	public ADspData(String name, int varSize, DspVarListener listner, long refreshPeriod) throws Exception {
		if (Util.userId < 1)
			throw new Exception("Library initialization error");
		if (varSize <= 0)
			throw new RuntimeException("wrong parameters");
		setPerission(name, Util.readPermission, Util.writePerission);
		data = new byte[varSize];
		data_set = new byte[varSize];
		this.dspSwName = name;
		this.refrechPeriod = refreshPeriod;
		this.validTimeAfterRefresh = this.refrechPeriod * 10;
		this.hashCode = getHashCode(dspSwName);
		changeListenesrs = new ArrayList<>();
		if (listner != null) {
			changeListenesrs.add(listner);
		}
		bufRead = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		bufWrite = ByteBuffer.wrap(data_set).order(ByteOrder.LITTLE_ENDIAN);
	}

	private void setPerission(String name, int rPerm, int wPerm) throws Exception {
		if (rPerm >= 5 && wPerm >= 5) {
			readEnable = writeEnable = true;
			return;
		}
		for (String[] entry : PermList) {
			if (entry[0].equals(name)) {
				if (rPerm >= Integer.parseInt(entry[1]))
					readEnable = true;
				if (wPerm >= Integer.parseInt(entry[2]))
					writeEnable = true;
			}
		}
	}

	/**
	 * Can read data according to user permissions?
	 * 
	 * @return true if reading is permitted
	 */
	public boolean canRead() {
		return readEnable;
	}

	/**
	 * Can write data according to user permissions?
	 * 
	 * @return true if writing is permitted
	 */
	public boolean canWrite() {
		return writeEnable;
	}

	/**
	 * Get variable value.
	 * 
	 * @return Object representing variable value
	 */
	public abstract Object getValue();

	/**
	 * Set time period for validity check.
	 *
	 * @param tp time in milliseconds
	 */
	public void setValidPeriod(long tp) {
		validTimeAfterRefresh = tp;
	}

	/**
	 * Validity check according to selected time period.
	 *
	 * @return true if valid
	 */
	public boolean isValid() {
		return (System.currentTimeMillis() < (refreshTime + validTimeAfterRefresh));
	}

	/**
	 * Get refresh timestamp.
	 *
	 * @return last refresh timestamp in milliseconds
	 */
	public long refreshTime() {
		return refreshTime;
	}

	/**
	 * Put refresh request and clear refresh timestamp. (Use 'refreshTime() != 0' to
	 * check data was refreshed)
	 */
	public synchronized void refresh() {
		if (synchReadDspValue != null) {
			synchReadDspValue.refreshTime = 0;
			synchReadDspValue.refreshRequired = true;
		}
		refreshTime = 0;
		refreshRequired = true;
	}

	/**
	 * Use to avoid reading immediately after modification.
	 * 
	 * @param delay time in milliseconds
	 * @return true if delay after write is complete
	 */
	public boolean canReadAfterModify(long delay) {
		return (System.currentTimeMillis() > (timeLastChange + delay));
	}

	/**
	 * Set modification time to now. Use if value affected indirect. To prevent
	 * unnecessary reading immediately after modification use canReadAfterModify().
	 */
	public void setModifiedNow() {
		timeLastChange = System.currentTimeMillis();
	}

	/**
	 * Test if value has changed.
	 *
	 * @return true if modification not complete
	 */
	public boolean hasChanged() {
		return changed;
	}

	/**
	 * Add change listener.
	 *
	 * @param vl listener
	 */
	public void addListener(DspVarListener vl) {
		changeListenesrs.add(vl);
	}

	/**
	 * Set key 2.
	 *
	 * @param val new key 2 value
	 */
	public void setKey2(int val) {
		externalKey2 = val;
	}

	/**
	 * Set additional embedded variable to read simultaneously. Use if some data
	 * items must be simultaneously read (single TCP/IP message) to prevent data
	 * interference by a multi client server.
	 * 
	 * @param dv variable to read simultaneously
	 */
	public void setSynchReadDspVar(ADspData dv) {
		synchReadDspValue = dv;
	}

	/**
	 * Enable optional request field (Must be supported by dsp variable!)
	 *
	 * @param b true to enable optional field
	 */
	public void enableReqOptionalField(boolean b) {
		bOptionEnable = b;
	}

	/**
	 * Set optional request field (Must be supported by dsp variable!)
	 *
	 * @param val value of request field
	 */
	public void setReqOptionalField(int val) {
		requestValue = val;
	}

	/**
	 * Get byte by index.
	 *
	 * @param ix byte number (0 first)
	 * @return one byte
	 */
	public byte getByte(int ix) {
		return this.data[ix];
	}

	/**
	 * Get all bytes.
	 *
	 * @return copy of internal byte array
	 */
	public synchronized byte[] getBytes() {
		byte r[] = new byte[data.length];
		System.arraycopy(data, 0, r, 0, r.length);
		return r;
	}

	synchronized boolean isRefreschRequired() {
		if (!readEnable)
			return false;
		if (refreshRequired) {
			return true;
		}
		if ((System.currentTimeMillis() > (refreshTime + refrechPeriod)) && (refrechPeriod > 0)) {
			return true;
		} else {
			return false;
		}
	}

	synchronized static int getNextEntry(byte[] b, int ix, int b_size) {
		int r;
		try {
			int len = (int) ((b[ix] & 0xFF) | ((b[ix + 1] & 0xFF) << 8)) + 4;
			if (ix + len > b_size) {
				r = -2;
			} else {
				r = len;
			}
		} catch (Exception e) {
			r = -1;
		}
		return r;
	}

	synchronized void readBytesFromBuffer(byte[] b, int ix, int b_size) {
		if (refrechPeriod == 0 && refreshRequired == false) {
			return;
		}
		try {
			int len = (int) ((b[ix] & 0xFF) | ((b[ix + 1] & 0xFF) << 8));
			int hc = (int) ((b[ix + 2] & 0xFF) | ((b[ix + 3] & 0xFF) << 8));
			if (hc != this.hashCode || len != data.length) {
				return;
			}
			System.arraycopy(b, ix + 4, data, 0, data.length);
			refreshRequired = false;
			refreshTime = System.currentTimeMillis();
			callAllListeners();
		} catch (Exception e) {
		}
	}

	synchronized int writeBytes2Buffer(byte[] b, int ix) {
		int r;
		try {
			b[ix] = (byte) (data_set.length & 0xFF);
			b[ix + 1] = (byte) ((data_set.length >> 8) & 0xFF);
			b[ix + 2] = (byte) (hashCode & 0xFF);
			b[ix + 3] = (byte) ((hashCode >> 8) & 0xFF);
			System.arraycopy(data_set, 0, b, (ix + 4), data_set.length);
			r = 4 + data_set.length;
		} catch (Exception e) {
			r = -1;
		}
		return r;
	}

	synchronized int writeHashCode2Buffer(byte[] b, int ix) {
		int r = 0;
		try {
			b[ix] = (byte) (hashCode & 0xFF);
			b[ix + 1] = (byte) ((hashCode >> 8) & 0xFF);
			r += 2;
			if (bOptionEnable) {
				b[ix + 2] = (byte) (requestValue & 0xFF);
				b[ix + 3] = (byte) ((requestValue >> 8) & 0xFF);
				r += 2;
			}
			if (synchReadDspValue != null) {
				b[ix + 4] = (byte) (synchReadDspValue.hashCode & 0xFF);
				b[ix + 5] = (byte) ((synchReadDspValue.hashCode >> 8) & 0xFF);
				r += 2;
			}
		} catch (Exception e) {
			r = -1;
		}
		return r;
	}

	void callAllListeners() {
		for (DspVarListener e : changeListenesrs) {
			if (e != null) {
				e.onChange();
			}
		}
	}

	private int getHashCode(String vName) throws NoSuchAlgorithmException {
		return (vName.hashCode() & 0xFFFF);
	}

	synchronized void setChanged() {
		changed = writeEnable;
	}

	synchronized void clearChanged() {
		changed = false;
	}

	int getByteArrayLen() {
		return data.length;
	}
}
//CHECKSTYLE:ON
