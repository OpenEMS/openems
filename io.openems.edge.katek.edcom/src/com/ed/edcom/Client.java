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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import com.ed.data.history.ADataItem;

/**
 * Client implementation.
 */
public final class Client implements Comparable<Client>, Runnable, Closeable {

	static int ServerPort = 9760; // port number
	static int MaxPackSize = 500; // max tcp-ip packet size
	static int MaxMBTelSize = 3000; // max modbus telegram size
	static int MaxPDUSize = 3000 - 9;
	static int PackDelayMs = 1000;

	public static final int PIC_REFRESH_PERIOD = 5000; // ms

	private Socket sc = null;
	private final byte[] out_buf;
	private final byte[] in_buf;
	private final InetAddress ipDevice;
	private final InetAddress ipHost;

	private final List<ADspData> reqVal;
	private List<DspBlob> pack;

	private DspBlob cdp;
	private final DspVar dev_serial_num;

	private boolean stop;
	private boolean connected;
	private boolean visible; // service flag mostly for hy-sys, it covers the reconnects of not active
								// clients
	private int transState;
	private int telSize, telIx;
	private int reqPackSize = MaxPackSize;
	private int startDelay;
	private String descriptor;
	private boolean bRecon;
	private int extStatus;
	private boolean flushRaw;
	private long lastPicRefreshTime = 0;
	private static final int MAX_DSP_RESPONCE_LEN = 1024;
	private int readVarIndex;
	private int writeVarIndex;
	private boolean threadRuning = true;
	private final Thread task;
	private DspVar pic_version;
	private int[] comVersion = { 0, 0 };

	/**
	 * Constructor
	 *
	 * @param devAddress  inverter IP address
	 * @param hostAddress local host IP address related to desired network interface
	 * @param startDelay  communication start delay in milliseconds
	 * @throws Exception wrong parameters or resources, etc.
	 */
	public Client(InetAddress devAddress, InetAddress hostAddress, int startDelay) throws Exception {
		if (Util.userId < 1) {
			throw new Exception("wrong parameters");
		}
		if (!(devAddress.isSiteLocalAddress() || devAddress.isAnyLocalAddress() || devAddress.isLinkLocalAddress()
				|| devAddress.isLoopbackAddress() || devAddress.isMulticastAddress())) {
			throw new Exception("public ip is not allowed");
		}
		this.ipDevice = devAddress;
		this.ipHost = hostAddress;
		this.startDelay = startDelay;
		in_buf = new byte[MaxMBTelSize];
		out_buf = new byte[MaxMBTelSize];
		msgBytes = new byte[MaxMBTelSize];
		reqVal = new ArrayList<>();
		pack = new ArrayList<>();
		dev_serial_num = new DspVar("dev_serial_num", DspVar.TYPE_UINT8, 20, null, 5000);
		pic_version = new DspVar("pic_version", DspVar.TYPE_UINT16, 0, null, 30000);
		task = new Thread(this, "edcom" + devAddress.getHostAddress());

	}

	/**
	 * Set user password
	 *
	 * @param key user password
	 */
	public void setUserPass(String key) {
		userKey = key.hashCode();
	}

	/**
	 * Set password as integer
	 *
	 * @param key user password
	 */
	public void setUserPass(int key) {
		userKey = key;
	}

	@Deprecated
	public void setUserKey(int key) {
		setUserPass(key);
	}

	@Deprecated
	public void setUserKey(String key) {
		setUserPass(key);
	}

	/**
	 * Get user password set by setUserKey
	 *
	 * @return user password
	 */
	public int getUserPass() {
		return userKey;
	}

	@Deprecated
	public int getUserKey() {
		return getUserPass();
	}

	/**
	 * Read current permission (up to package 7)
	 *
	 * @deprecated Only use this with inver versions older than 8.0.
	 *             <p>
	 *             Use
	 *
	 * @return -1 - not read (wait for server response) 0 - access denied 1 - no
	 *         password required 2 - password accepted 3 - energy depot
	 */
	@Deprecated
	public int getUserStatus() {
		int user = -1;
		if (userTypeRefreshTime > 0) {
			user = userType;
		}
		if (userPassErrorCnt < 3 && user == 0) {
			user = -1;
		}
		return user;
	}

	/**
	 * Get accessFeedb (from package 8)
	 *
	 * @return access feedback bitfield: Bit0 = Bootloader active; Bit1 = identKey
	 *         accepted; Bit2 = userKey accepted
	 */
	public byte getAccessFeedb() {
		if (Util.communication_ver8x) {
			if (isConnected() && (validPicMessageCnt == 0)) { // bootloader active?
				accessFeedb = 0b00000001;
			}
			return accessFeedb;
		} else {
			return 0;
		}
	}

	/**
	 * Check if ID is accepted by inverter
	 *
	 * @return true - ID accepted, false - ID not accepted.
	 */
	public boolean isIdAccepted() {

		return this.accessBitTest(1);
	}

	/**
	 * Check if user password is accepted by inverter
	 *
	 * @return true - ID accepted, false - ID not accepted.
	 */
	public boolean isPasswordAccepted() {
		return this.accessBitTest(2);
	}

	private boolean accessBitTest(int pos) {

		return (accessFeedb & (1 << pos)) != 0;
	}

	/**
	 * Get timestamp of last authentication
	 *
	 * @return Authentication timestamp
	 */
	public long getLastAuthentication() {
		return userTypeRefreshTime;
	}

	/**
	 * Start communication
	 */
	public void start() throws IllegalThreadStateException {
		task.start();
	}

	/**
	 * Close this client. Once a client has been closed, it is not available for
	 * further use. A new client needs to be created.
	 *
	 * @throws IOException according to interface definition
	 */
	@Override
	public void close() throws IOException {
		stop = true;
	}

	/**
	 * Get device descriptor
	 *
	 * @return ip address or device serial number
	 */
	public String getDescriptor() {
		return descriptor;
	}

	/**
	 * Connection status
	 *
	 * @return connection status
	 */
	public boolean isConnected() {
		return connected;
	}

	private void setConnected(boolean c) {
		connected = c;
		if (connected == false) {
			validPicMessageCnt = 0;
			accessFeedb = 0;
		}
	}

	/**
	 * Visibility status (hy-sys specific option)
	 *
	 * @return visibility status
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Restart current connection
	 */
	public void reconnect() {
		bRecon = true;
	}

	/**
	 * Add variable
	 *
	 * @param dv dsp variable to add
	 */
	public void addDspVar(ADspData dv) {
		reqVal.add(dv);
	}

	/**
	 * Add list of variables
	 *
	 * @param dvLst list of variables to add
	 */
	public void addDspVar(List<ADspData> dvLst) {
		for (ADspData dv : dvLst) {
			if (dv != null) {
				reqVal.add(dv);
			}
		}
	}

	/**
	 * Remove variable
	 *
	 * @param key remove all variables with selected key
	 */
	public void removeDspVarByKey(int key) {
		for (int i = 0; i < reqVal.size(); i++) {
			ADspData dv = (ADspData) reqVal.get(i);
			if (dv.externalKey1 == key) {
				reqVal.remove(i);
			}
		}
	}

	/**
	 * Remove variable
	 *
	 * @param d dsp variable to remove
	 */
	public void removeDspVar(ADspData d) {
		for (int i = 0; i < reqVal.size(); i++) {
			ADspData dv = (ADspData) reqVal.get(i);
			if (dv.equals(d)) {
				reqVal.remove(i);
			}
		}
	}

	/**
	 * Remove all registered variables
	 */
	public void removeAllDspVar() {
		reqVal.clear();
	}

	/**
	 * Activate client
	 */
	public void activate() {
		addDspVar(dev_serial_num);
		addDspVar(pic_version);
		dev_serial_num.refresh();
		pic_version.refresh();
	}

	/**
	 * Get status of registered dsp variables
	 *
	 * @return true if was changed
	 */
	public boolean isParamChanged() {
		for (ADspData dv : reqVal) {
			if (dv != null) {
				if (dv.hasChanged()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Set descriptor
	 *
	 * @param buf new descriptor
	 */
	public void setDesc(ByteBuffer buf) {
		dev_serial_num.setValue(buf);
	}

	/**
	 * toString implementation
	 *
	 * @return device name
	 */
	@Override
	public String toString() {
		String s, s_temp;
		if (descriptor != null) {
			s_temp = descriptor.trim();
			if (!s_temp.isEmpty()) {
				s = descriptor;
			} else {
				s = ipDevice.getHostAddress();
			}
		} else {
			s = ipDevice.getHostAddress();
		}
		return s;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj.getClass() != this.getClass()) {
			return false;
		}
		final Client other = (Client) obj;

		return this.toString().equals(other.toString());
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 47 * hash + Objects.hashCode(this.ipDevice);
		hash = 47 * hash + Objects.hashCode(this.descriptor);
		return hash;
	}

	/**
	 * Send BLOB
	 *
	 * @param blob to send
	 */
	public void sendBlob(DspBlob blob) {
		pack.add(blob);
	}

	/**
	 * Send list of BLOBs
	 *
	 * @param blobs to send
	 */
	public void sendBlobList(List<DspBlob> blobs) {
		pack = blobs;
	}

	/**
	 * Get BLOBS count
	 *
	 * @return blobs count
	 */
	public int getBlobsCnt() {
		return pack.size();
	}

	/**
	 * BLOB status
	 *
	 * @return '0' - wait for upload, '1' - upload started, '2' - all complete, '-1'
	 *         - upload error
	 */
	public int getBlobsState() {
		return extStatus;
	}

	/**
	 * Remove all BLOBs.
	 */
	public void removeAllBlobs() {
		flushRaw = true;
	}

	/**
	 * Run implementation.
	 */
	@Override
	public void run() {
		try {
			Thread.sleep(startDelay);
		} catch (Exception e) {
		}
		while (threadRuning) {
			try {
				client();
				if (stop) { // stop thread
					threadRuning = false;
					if (sc != null) {
						sc.close();
					}
					break;
				}
				Thread.sleep(1);
			} catch (Exception e) {
				hs_state = -1;
			}
		}
	}

	private OutputStream out;
	private InputStream in;
	private int hs_state = 0;
	private long timeLastPack = 0;
	private int reconnctCounter = 0;

	private void client() throws Exception {
		if (dev_serial_num.isValid()) {
			descriptor = dev_serial_num.getCString();
		}
		if (pic_version.isValid()) {
			comVersion[0] = pic_version.getByte(0) & 0xFF;
			comVersion[1] = pic_version.getByte(1) & 0xFF;
		}
		switch (hs_state) {
		default:
		case -1: // error
			setConnected(false);
			try {
				if (sc != null) {
					sc.close();
				}
				if (reconnctCounter > 3) {
					visible = false;
					Thread.sleep(10000);
				} else {
					Thread.sleep(1000);
				}
				hs_state = 0;
			} catch (Exception e) {
				hs_state = -1;
			}
			break;
		case 0: // get ip from board
			setConnected(false);
			try {
				if (ipDevice == null) {
					hs_state = -1;
				} else {
					hs_state = 1;
				}
			} catch (Exception e) {
				hs_state = -1;
			}
			break;
		case 1: // try to connect
			if (reconnctCounter < 100) {
				reconnctCounter++;
			}
			setConnected(false);
			try {
				if (sc != null) {
					sc.close();
				}
				sc = new Socket();
				sc.bind(new InetSocketAddress(ipHost, 0));
				sc.connect(new InetSocketAddress(ipDevice, ServerPort), 2000);
				sc.setTcpNoDelay(false);
				if (sc.isConnected()) {
					reconnctCounter = 0;
					hs_state = 2;
					bRecon = false;
					setConnected(true);
					visible = true;
					pack.clear();
					flushRaw = true;
					out = sc.getOutputStream();
					in = sc.getInputStream();
				} else {
					hs_state = -1;
				}
			} catch (Exception e) {
				hs_state = -1;
			}
			break;
		case 2: // read & write
			try {
				if (!sc.isConnected() || sc.isClosed() || !sc.isBound() || bRecon) {
					hs_state = -1;
					break;
				}
				if (reqVal.isEmpty() && pack.isEmpty()) {
					hs_state = -1;
					Thread.sleep(2000);
					break;
				} // nothing to send ?
					// Raw data telegrams
				if (flushRaw) { // clear old data
					pack.clear();
					cdp = null;
					flushRaw = false;
					transState = 0;
					Thread.sleep(10);
				}
				// Send
				switch (transState) {
				default:
				case 0: // create telegram
					cdp = null;
					telSize = createMessage();
					if (cdp != null) {
						extStatus = 1;
					}
					if (telSize > 0) {
						transState = 10;
					} else {
						break;
					}
				case 10:
					in.skip(in.available());
					mb_state = 0; // reset input protocol
					transState = 1;
					telIx = 0;
					reqPackSize = MaxPackSize;
				case 1: // send in blocks
					int ps = reqPackSize;
					int rest = telSize - telIx;
					if (rest < reqPackSize) {
						ps = rest;
					}
					out.write(out_buf, telIx, ps);
					out.flush();
					telIx += ps;
					transState = 2; // complete
					startDelay = 0;
					break;
				case 2: // delay, wait for antwort
					if (telIx >= telSize) {
						if (cdp != null) { // was raw data ?
							cdp.ts = System.currentTimeMillis(); // time stamp
							cdp.retryCount--;
						}
						timeLastPack = System.currentTimeMillis();
						transState = 3;
					} // complete ?
					else {
						transState = 1;
					}
					break;
				case 3: // wait for asc
					if (cdp != null) {
						if (cdp.acknowledge != null) {
							if (in.available() > 0) {
								int n = in.read(in_buf);
								if (n > 0) {
									if (processInput(in_buf, n)) {
										if (findKey(msgBytes, msgLen, cdp.acknowledge) > 0) {
											cdp.acknowledgeStatus = 1;
											transState = 0;
											if (pack.isEmpty()) {
												extStatus = 2;
											}
										}
									}
								}
							}
							if ((cdp.timeOut + timeLastPack) < System.currentTimeMillis()) {
								if (cdp.retryCount > 0) { // retry ?
									transState = 10;
								} else {
									transState = 0;
									extStatus = -1;
									cdp.acknowledgeStatus = -1;
									flushRaw = true;
								}
							} // timeout complete?
						} // need asc ?
						else {
							if ((cdp.timeOut + timeLastPack) < System.currentTimeMillis()) {
								transState = 0;
							} // timeout complete?
						}
					} else {
						if (in.available() > 0) { // read data
							int n = in.read(in_buf);
							if (n > 0) {
								if (processInput(in_buf, n)) {
									if (adu_unit_id == 0x30) {
										readPicMsg(msgLen);
										if (validPicMessageCnt < 3) {
											validPicMessageCnt++;
										}
									} // read pic message ?
									readFromBuf(msgBytes, msgLen);
									transState = 0;
								}
							}
						}
						if ((PackDelayMs + timeLastPack) < System.currentTimeMillis()) {
							transState = 0;
						} // timeout complete?
					}
					break;
				}
				hs_state = 2;
			} catch (Exception e) {
				hs_state = -1;
			}
			break;
		case 4:
			try {
				if (sc != null) {
					sc.close();
				}
				hs_state = 5;
			} catch (Exception e) {
				hs_state = -1;
			}
			break;
		case 5:
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				hs_state = -1;
			}
			break;
		}
	}

	private int mb_state = 0;
	private int msgLen = 0;
	private int pdu_index = 0;
	private int adu_unit_id = 0;
	private byte[] msgBytes;

	// Parser
	private boolean processInput(byte d[], int size) {
		boolean complete = false;
		// regular protocol
		for (int i = 0; i < size && complete == false; i++) {
			switch (mb_state) {
			default:
			case 0: // Header
				pdu_index = 0;
				if (d[i] == (byte) 0xED) {
					mb_state = 1;
					msgBytes[pdu_index++] = d[i];
				}
				break;
			case 1:
				if (d[i] == (byte) 0xDE) {
					mb_state = 2;
					msgBytes[pdu_index++] = d[i];
				} else {
					mb_state = -1;
				}
				break;
			case 2: // Unit ID
				adu_unit_id = d[i];
				msgBytes[pdu_index++] = d[i];
				if (adu_unit_id >= 0x30 && adu_unit_id <= 0x40) {
					mb_state = 3;
				} else {
					mb_state = -1;
				}
				break;
			case 3: // Length
				msgBytes[pdu_index++] = d[i];
				mb_state = 4;
				break;
			case 4:
				msgBytes[pdu_index++] = d[i];
				msgLen = (ByteBuffer.wrap(msgBytes, 3, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF) + 9;
				mb_state = 5;
				if (msgLen < 9 || msgLen > msgBytes.length) {
					mb_state = -1;
				}
				break;
			case 5: // CS32 and Data
				msgBytes[pdu_index++] = d[i];
				if (pdu_index == msgLen) {
					mb_state = 0;
					complete = true;
				} // complete ?
				break;
			}
		}
		return complete;
	}

	private int findKey(byte[] pdu, int pdu_size, byte key[]) {
		long cs32 = 0, cs32_ext = 0;
		int r = 0, j = 0;
		// CS32
		for (int i = 9; i < pdu_size; i++) {
			cs32 = (cs32 + (pdu[i] & 0xFF)) & 0xFFFFFFFF;
		}
		cs32_ext = (ByteBuffer.wrap(pdu, 5, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFF);
		if (cs32 != cs32_ext) {
			return r;
		}
		for (int pdu_ix = 9; pdu_ix < pdu_size; pdu_ix++) {
			if (key[j] == pdu[pdu_ix]) {
				if (++j == key.length) {
					break;
				}
			} else {
				j = 0;
			}
		}
		if (j == key.length) {
			r = 1;
		}
		return r;
	}

	private int readFromBuf(byte[] pdu, int pdu_size) {
		int el_size;
		long cs32 = 0, cs32_ext = 0;
		// CS32
		for (int i = 9; i < pdu_size; i++) {
			cs32 = (cs32 + (pdu[i] & 0xFF)) & 0xFFFFFFFF;
		}
		cs32_ext = (ByteBuffer.wrap(pdu, 5, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFF);
		if (cs32 != cs32_ext) {
			return 0;
		}
		for (int pdu_ix = 9; pdu_ix < pdu_size;) {
			el_size = ADspData.getNextEntry(pdu, pdu_ix, pdu_size); // get next element
			if (el_size > 0) {
				for (ADspData dv : reqVal) { // for all entrys
					if (dv != null) {
						dv.readBytesFromBuffer(pdu, pdu_ix, pdu_size);
					}
				}
				pdu_ix += el_size;
			} else {
				if (el_size == -1) {
					pdu_ix++; // type is not correct, search for correct entry
				}
				if (el_size == -2) {
					break; // end off file
				}
			}
		}
		return 1;
	}

	boolean isPicRefreshRequired() {
		boolean b = false;
		long ctime = System.currentTimeMillis();
		if ((lastPicRefreshTime + PIC_REFRESH_PERIOD) < ctime) {
			b = true;
			lastPicRefreshTime = ctime;
		}
		return b;
	}

	private int putParams(byte b[]) {
		int cnt = 0;
		int ix = 9, n = 0;
		if (writeVarIndex + 1 >= reqVal.size()) {
			writeVarIndex = 0;
		}
		for (int i = writeVarIndex; i < reqVal.size() && cnt < 40; i++) { // for all entries
			ADspData dv = (ADspData) reqVal.get(i);
			if (dv != null) {
				n = 0;
				if (dv.hasChanged()) {
					n = dv.writeBytes2Buffer(b, ix);
					if (n >= 0) {
						dv.clearChanged();
					}
				}
				if (n >= 0) {
					writeVarIndex = i;
					ix += n;
					cnt++;
				} else {
					break; // end off buffer
				}
			}
		}
		return ix;
	}

	private int putIds(byte b[]) {
		int cnt = 0;
		int responceSize = 9;
		int ix = 9, n = 0, i = 0;
		i = 0;
		while (i < reqVal.size() && cnt < 40) { // for all entries
			if (readVarIndex >= reqVal.size()) {
				readVarIndex = 0;
			}
			ADspData dv = reqVal.get(readVarIndex);
			if (dv != null) {
				if (dv.isRefreschRequired()) {
					if ((responceSize + (dv.getByteArrayLen() + 4)) < MAX_DSP_RESPONCE_LEN) {
						n = dv.writeHashCode2Buffer(b, ix);
						if (n > 0) {
							responceSize += dv.getByteArrayLen() + 4;
							ix += n;
							readVarIndex++;
							cnt++;
						}
					} else {
						break;
					}
				} else {
					readVarIndex++;
				}
			} else {
				readVarIndex++;
			}
			i++;
		}
		return ix;
	}

	private long lastMsgTime = 0;
	private int lastMsgType = 0;
	private long tsPack = 0;

	// Create output message
	private int createMessage() {
		int tel_type = 0, tmp;
		long s_tmp = 0;
		int i, tel_len = 0;
		// row data ?
		long ctime = System.currentTimeMillis();
		if (ctime < lastMsgTime + 50) {
			return 0;
		}
		if (pack.size() > 0 && (Util.userId >= 1)) { // blob message
			cdp = (DspBlob) pack.remove(0);
			System.arraycopy(cdp.bytes, 0, out_buf, 9, cdp.bytes.length);
			tel_len = cdp.bytes.length + 9;
			out_buf[2] = cdp.getType(); // Unit ID
			tsPack = System.currentTimeMillis();
		} else { // regular message
			if (ctime < tsPack + 1000) {
				return 0;
			}
			// select message type
			if (validPicMessageCnt < 3) {
				tel_type = 2;
			} else {
				if (isParamChanged() && (lastMsgType != 1) && (Util.userId >= 2)) {
					tel_type = 1;
				} // Send request or changed parameters
				else {
					tel_type = 0;
					if (isPicRefreshRequired()) {
						tel_type = 2;
					} else {
						tel_type = 0;
					}
				}
			}
			switch (tel_type) {
			default:
			case 0: // dsp data request
				out_buf[2] = (byte) 0x34;
				tel_len = putIds(out_buf);
				break;
			case 1: // dsp parameters senden
				out_buf[2] = (byte) 0x33;
				tel_len = putParams(out_buf);
				break;
			case 2: // pic dara request
				out_buf[2] = (byte) 0x30;
				tel_len = putPicMsg(out_buf);
				break;
			}
			lastMsgType = tel_type;
		}
		if (tel_len < 10) {
			return 0; // nothing to send
		}
		tmp = tel_len - 9;
		for (i = 9; i < tel_len; i++) {
			s_tmp = ((long) s_tmp + (long) (out_buf[i] & 0xFF)) & 0xFFFFFFFF;
		}
		out_buf[0] = (byte) 0x55; // MBAP Header
		out_buf[1] = (byte) 0xAA;
		out_buf[3] = (byte) (tmp & 0xFF);
		out_buf[4] = (byte) ((tmp >> 8) & 0xFF); // Message Length
		out_buf[5] = (byte) (s_tmp & 0xFF);
		out_buf[6] = (byte) ((s_tmp >> 8) & 0xFF);
		out_buf[7] = (byte) ((s_tmp >> 16) & 0xFF);
		out_buf[8] = (byte) ((s_tmp >> 24) & 0xFF);
		lastMsgTime = ctime;
		return tel_len;
	}

	private int userKey = 0;

	private int picHighVersion, picLowVersion;
	private long picVersionRefreshTime;

	private byte mac[] = new byte[6];

	private byte serialNum[] = new byte[20];

	private byte picRandomKey[] = new byte[6];
	private long picRandomKeyRefreshTime;

	private byte userType;
	private long userTypeRefreshTime;
	private int userPassErrorCnt;
	private byte accessFeedb = 0;
	private long accessFeedbRefreshTime;

	private int validPicMessageCnt;

	private Random rnd = new Random(System.currentTimeMillis());
	private byte dummyBytes[] = new byte[10];

	private void readPicMsg(int len) {
		// Pic software version
		if (len >= 15) {
			picHighVersion = (msgBytes[13] & 0xFF);
			picLowVersion = (msgBytes[14] & 0xFF);
			picVersionRefreshTime = System.currentTimeMillis();
			if (picHighVersion < 2 && len < 62) {
				userType = 1;
				userTypeRefreshTime = System.currentTimeMillis();
				userPassErrorCnt = 0;
			} // compatibility (older as v2.x): no key required at old pic version
		}
		// PIC MAC Address
		if (len >= 25) {
			mac[0] = msgBytes[19];
			mac[1] = msgBytes[20];
			mac[2] = msgBytes[21];
			mac[3] = msgBytes[22];
			mac[4] = msgBytes[23];
			mac[5] = msgBytes[24];
		}
		// Serial number
		if (len >= 55) {
			serialNum[0] = msgBytes[35];
			serialNum[1] = msgBytes[36];
			serialNum[2] = msgBytes[37];
			serialNum[3] = msgBytes[38];
			serialNum[4] = msgBytes[39];
			serialNum[5] = msgBytes[40];
			serialNum[6] = msgBytes[41];
			serialNum[7] = msgBytes[42];
			serialNum[8] = msgBytes[43];
			serialNum[9] = msgBytes[44];
			serialNum[10] = msgBytes[45];
			serialNum[11] = msgBytes[46];
			serialNum[12] = msgBytes[47];
			serialNum[13] = msgBytes[48];
			serialNum[14] = msgBytes[49];
			serialNum[15] = msgBytes[50];
			serialNum[16] = msgBytes[51];
			serialNum[17] = msgBytes[52];
			serialNum[18] = msgBytes[53];
			serialNum[19] = msgBytes[54];
		}
		// Random key
		if (len >= 61) {
			picRandomKey[0] = msgBytes[55];
			picRandomKey[1] = msgBytes[56];
			picRandomKey[2] = msgBytes[57];
			picRandomKey[3] = msgBytes[58];
			picRandomKey[4] = msgBytes[59];
			picRandomKey[5] = msgBytes[60];
			picRandomKeyRefreshTime = System.currentTimeMillis();
		}
		// Current user
		if (len >= 62) {
			if (Util.communication_ver8x) { // EDCOM with identKey (8.x)
				if (picHighVersion >= 8 || picHighVersion == 0) { // COM version >=8.x
					accessFeedb = msgBytes[61];
					accessFeedbRefreshTime = System.currentTimeMillis();
					clientId[0] = clientId[1] = clientId[2] = clientId[3] = 0;
					clientIdRefreshTime = userTypeRefreshTime = 0;
				} else { // COM version old (<8.x)
					userType = msgBytes[61];

					userTypeRefreshTime = System.currentTimeMillis();
					if (userType == 0) {
						userPassErrorCnt++;
					} else {
						userPassErrorCnt = 0;
					}
					accessFeedb = 0;
					accessFeedbRefreshTime = 0;
				}
			} else { // EDCOM with old initialization (<8.x with "EDCOM keys")
				accessFeedb = 0;
				accessFeedbRefreshTime = 0;
				if (picHighVersion >= 8) // COM version >=8.x
				{
					userType = 0; // no permission, also version check of hy-sys does not allow usage
				} else {
					userType = msgBytes[61];
					userTypeRefreshTime = System.currentTimeMillis();
					if (userType == 0) {
						userPassErrorCnt++;
					} else {
						userPassErrorCnt = 0;
					}
					// Active connections - clients IDs
					if (len >= 66 && Util.readPermission == 5) {
						try {
							clientId[0] = ADataItem.getU8Value(msgBytes, 62);
							clientId[1] = ADataItem.getU8Value(msgBytes, 63);
							clientId[2] = ADataItem.getU8Value(msgBytes, 64);
							clientId[3] = ADataItem.getU8Value(msgBytes, 65);
							clientIdRefreshTime = System.currentTimeMillis();
						} catch (Exception e) {
						}
					} else {
						Arrays.fill(clientId, 0, clientId.length, 0);
						clientIdRefreshTime = 0;
					}
				}
			}
		}
	}

	private int clientId[] = new int[4];
	private long clientIdRefreshTime;

	/**
	 * Get connected clients
	 *
	 * @param ix client index [0..3]
	 * @return client id
	 */
	public int getClientId(int ix) throws RuntimeException {
		if (ix >= clientId.length) {
			throw new RuntimeException("P10");
		}
		return clientId[ix];
	}

	/**
	 * Get connected clients list refresh time
	 *
	 * @return time [ms]
	 */
	public long getClientIdListRefreshTime() {
		if (Util.userId > 1) {
			return clientIdRefreshTime;
		}
		return 0;
	}

	private int putPicMsg(byte[] dest) {
		if (Util.communication_ver8x) {
			rnd.nextBytes(dummyBytes);
			System.arraycopy(dummyBytes, 0, dest, 9, 10);
			if (picRandomKeyRefreshTime > 0) {

				Util.feedback.getData(mac, 0, 6, picRandomKey, dest, 9);
				// put user key
				dest[15] = (byte) (userKey & 0xFF);
				dest[16] = (byte) ((userKey >> 8) & 0xFF);
				dest[17] = (byte) ((userKey >> 16) & 0xFF);
				dest[18] = (byte) ((userKey >> 24) & 0xFF);
				getData(dest, 15, 4, picRandomKey, dest, 15, 2);
				byte[] encIdentKey = Util.feedback.updateIdentKey(picRandomKey);
				System.arraycopy(encIdentKey, 0, dest, 20, 8);

			}
			return 28;
		} else {
			rnd.nextBytes(dummyBytes);
			System.arraycopy(dummyBytes, 0, dest, 9, 10);
			if (picRandomKeyRefreshTime > 0) {
				if (Util.feedback != null && Util.g == 1980 && Util.userId >= 1) {
					Util.feedback.getData(mac, 0, 6, picRandomKey, dest, 9);
				}
				// put user key
				dest[15] = (byte) (userKey & 0xFF);
				dest[16] = (byte) ((userKey >> 8) & 0xFF);
				dest[17] = (byte) ((userKey >> 16) & 0xFF);
				dest[18] = (byte) ((userKey >> 24) & 0xFF);
				getData(dest, 15, 4, picRandomKey, dest, 15, 2);
				// client id index
				dest[19] = (byte) (Util.userId & 0xFF);
			}
			return 20;
		}
	}

	private int getData(byte[] src, int spos, int len, byte[] key, byte[] dest, int dpos, int k) {
		byte[] tmp = new byte[len];
		System.arraycopy(src, spos, tmp, 0, len);
		// apply key
		for (int i = 0; i < tmp.length && i < key.length; i++) {
			tmp[i] += key[i];
		}
		// simple mix
		for (int i = 0; i < k; i++) {
			tmp[i % len] += 1;
			tmp[i % len] += tmp[(i + 10) % len];
			tmp[(i + 3) % len] *= tmp[(i + 11) % len];
			tmp[i % len] += tmp[(i + 7) % len];
		}
		System.arraycopy(tmp, 0, dest, dpos, len);
		return 1;
	}

	public byte[] getPicVersion() {
		byte v[] = { 0, 0 };
		if (picVersionRefreshTime > 0) {
			v[0] = (byte) picLowVersion;
			v[1] = (byte) picHighVersion;
		} else {
			v[0] = v[1] = -1;
		}
		return v;
	}

	public byte[] getRandomKey() {
		return this.picRandomKey;
	}

	public long getAccessFeedbRefreshTime() {
		return this.accessFeedbRefreshTime;
	}

	public int[] getComVersion() {
		return this.comVersion;
	}

	@Override
	public int compareTo(Client o) {
		return this.toString().compareTo(o.toString());
	}
}
//CHECKSTYLE:ON
