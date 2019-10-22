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
import com.dalsemi.onewire.adapter.OneWireIOException;

public class PDKAdapterUSB extends DSPortAdapter {

	private int port_handle = -1;
	private String port_name = "";

	private Object syncObj = new Object();
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
		inExclusive = false;
	}

	private native int OpenPort(String port);

	public boolean selectPort(String portName) throws OneWireIOException, OneWireException {
		try {
			int portnumber = Integer.parseInt(portName.substring(3));
			port_handle = OpenPort("DS2490-" + portnumber);
			port_name = portName;
			inExclusive = false;
			return port_handle != -1;
		} catch (Exception e) {
			throw new OneWireException("Bad Portnumber: " + portName);
		}
	}

	private native void ClosePort(int port);

	public void freePort() throws OneWireException {
		if (port_handle != -1) {
			ClosePort(port_handle);
			port_handle = -1;
			port_name = "";
			inExclusive = false;
		}
	}

	public String getAdapterName() {
		return "DS9490";
	}

	public String getPortTypeDescription() {
		return "USB Adapter with libUSB and PDK API";
	}

	public String getClassVersion() {
		return "USB-Beta";
	}

	public Enumeration<String> getPortNames() {
		java.util.Vector<String> v = new java.util.Vector<String>();
		for (int i = 1; i < 15; i++)
			v.addElement("USB" + i);
		return v.elements();
	}

	public String getPortName() throws OneWireException {
		return port_name;
	}

	private native boolean AdapterDetected(int portHandle);

	public boolean adapterDetected() throws OneWireIOException, OneWireException {
		if (port_handle != -1) {
			return AdapterDetected(port_handle);
		} else
			throw new OneWireException("Port not selected");
	}

	private native void GetAddress(int portHandle, byte[] address);

	public void getAddress(byte[] address) {
		if (port_handle != -1)
			GetAddress(port_handle, address);
	}

	private native int Search(int portHandle, boolean find_first, boolean do_reset, boolean alarm_only);

	public boolean findFirstDevice() throws OneWireIOException, OneWireException {
		if (port_handle != -1) {
			int ret = Search(port_handle, true, !searchNoReset, searchAlarmOnly);
			if (ret == -1)
				throw new OneWireException("Adapter communication error during search");
			return ret == 1 ? true : false;
		} else
			throw new OneWireException("Port not selected");
	}

	public boolean findNextDevice() throws OneWireIOException, OneWireException {
		if (port_handle != -1) {
			int ret = Search(port_handle, false, !searchNoReset, searchAlarmOnly);
			if (ret == -1)
				throw new OneWireException("Adapter communication error during search");
			return ret == 1 ? true : false;
		} else
			throw new OneWireException("Port not selected");
	}

	private boolean searchAlarmOnly = false;
	private boolean searchNoReset = false;

	public void setSearchOnlyAlarmingDevices() {
		searchAlarmOnly = true;
	}

	public void setNoResetSearch() {
		searchNoReset = true;
	}

	public void setSearchAllDevices() {
		searchAlarmOnly = false;
		searchNoReset = false;
	}

	public boolean beginExclusive(boolean blocking) throws OneWireException {
		boolean gotExclusive = false;
		while (!gotExclusive && blocking) {
			synchronized (syncObj) {
				if (!inExclusive) {
					inExclusive = true;
					gotExclusive = true;
				}
			}
		}
		return false;
	}

	public void endExclusive() {
		inExclusive = false;
	}

	private native int TouchBit(int portHandle, int dataBit);

	private native int TouchBitPower(int portHandle, int dataBit);

	public void putBit(boolean dataBit) throws OneWireIOException, OneWireException {
		if (port_handle != -1) {
			int ret;
			if (levelChangeOnNextBit && primedLevelValue == LEVEL_POWER_DELIVERY)
				ret = TouchBitPower(port_handle, dataBit ? 1 : 0);
			else
				ret = TouchBit(port_handle, dataBit ? 1 : 0);
			levelChangeOnNextBit = false;

			if (ret == -1)
				throw new OneWireIOException("1-Wire Adapter Communication Failed During Touchbit");
			else if (ret != (dataBit ? 1 : 0))
				throw new OneWireIOException("PutBit failed, echo did not match");
		} else
			throw new OneWireException("Port not selected");
	}

	public boolean getBit() throws OneWireIOException, OneWireException {
		if (port_handle != -1) {
			int ret;
			if (levelChangeOnNextBit && primedLevelValue == LEVEL_POWER_DELIVERY)
				ret = TouchBitPower(port_handle, 1);
			else
				ret = TouchBit(port_handle, 1);
			levelChangeOnNextBit = false;

			if (ret == 1)
				return true;
			else if (ret == 0)
				return false;
			else
				throw new OneWireIOException("1-Wire Adapter Communication Failed During Touchbit");
		} else
			throw new OneWireException("Port not selected");
	}

	private native int TouchByte(int portHandle, int dataByte);

	private native int TouchBytePower(int portHandle, int dataByte);

	public void putByte(int dataByte) throws OneWireIOException, OneWireException {
		if (port_handle != -1) {
			int ret;
			if (levelChangeOnNextByte && primedLevelValue == LEVEL_POWER_DELIVERY)
				ret = TouchBytePower(port_handle, dataByte);
			else
				ret = TouchByte(port_handle, dataByte);
			levelChangeOnNextByte = false;

// TODO non-functional
//			if (ret == -1)
//				throw new OneWireIOException("1-Wire Adapter Communication Failed During Touchbyte");
//			else if (ret != dataByte)
//				throw new OneWireIOException("PutByte failed, echo did not match");
		} else
			throw new OneWireException("Port not selected");
	}

	public int getByte() throws OneWireIOException, OneWireException {
		if (port_handle != -1) {
			int ret;
			if (levelChangeOnNextByte && primedLevelValue == LEVEL_POWER_DELIVERY)
				ret = TouchBytePower(port_handle, 0x0FF);
			else
				ret = TouchByte(port_handle, 0x0FF);
			levelChangeOnNextByte = false;

			if (ret == -1)
				throw new OneWireIOException("1-Wire Adapter Communication Failed During Touchbyte");
			return ret;
		} else
			throw new OneWireException("Port not selected");
	}

	public byte[] getBlock(int len) throws OneWireIOException, OneWireException {
		byte[] buff = new byte[len];
		getBlock(buff, 0, len);
		return buff;
	}

	public void getBlock(byte[] buff, int len) throws OneWireIOException, OneWireException {
		getBlock(buff, 0, len);
	}

	public void getBlock(byte[] buff, int off, int len) throws OneWireIOException, OneWireException {
		for (int i = 0; i < len; i++)
			buff[i + off] = (byte) 0xFF;
		dataBlock(buff, off, len);
	}

	private native int DataBlock(int portHandle, byte[] buff, int off, int len);

	public void dataBlock(byte[] buff, int off, int len) throws OneWireIOException, OneWireException {
		if (port_handle != -1) {
			int ret = DataBlock(port_handle, buff, off, len);
			if (ret == -1)
				throw new OneWireIOException("1-Wire Adapter Communication Failed During Reset");
		} else
			throw new OneWireException("Port not selected");
	}

	private native int Reset(int portHandle);

	public int reset() throws OneWireIOException, OneWireException {
		if (port_handle != -1) {
			int ret = Reset(port_handle);
			if (ret == -1)
				throw new OneWireIOException("1-Wire Adapter Communication Failed During Reset");
			return ret;
		} else
			throw new OneWireException("Port not selected");
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
	public void setPowerDuration(int timeFactor) throws OneWireIOException, OneWireException {
		if (timeFactor != DELIVERY_INFINITE)
			throw new OneWireException(
					"USerialAdapter-setPowerDuration, does not support this duration, infinite only");
		else
			levelTimeFactor = DELIVERY_INFINITE;
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
	public boolean startPowerDelivery(int changeCondition) throws OneWireIOException, OneWireException {
		if (changeCondition == CONDITION_AFTER_BIT) {
			levelChangeOnNextBit = true;
			primedLevelValue = LEVEL_POWER_DELIVERY;
			return true;
		} else if (changeCondition == CONDITION_AFTER_BYTE) {
			levelChangeOnNextByte = true;
			primedLevelValue = LEVEL_POWER_DELIVERY;
			return true;
		} else if (changeCondition == CONDITION_NOW) {
			int ret = PowerLevel(port_handle, LEVEL_POWER_DELIVERY);

			if (ret == 1)
				return true;
			else if (ret == 0)
				return false;
			else
				throw new OneWireIOException("1-Wire Adapter Communication Failed");
		} else
			throw new OneWireException("Invalid power delivery condition");
	}

	public native int PowerLevel(int portHandle, int newLevel);

	public void setPowerNormal() {
		levelChangeOnNextByte = false;
		levelChangeOnNextBit = false;
		primedLevelValue = LEVEL_NORMAL;
		PowerLevel(port_handle, LEVEL_NORMAL);
	}

}
