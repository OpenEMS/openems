// CHECKSTYLE:OFF
//---------------------------------------------------------------------------
// Copyright (C) 2005 Maxim Integrated Products, All Rights Reserved.
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL MAXIM INTEGRATED PRODUCTS BE LIABLE FOR ANY CLAIM, DAMAGES
// OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
// ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// OTHER DEALINGS IN THE SOFTWARE.
//
// Except as contained in this notice, the name of Maxim Integrated Products
// shall not be used except as stated in the Maxim Integrated Products
// Branding Policy.
//--------------------------------------------------------------------------
//
//  PDKAdapterUSB.java - Implements DSPortAdapter for PDK libUSB build
//  version 2.00

package com.dalsemi.onewire.adapter;

//imports
import java.util.Enumeration;

import com.dalsemi.onewire.OneWireException;

@SuppressWarnings({ "unused" })
public class PDKAdapterUSB extends DSPortAdapter {

	private int port_handle = -1;
	private String port_name = "";

	private final Object syncObj = new Object();
	private static Object staticSyncObj = new Object();
	private static boolean libLoaded = false;
	private boolean inExclusive = false;

	public PDKAdapterUSB() {
		synchronized (staticSyncObj) {
			if (!libLoaded) {
				System.loadLibrary("onewireUSB");
				libLoaded = true;
			}
		}
		this.inExclusive = false;
	}

	private native int OpenPort(String port);

	@Override
	public boolean selectPort(String portName) throws OneWireIOException, OneWireException {
		try {
			var portnumber = Integer.parseInt(portName.substring(3));
			this.port_handle = this.OpenPort("DS2490-" + portnumber);
			this.port_name = portName;
			this.inExclusive = false;
			return this.port_handle != -1;
		} catch (Exception e) {
			throw new OneWireException("Bad Portnumber: " + portName);
		}
	}

	private native void ClosePort(int port);

	@Override
	public void freePort() throws OneWireException {
		if (this.port_handle != -1) {
			this.ClosePort(this.port_handle);
			this.port_handle = -1;
			this.port_name = "";
			this.inExclusive = false;
		}
	}

	@Override
	public String getAdapterName() {
		return "DS9490";
	}

	@Override
	public String getPortTypeDescription() {
		return "USB Adapter with libUSB and PDK API";
	}

	@Override
	public String getClassVersion() {
		return "USB-Beta";
	}

	@Override
	public Enumeration<String> getPortNames() {
		var v = new java.util.Vector<String>();
		for (var i = 1; i < 15; i++) {
			v.addElement("USB" + i);
		}
		return v.elements();
	}

	@Override
	public String getPortName() throws OneWireException {
		return this.port_name;
	}

	private native boolean AdapterDetected(int portHandle);

	@Override
	public boolean adapterDetected() throws OneWireIOException, OneWireException {
		if (this.port_handle != -1) {
			return this.AdapterDetected(this.port_handle);
		}
		throw new OneWireException("Port not selected");
	}

	private native void GetAddress(int portHandle, byte[] address);

	@Override
	public void getAddress(byte[] address) {
		if (this.port_handle != -1) {
			this.GetAddress(this.port_handle, address);
		}
	}

	private native int Search(int portHandle, boolean find_first, boolean do_reset, boolean alarm_only);

	@Override
	public boolean findFirstDevice() throws OneWireIOException, OneWireException {
		if (this.port_handle == -1) {
			throw new OneWireException("Port not selected");
		}
		var ret = this.Search(this.port_handle, true, !this.searchNoReset, this.searchAlarmOnly);
		if (ret == -1) {
			throw new OneWireException("Adapter communication error during search");
		}
		return ret == 1 == true;
	}

	@Override
	public boolean findNextDevice() throws OneWireIOException, OneWireException {
		if (this.port_handle == -1) {
			throw new OneWireException("Port not selected");
		}
		var ret = this.Search(this.port_handle, false, !this.searchNoReset, this.searchAlarmOnly);
		if (ret == -1) {
			throw new OneWireException("Adapter communication error during search");
		}
		return ret == 1 == true;
	}

	private boolean searchAlarmOnly = false;
	private boolean searchNoReset = false;

	@Override
	public void setSearchOnlyAlarmingDevices() {
		this.searchAlarmOnly = true;
	}

	@Override
	public void setNoResetSearch() {
		this.searchNoReset = true;
	}

	@Override
	public void setSearchAllDevices() {
		this.searchAlarmOnly = false;
		this.searchNoReset = false;
	}

	@Override
	public boolean beginExclusive(boolean blocking) throws OneWireException {
		var gotExclusive = false;
		while (!gotExclusive && blocking) {
			synchronized (this.syncObj) {
				if (!this.inExclusive) {
					this.inExclusive = true;
					gotExclusive = true;
				}
			}
		}
		return false;
	}

	@Override
	public void endExclusive() {
		this.inExclusive = false;
	}

	private native int TouchBit(int portHandle, int dataBit);

	private native int TouchBitPower(int portHandle, int dataBit);

	@Override
	public void putBit(boolean dataBit) throws OneWireIOException, OneWireException {
		if (this.port_handle == -1) {
			throw new OneWireException("Port not selected");
		}
		int ret;
		if (this.levelChangeOnNextBit && this.primedLevelValue == LEVEL_POWER_DELIVERY) {
			ret = this.TouchBitPower(this.port_handle, dataBit ? 1 : 0);
		} else {
			ret = this.TouchBit(this.port_handle, dataBit ? 1 : 0);
		}
		this.levelChangeOnNextBit = false;

		if (ret == -1) {
			throw new OneWireIOException("1-Wire Adapter Communication Failed During Touchbit");
		} else if (ret != (dataBit ? 1 : 0)) {
			throw new OneWireIOException("PutBit failed, echo did not match");
		}
	}

	@Override
	public boolean getBit() throws OneWireIOException, OneWireException {
		if (this.port_handle == -1) {
			throw new OneWireException("Port not selected");
		}
		int ret;
		if (this.levelChangeOnNextBit && this.primedLevelValue == LEVEL_POWER_DELIVERY) {
			ret = this.TouchBitPower(this.port_handle, 1);
		} else {
			ret = this.TouchBit(this.port_handle, 1);
		}
		this.levelChangeOnNextBit = false;

		if (ret == 1) {
			return true;
		} else if (ret == 0) {
			return false;
		} else {
			throw new OneWireIOException("1-Wire Adapter Communication Failed During Touchbit");
		}
	}

	private native int TouchByte(int portHandle, int dataByte);

	private native int TouchBytePower(int portHandle, int dataByte);

	@Override
	public void putByte(int dataByte) throws OneWireIOException, OneWireException {
		if (this.port_handle == -1) {
			throw new OneWireException("Port not selected");
		}
		int ret;
		if (this.levelChangeOnNextByte && this.primedLevelValue == LEVEL_POWER_DELIVERY) {
			ret = this.TouchBytePower(this.port_handle, dataByte);
		} else {
			ret = this.TouchByte(this.port_handle, dataByte);
		}
		this.levelChangeOnNextByte = false;
	}

	@Override
	public int getByte() throws OneWireIOException, OneWireException {
		if (this.port_handle == -1) {
			throw new OneWireException("Port not selected");
		}
		int ret;
		if (this.levelChangeOnNextByte && this.primedLevelValue == LEVEL_POWER_DELIVERY) {
			ret = this.TouchBytePower(this.port_handle, 0x0FF);
		} else {
			ret = this.TouchByte(this.port_handle, 0x0FF);
		}
		this.levelChangeOnNextByte = false;

		if (ret == -1) {
			throw new OneWireIOException("1-Wire Adapter Communication Failed During Touchbyte");
		}
		return ret;
	}

	@Override
	public byte[] getBlock(int len) throws OneWireIOException, OneWireException {
		var buff = new byte[len];
		this.getBlock(buff, 0, len);
		return buff;
	}

	@Override
	public void getBlock(byte[] buff, int len) throws OneWireIOException, OneWireException {
		this.getBlock(buff, 0, len);
	}

	@Override
	public void getBlock(byte[] buff, int off, int len) throws OneWireIOException, OneWireException {
		for (var i = 0; i < len; i++) {
			buff[i + off] = (byte) 0xFF;
		}
		this.dataBlock(buff, off, len);
	}

	private native int DataBlock(int portHandle, byte[] buff, int off, int len);

	@Override
	public void dataBlock(byte[] buff, int off, int len) throws OneWireIOException, OneWireException {
		if (this.port_handle == -1) {
			throw new OneWireException("Port not selected");
		}
		var ret = this.DataBlock(this.port_handle, buff, off, len);
		if (ret == -1) {
			throw new OneWireIOException("1-Wire Adapter Communication Failed During Reset");
		}
	}

	private native int Reset(int portHandle);

	@Override
	public int reset() throws OneWireIOException, OneWireException {
		if (this.port_handle == -1) {
			throw new OneWireException("Port not selected");
		}
		var ret = this.Reset(this.port_handle);
		if (ret == -1) {
			throw new OneWireIOException("1-Wire Adapter Communication Failed During Reset");
		}
		return ret;
	}

	// --------
	// -------- Adapter features
	// --------

	/*
	 * The following interogative methods are provided so that client code can react
	 * selectively to underlying states without generating an exception.
	 */

	/**
	 * Returns whether adapter can physically support overdrive mode.
	 *
	 * @return <code>true</code> if this port adapter can do OverDrive,
	 *         <code>false</code> otherwise.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error with the adapter
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public boolean canOverdrive() throws OneWireIOException, OneWireException {
		return false;
	}

	/**
	 * Returns whether the adapter can physically support hyperdrive mode.
	 *
	 * @return <code>true</code> if this port adapter can do HyperDrive,
	 *         <code>false</code> otherwise.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error with the adapter
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public boolean canHyperdrive() throws OneWireIOException, OneWireException {
		return false;
	}

	/**
	 * Returns whether the adapter can physically support flex speed mode.
	 *
	 * @return <code>true</code> if this port adapter can do flex speed,
	 *         <code>false</code> otherwise.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error with the adapter
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public boolean canFlex() throws OneWireIOException, OneWireException {
		return false;
	}

	/**
	 * Returns whether adapter can physically support 12 volt power mode.
	 *
	 * @return <code>true</code> if this port adapter can do Program voltage,
	 *         <code>false</code> otherwise.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error with the adapter
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public boolean canProgram() throws OneWireIOException, OneWireException {
		return false;
	}

	/**
	 * Returns whether the adapter can physically support strong 5 volt power mode.
	 *
	 * @return <code>true</code> if this port adapter can do strong 5 volt mode,
	 *         <code>false</code> otherwise.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error with the adapter
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public boolean canDeliverPower() throws OneWireIOException, OneWireException {
		return true;
	}

	/**
	 * Returns whether the adapter can physically support "smart" strong 5 volt
	 * power mode. "smart" power delivery is the ability to deliver power until it
	 * is no longer needed. The current drop it detected and power delivery is
	 * stopped.
	 *
	 * @return <code>true</code> if this port adapter can do "smart" strong 5 volt
	 *         mode, <code>false</code> otherwise.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error with the adapter
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public boolean canDeliverSmartPower() throws OneWireIOException, OneWireException {

		// regardless of adapter, the class does not support it
		return false;
	}

	/**
	 * Returns whether adapter can physically support 0 volt 'break' mode.
	 *
	 * @return <code>true</code> if this port adapter can do break,
	 *         <code>false</code> otherwise.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error with the adapter
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public boolean canBreak() throws OneWireIOException, OneWireException {
		return false;
	}

	// --------
	// -------- OneWire power methods
	// --------

	/**
	 * Sets the duration to supply power to the 1-Wire Network. This method takes a
	 * time parameter that indicates the program pulse length when the method
	 * startPowerDelivery().
	 * <p>
	 *
	 * Note: to avoid getting an exception, use the canDeliverPower() and
	 * canDeliverSmartPower() method to check it's availability.
	 * <p>
	 *
	 * @param timeFactor
	 *                   <ul>
	 *                   <li>0 (DELIVERY_HALF_SECOND) provide power for 1/2 second.
	 *                   <li>1 (DELIVERY_ONE_SECOND) provide power for 1 second.
	 *                   <li>2 (DELIVERY_TWO_SECONDS) provide power for 2 seconds.
	 *                   <li>3 (DELIVERY_FOUR_SECONDS) provide power for 4 seconds.
	 *                   <li>4 (DELIVERY_SMART_DONE) provide power until the the
	 *                   device is no longer drawing significant power.
	 *                   <li>5 (DELIVERY_INFINITE) provide power until the
	 *                   setBusNormal() method is called.
	 *                   </ul>
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public void setPowerDuration(int timeFactor) throws OneWireIOException, OneWireException {
		if (timeFactor != DELIVERY_INFINITE) {
			throw new OneWireException(
					"USerialAdapter-setPowerDuration, does not support this duration, infinite only");
		}
		this.levelTimeFactor = DELIVERY_INFINITE;
	}

	int levelTimeFactor = DELIVERY_INFINITE;
	int primedLevelValue = LEVEL_NORMAL;
	boolean levelChangeOnNextByte = false;
	boolean levelChangeOnNextBit = false;

	/**
	 * Sets the 1-Wire Network voltage to supply power to an iButton device. This
	 * method takes a time parameter that indicates whether the power delivery
	 * should be done immediately, or after certain conditions have been met.
	 * <p>
	 *
	 * Note: to avoid getting an exception, use the canDeliverPower() and
	 * canDeliverSmartPower() method to check it's availability.
	 * <p>
	 *
	 * @param changeCondition
	 *                        <ul>
	 *                        <li>0 (CONDITION_NOW) operation should occur
	 *                        immediately.
	 *                        <li>1 (CONDITION_AFTER_BIT) operation should be
	 *                        pending execution immediately after the next bit is
	 *                        sent.
	 *                        <li>2 (CONDITION_AFTER_BYTE) operation should be
	 *                        pending execution immediately after next byte is sent.
	 *                        </ul>
	 *
	 * @return <code>true</code> if the voltage change was successful,
	 *         <code>false</code> otherwise.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public boolean startPowerDelivery(int changeCondition) throws OneWireIOException, OneWireException {
		switch (changeCondition) {
		case CONDITION_AFTER_BIT:
			this.levelChangeOnNextBit = true;
			this.primedLevelValue = LEVEL_POWER_DELIVERY;
			return true;
		case CONDITION_AFTER_BYTE:
			this.levelChangeOnNextByte = true;
			this.primedLevelValue = LEVEL_POWER_DELIVERY;
			return true;
		case CONDITION_NOW:
			var ret = this.PowerLevel(this.port_handle, LEVEL_POWER_DELIVERY);
			if (ret == 1) {
				return true;
			} else if (ret == 0) {
				return false;
			} else {
				throw new OneWireIOException("1-Wire Adapter Communication Failed");
			}
		default:
			throw new OneWireException("Invalid power delivery condition");
		}
	}

	public native int PowerLevel(int portHandle, int newLevel);

	@Override
	public void setPowerNormal() {
		this.levelChangeOnNextByte = false;
		this.levelChangeOnNextBit = false;
		this.primedLevelValue = LEVEL_NORMAL;
		this.PowerLevel(this.port_handle, LEVEL_NORMAL);
	}

}
// CHECKSTYLE:ON
