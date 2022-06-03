// CHECKSTYLE:OFF

/*---------------------------------------------------------------------------
 * Copyright (C) 1999,2000 Maxim Integrated Products, All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL MAXIM INTEGRATED PRODUCTS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Maxim Integrated Products
 * shall not be used except as stated in the Maxim Integrated Products
 * Branding Policy.
 *---------------------------------------------------------------------------
 */

package com.dalsemi.onewire.adapter;

// imports
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.utils.Address;

/**
 * <p>
 * This <code>DSPortAdapter</code> class was designed to be used for the
 * iB-IDE's emulator. The <code>DumbAdapter</code> allows programmers to add and
 * remove <code>OneWireContainer</code> objects that will be found in its
 * search. The Java iButton emulator works by creating a class that subclasses
 * all of <code>OneWireContainer16</code>'s relevant methods and redirecting
 * them to the emulation code. That object is then added to this class's list of
 * <code>OneWireContainer</code>s.
 * </p>
 *
 * <p>
 * Note that methods such as <code>selectPort</code> and
 * <code>beginExclusive</code> by default do nothing. This class is mainly meant
 * for debugging using an emulated iButton. It will do a poor job of debugging
 * any multi-threading, port-sharing issues.
 *
 * @see com.dalsemi.onewire.adapter.DSPortAdapter
 * @see com.dalsemi.onewire.container.OneWireContainer
 *
 * @version 0.00, 16 Mar 2001
 * @author K
 */
@SuppressWarnings({ "unused" })
public class DumbAdapter extends DSPortAdapter {
	// --------
	// -------- Variables
	// --------

	int containers_index = 0;

	private final Vector<OneWireContainer> containers = new Vector<>();

	/**
	 * Adds a <code>OneWireContainer</code> to the list of containers that this
	 * adapter object will find.
	 *
	 * @param c represents a 1-Wire device that this adapter will report from a
	 *          search
	 */
	public void addContainer(OneWireContainer c) {
		synchronized (this.containers) {
			this.containers.addElement(c);
		}
	}

	/**
	 * Removes a <code>OneWireContainer</code> from the list of containers that this
	 * adapter object will find.
	 *
	 * @param c represents a 1-Wire device that this adapter should no longer report
	 *          as found by a search
	 */
	public void removeContainer(OneWireContainer c) {
		synchronized (this.containers) {
			this.containers.removeElement(c);
		}
	}

	/**
	 * Hashtable to contain the user replaced OneWireContainers
	 */
	private final Hashtable<Integer, OneWireContainer> registeredOneWireContainerClasses = new Hashtable<>(5);

	/**
	 * Byte array of families to include in search
	 */
	private byte[] include;

	/**
	 * Byte array of families to exclude from search
	 */
	private byte[] exclude;

	// --------
	// -------- Methods
	// --------

	/**
	 * Retrieves the name of the port adapter as a string. The 'Adapter' is a device
	 * that connects to a 'port' that allows one to communicate with an iButton or
	 * other 1-Wire device. As example of this is 'DS9097U'.
	 *
	 * @return <code>String</code> representation of the port adapter.
	 */
	@Override
	public String getAdapterName() {
		return "DumbAdapter";
	}

	/**
	 * Retrieves a description of the port required by this port adapter. An example
	 * of a 'Port' would 'serial communication port'.
	 *
	 * @return <code>String</code> description of the port type required.
	 */
	@Override
	public String getPortTypeDescription() {
		return "Virtual Emulated Port";
	}

	/**
	 * Retrieves a version string for this class.
	 *
	 * @return version string
	 */
	@Override
	public String getClassVersion() {
		return "0.00";
	}

	// --------
	// -------- Port Selection
	// --------

	/**
	 * Retrieves a list of the platform appropriate port names for this adapter. A
	 * port must be selected with the method 'selectPort' before any other
	 * communication methods can be used. Using a communication method before
	 * 'selectPort' will result in a <code>OneWireException</code> exception.
	 *
	 * @return <code>Enumeration</code> of type <code>String</code> that contains
	 *         the port names
	 */
	@Override
	public Enumeration<String> getPortNames() {
		var portNames = new Vector<String>();
		portNames.addElement("NULL0");
		return portNames.elements();
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 */
	@Override
	public void registerOneWireContainerClass(int family, Class<?> OneWireContainerClass) {
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @param portName name of the target port, retrieved from getPortNames()
	 *
	 * @return always returns <code>true</code>
	 */
	@Override
	public boolean selectPort(String portName) {
		// be lazy, allow anything
		return true;
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 */
	@Override
	public void freePort() {
		// airball
	}

	/**
	 * Retrieves the name of the selected port as a <code>String</code>.
	 *
	 * @return always returns the <code>String</code> "NULL0"
	 */
	@Override
	public String getPortName() {
		return "NULL0";
	}

	// --------
	// -------- Adapter detection
	// --------

	/**
	 * Detects adapter presence on the selected port. In <code>DumbAdapter</code>,
	 * the adapter is always detected.
	 *
	 * @return <code>true</code>
	 */
	@Override
	public boolean adapterDetected() {
		return true;
	}

	// --------
	// -------- Adapter features
	// --------

	/*
	 * The following interogative methods are provided so that client code can react
	 * selectively to underlying states without generating an exception.
	 */

	/**
	 * Applications might check this method and not attempt operation unless this
	 * method returns <code>true</code>. To make sure that a wide variety of
	 * applications can use this class, this method always returns
	 * <code>true</code>.
	 *
	 * @return <code>true</code>
	 *
	 */
	@Override
	public boolean canOverdrive() {
		// don't want someone to bail because of this
		return true;
	}

	/**
	 * Applications might check this method and not attempt operation unless this
	 * method returns <code>true</code>. To make sure that a wide variety of
	 * applications can use this class, this method always returns
	 * <code>true</code>.
	 *
	 * @return <code>true</code>
	 */
	@Override
	public boolean canHyperdrive() {
		// don't want someone to bail because of this, although it doesn't exist yet
		return true;
	}

	/**
	 * Applications might check this method and not attempt operation unless this
	 * method returns <code>true</code>. To make sure that a wide variety of
	 * applications can use this class, this method always returns
	 * <code>true</code>.
	 *
	 * @return <code>true</code>
	 */
	@Override
	public boolean canFlex() {
		// don't want someone to bail because of this
		return true;
	}

	/**
	 * Applications might check this method and not attempt operation unless this
	 * method returns <code>true</code>. To make sure that a wide variety of
	 * applications can use this class, this method always returns
	 * <code>true</code>.
	 *
	 * @return <code>true</code>
	 */
	@Override
	public boolean canProgram() {
		// don't want someone to bail because of this
		return true;
	}

	/**
	 * Applications might check this method and not attempt operation unless this
	 * method returns <code>true</code>. To make sure that a wide variety of
	 * applications can use this class, this method always returns
	 * <code>true</code>.
	 *
	 * @return <code>true</code>
	 */
	@Override
	public boolean canDeliverPower() {
		// don't want someone to bail because of this
		return true;
	}

	/**
	 * Applications might check this method and not attempt operation unless this
	 * method returns <code>true</code>. To make sure that a wide variety of
	 * applications can use this class, this method always returns
	 * <code>true</code>.
	 *
	 * @return <code>true</code>
	 */
	@Override
	public boolean canDeliverSmartPower() {
		// don't want someone to bail because of this
		return true;
	}

	/**
	 * Applications might check this method and not attempt operation unless this
	 * method returns <code>true</code>. To make sure that a wide variety of
	 * applications can use this class, this method always returns
	 * <code>true</code>.
	 *
	 * @return <code>true</code>
	 */
	@Override
	public boolean canBreak() {
		// don't want someone to bail because of this
		return true;
	}

	// --------
	// -------- Finding iButtons and 1-Wire devices
	// --------

	/**
	 * Returns an enumeration of <code>OneWireContainer</code> objects corresponding
	 * to all of the iButtons or 1-Wire devices found on the 1-Wire Network. In the
	 * case of the <code>DumbAdapter</code>, this method returns a simple copy of
	 * the internal <code>java.util.Vector</code> that stores all the 1-Wire devices
	 * this class finds in a search.
	 *
	 * @return <code>Enumeration</code> of <code>OneWireContainer</code> objects
	 *         found on the 1-Wire Network.
	 */
	@Override
	public Enumeration<OneWireContainer> getAllDeviceContainers() {
		var copy_vector = new Vector<OneWireContainer>();
		synchronized (this.containers) {
			for (var i = 0; i < this.containers.size(); i++) {
				copy_vector.addElement(this.containers.elementAt(i));
			}
		}
		return copy_vector.elements();
	}

	/**
	 * Returns a <code>OneWireContainer</code> object corresponding to the first
	 * iButton or 1-Wire device found on the 1-Wire Network. If no devices are
	 * found, then a <code>null</code> reference will be returned. In most cases,
	 * all further communication with the device is done through the
	 * <code>OneWireContainer</code>.
	 *
	 * @return The first <code>OneWireContainer</code> object found on the 1-Wire
	 *         Network, or <code>null</code> if no devices found.
	 */
	@Override
	public OneWireContainer getFirstDeviceContainer() {
		synchronized (this.containers) {
			if (this.containers.size() > 0) {
				this.containers_index = 1;
				return this.containers.elementAt(0);
			}
			return null;
		}
	}

	/**
	 * Returns a <code>OneWireContainer</code> object corresponding to the next
	 * iButton or 1-Wire device found. The previous 1-Wire device found is used as a
	 * starting point in the search. If no devices are found, then a
	 * <code>null</code> reference will be returned. In most cases, all further
	 * communication with the device is done through the
	 * <code>OneWireContainer</code>.
	 *
	 * @return The next <code>OneWireContainer</code> object found on the 1-Wire
	 *         Network, or <code>null</code> if no iButtons found.
	 */
	@Override
	public OneWireContainer getNextDeviceContainer() {
		synchronized (this.containers) {
			if (this.containers.size() > this.containers_index) {
				this.containers_index++;
				return this.containers.elementAt(this.containers_index - 1);
			}
			return null;
		}
	}

	/**
	 * Returns <code>true</code> if the first iButton or 1-Wire device is found on
	 * the 1-Wire Network. If no devices are found, then <code>false</code> will be
	 * returned.
	 *
	 * @return <code>true</code> if an iButton or 1-Wire device is found.
	 */
	@Override
	public boolean findFirstDevice() {
		synchronized (this.containers) {
			if (this.containers.size() > 0) {
				this.containers_index = 1;
				return true;
			}
			return false;
		}
	}

	/**
	 * Returns <code>true</code> if the next iButton or 1-Wire device is found. The
	 * previous 1-Wire device found is used as a starting point in the search. If no
	 * more devices are found then <code>false</code> will be returned.
	 *
	 * @return <code>true</code> if an iButton or 1-Wire device is found.
	 */
	@Override
	public boolean findNextDevice() {
		synchronized (this.containers) {
			if (this.containers.size() > this.containers_index) {
				this.containers_index++;
				return true;
			}
			return false;
		}
	}

	/**
	 * Copies the 'current' 1-Wire device address being used by the adapter into the
	 * array. This address is the last iButton or 1-Wire device found in a search
	 * (findNextDevice()...). This method copies into a user generated array to
	 * allow the reuse of the buffer. When searching many iButtons on the one wire
	 * network, this will reduce the memory burn rate.
	 *
	 * @param address An array to be filled with the current iButton address.
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public void getAddress(byte[] address) {
		var temp = this.containers.elementAt(this.containers_index - 1);
		if (temp != null) {
			System.arraycopy(temp.getAddress(), 0, address, 0, 8);
		}
	}

	/**
	 * Gets the 'current' 1-Wire device address being used by the adapter as a long.
	 * This address is the last iButton or 1-Wire device found in a search
	 * (findNextDevice()...).
	 *
	 * @return <code>long</code> representation of the iButton address
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public long getAddressAsLong() {
		var address = new byte[8];

		this.getAddress(address);

		return Address.toLong(address);
	}

	/**
	 * Gets the 'current' 1-Wire device address being used by the adapter as a
	 * String. This address is the last iButton or 1-Wire device found in a search
	 * (findNextDevice()...).
	 *
	 * @return <code>String</code> representation of the iButton address
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public String getAddressAsString() {
		var address = new byte[8];

		this.getAddress(address);

		return Address.toString(address);
	}

	/**
	 * Verifies that the iButton or 1-Wire device specified is present on the 1-Wire
	 * Network. This does not affect the 'current' device state information used in
	 * searches (findNextDevice...).
	 *
	 * @param address device address to verify is present
	 *
	 * @return <code>true</code> if device is present, else <code>false</code>.
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public boolean isPresent(byte[] address) {
		return this.isPresent(Address.toLong(address));
	}

	/**
	 * Verifies that the iButton or 1-Wire device specified is present on the 1-Wire
	 * Network. This does not affect the 'current' device state information used in
	 * searches (findNextDevice...).
	 *
	 * @param address device address to verify is present
	 *
	 * @return <code>true</code> if device is present, else <code>false</code>.
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public boolean isPresent(long address) {
		synchronized (this.containers) {
			for (var i = 0; i < this.containers.size(); i++) {
				var temp = this.containers.elementAt(i);
				var addr = temp.getAddressAsLong();
				if (addr == address) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Verifies that the iButton or 1-Wire device specified is present on the 1-Wire
	 * Network. This does not affect the 'current' device state information used in
	 * searches (findNextDevice...).
	 *
	 * @param address device address to verify is present
	 *
	 * @return <code>true</code> if device is present, else <code>false</code>.
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public boolean isPresent(String address) {
		return this.isPresent(Address.toByteArray(address));
	}

	/**
	 * Verifies that the iButton or 1-Wire device specified is present on the 1-Wire
	 * Network and in an alarm state. This method is currently not implemented in
	 * <code>DumbAdapter</code>.
	 *
	 * @param address device address to verify is present and alarming
	 *
	 * @return <code>false</code>
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public boolean isAlarming(byte[] address) {
		return false;
	}

	/**
	 * Verifies that the iButton or 1-Wire device specified is present on the 1-Wire
	 * Network and in an alarm state. This method is currently not implemented in
	 * <code>DumbAdapter</code>.
	 *
	 * @param address device address to verify is present and alarming
	 *
	 * @return <code>false</code>
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public boolean isAlarming(long address) {
		return this.isAlarming(Address.toByteArray(address));
	}

	/**
	 * Verifies that the iButton or 1-Wire device specified is present on the 1-Wire
	 * Network and in an alarm state. This method is currently not implemented in
	 * <code>DumbAdapter</code>.
	 *
	 * @param address device address to verify is present and alarming
	 *
	 * @return <code>false</code>
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public boolean isAlarming(String address) {
		return this.isAlarming(Address.toByteArray(address));
	}

	/**
	 * Selects the specified iButton or 1-Wire device by broadcasting its address.
	 * With a <code>DumbAdapter</code>, this method simply returns true.
	 *
	 * Warning, this does not verify that the device is currently present on the
	 * 1-Wire Network (See isPresent).
	 *
	 * @param address address of iButton or 1-Wire device to select
	 *
	 * @return <code>true</code> if device address was sent, <code>false</code>
	 *         otherwise.
	 *
	 * @see #isPresent(byte[])
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public boolean select(byte[] address) {
		return this.isPresent(address);
	}

	/**
	 * Selects the specified iButton or 1-Wire device by broadcasting its address.
	 * With a <code>DumbAdapter</code>, this method simply returns true.
	 *
	 * Warning, this does not verify that the device is currently present on the
	 * 1-Wire Network (See isPresent).
	 *
	 * @param address address of iButton or 1-Wire device to select
	 *
	 * @return <code>true</code> if device address was sent, <code>false</code>
	 *         otherwise.
	 *
	 * @see #isPresent(byte[])
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public boolean select(long address) throws OneWireIOException, OneWireException {
		return this.select(Address.toByteArray(address));
	}

	/**
	 * Selects the specified iButton or 1-Wire device by broadcasting its address.
	 * With a <code>DumbAdapter</code>, this method simply returns true.
	 *
	 * Warning, this does not verify that the device is currently present on the
	 * 1-Wire Network (See isPresent).
	 *
	 * @param address address of iButton or 1-Wire device to select
	 *
	 * @return <code>true</code> if device address was sent, <code>false</code>
	 *         otherwise.
	 *
	 * @see #isPresent(byte[])
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public boolean select(String address) throws OneWireIOException, OneWireException {
		return this.select(Address.toByteArray(address));
	}

	// --------
	// -------- Finding iButton/1-Wire device options
	// --------

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @see #setNoResetSearch
	 */
	@Override
	public void setSearchOnlyAlarmingDevices() {
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 */
	@Override
	public void setNoResetSearch() {
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @see #setNoResetSearch
	 */
	@Override
	public void setSearchAllDevices() {
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @see #targetFamily
	 * @see #targetFamily(byte[])
	 * @see #excludeFamily
	 * @see #excludeFamily(byte[])
	 */
	@Override
	public void targetAllFamilies() {
		this.include = null;
		this.exclude = null;
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @param family the code of the family type to target for searches
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #targetAllFamilies
	 */
	@Override
	public void targetFamily(int family) {
		if (this.include == null || this.include.length != 1) {
			this.include = new byte[1];
		}

		this.include[0] = (byte) family;
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @param family array of the family types to target for searches
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #targetAllFamilies
	 */
	@Override
	public void targetFamily(byte family[]) {
		if (this.include == null || this.include.length != family.length) {
			this.include = new byte[family.length];
		}

		System.arraycopy(family, 0, this.include, 0, family.length);
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @param family the code of the family type NOT to target in searches
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #targetAllFamilies
	 */
	@Override
	public void excludeFamily(int family) {
		if (this.exclude == null || this.exclude.length != 1) {
			this.exclude = new byte[1];
		}

		this.exclude[0] = (byte) family;
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @param family array of family cods NOT to target for searches
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #targetAllFamilies
	 */
	@Override
	public void excludeFamily(byte family[]) {
		if (this.exclude == null || this.exclude.length != family.length) {
			this.exclude = new byte[family.length];
		}

		System.arraycopy(family, 0, this.exclude, 0, family.length);
	}

	// --------
	// -------- 1-Wire Network Semaphore methods
	// --------

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @param blocking <code>true</code> if want to block waiting for exclusive
	 *                 access to the adapter
	 * @return <code>true</code>
	 */
	@Override
	public boolean beginExclusive(boolean blocking) {
		// DEBUG!!! RIGHT NOW THIS IS NOT IMPLEMENTED!!!
		return true;
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 */
	@Override
	public void endExclusive() {
		// DEBUG!!! RIGHT NOW THIS IS NOT IMPLEMENTED!!!
	}

	// --------
	// -------- Primitive 1-Wire Network data methods
	// --------

	/**
	 * Sends a bit to the 1-Wire Network. This method does nothing in
	 * <code>DumbAdapter</code>.
	 *
	 * @param bitValue the bit value to send to the 1-Wire Network.
	 */
	@Override
	public void putBit(boolean bitValue) {
		// this will not be implemented
	}

	/**
	 * Gets a bit from the 1-Wire Network. This method does nothing in
	 * <code>DumbAdapter</code>.
	 *
	 * @return <code>true</code>
	 */
	@Override
	public boolean getBit() {
		// this will not be implemented
		return true;
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @param byteValue the byte value to send to the 1-Wire Network.
	 */
	@Override
	public void putByte(int byteValue) {
		// this will not be implemented
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @return the value 0x0ff
	 */
	@Override
	public int getByte() {
		// this will not be implemented
		return 0x0ff;
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @param len length of data bytes to receive
	 *
	 * @return a new byte array of length <code>len</code>
	 */
	@Override
	public byte[] getBlock(int len) {
		// this will not be implemented
		return new byte[len];
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @param arr array in which to write the received bytes
	 * @param len length of data bytes to receive
	 */
	@Override
	public void getBlock(byte[] arr, int len) {
		// this will not be implemented
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @param arr array in which to write the received bytes
	 * @param off offset into the array to start
	 * @param len length of data bytes to receive
	 */
	@Override
	public void getBlock(byte[] arr, int off, int len) {
		// this will not be implemented
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @param dataBlock array of data to transfer to and from the 1-Wire Network.
	 * @param off       offset into the array of data to start
	 * @param len       length of data to send / receive starting at 'off'
	 */
	@Override
	public void dataBlock(byte dataBlock[], int off, int len) {
		// this will not be implemented
	}

	/**
	 * Sends a Reset to the 1-Wire Network.
	 *
	 * @return the result of the reset. Potential results are:
	 *         <ul>
	 *         <li>0 (RESET_NOPRESENCE) no devices present on the 1-Wire Network.
	 *         <li>1 (RESET_PRESENCE) normal presence pulse detected on the 1-Wire
	 *         Network indicating there is a device present.
	 *         <li>2 (RESET_ALARM) alarming presence pulse detected on the 1-Wire
	 *         Network indicating there is a device present and it is in the alarm
	 *         condition. This is only provided by the DS1994/DS2404 devices.
	 *         <li>3 (RESET_SHORT) inticates 1-Wire appears shorted. This can be
	 *         transient conditions in a 1-Wire Network. Not all adapter types can
	 *         detect this condition.
	 *         </ul>
	 *         Note that in <code>DumbAdapter</code>, the only possible results are
	 *         0 and 1.
	 */
	@Override
	public int reset() {
		// this will not be implemented
		if (this.containers.size() > 0) {
			return 1;
		}
		return 0;
	}

	// --------
	// -------- 1-Wire Network power methods
	// --------

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
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
	 *                   setPowerNormal() method is called.
	 *                   </ul>
	 */
	@Override
	public void setPowerDuration(int timeFactor) {
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
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
	 * @return <code>true</code>
	 */
	@Override
	public boolean startPowerDelivery(int changeCondition) {
		return true;
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @param timeFactor
	 *                   <ul>
	 *                   <li>7 (DELIVERY_EPROM) provide program pulse for 480
	 *                   microseconds
	 *                   <li>5 (DELIVERY_INFINITE) provide power until the
	 *                   setPowerNormal() method is called.
	 *                   </ul>
	 */
	@Override
	public void setProgramPulseDuration(int timeFactor) {
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
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
	 * @return <code>true</code>
	 */
	@Override
	public boolean startProgramPulse(int changeCondition) {
		return true;
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 */
	@Override
	public void startBreak() {
	}

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 */
	@Override
	public void setPowerNormal() {
	}

	// --------
	// -------- 1-Wire Network speed methods
	// --------

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @param speed
	 *              <ul>
	 *              <li>0 (SPEED_REGULAR) set to normal communication speed
	 *              <li>1 (SPEED_FLEX) set to flexible communication speed used for
	 *              long lines
	 *              <li>2 (SPEED_OVERDRIVE) set to normal communication speed to
	 *              overdrive
	 *              <li>3 (SPEED_HYPERDRIVE) set to normal communication speed to
	 *              hyperdrive
	 *              <li>>3 future speeds
	 *              </ul>
	 *
	 */
	@Override
	public void setSpeed(int speed) {
		this.sp = speed;
	}

	private int sp = 0;

	/**
	 * This method does nothing in <code>DumbAdapter</code>.
	 *
	 * @return <the last value passed to the <code>setSpeed(int)</code> method, or 0
	 */
	@Override
	public int getSpeed() {
		return this.sp;
	}

	// --------
	// -------- Misc
	// --------

	/**
	 * Gets the container from this adapter whose address matches the address of a
	 * container in the <code>DumbAdapter</code>'s internal
	 * <code>java.util.Vector</code>.
	 *
	 * @param address device address with which to find a container
	 *
	 * @return The <code>OneWireContainer</code> object, or <code>null</code> if no
	 *         match could be found.
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public OneWireContainer getDeviceContainer(byte[] address) {
		var addr = Address.toLong(address);
		synchronized (this.containers) {
			for (var i = 0; i < this.containers.size(); i++) {
				if (this.containers.elementAt(i).getAddressAsLong() == addr) {
					return this.containers.elementAt(i);
				}
			}
		}
		return null;

	}

	/**
	 * Gets the container from this adapter whose address matches the address of a
	 * container in the <code>DumbAdapter</code>'s internal
	 * <code>java.util.Vector</code>.
	 *
	 * @param address device address with which to find a container
	 *
	 * @return The <code>OneWireContainer</code> object, or <code>null</code> if no
	 *         match could be found.
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public OneWireContainer getDeviceContainer(long address) {
		return this.getDeviceContainer(Address.toByteArray(address));
	}

	/**
	 * Gets the container from this adapter whose address matches the address of a
	 * container in the <code>DumbAdapter</code>'s internal
	 * <code>java.util.Vector</code>.
	 *
	 * @param address device address with which to find a container
	 *
	 * @return The <code>OneWireContainer</code> object, or <code>null</code> if no
	 *         match could be found.
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public OneWireContainer getDeviceContainer(String address) {
		return this.getDeviceContainer(Address.toByteArray(address));
	}

	/**
	 * Returns a <code>OneWireContainer</code> object using the current 1-Wire
	 * network address. The internal state of the port adapter keeps track of the
	 * last address found and is able to create container objects from this state.
	 *
	 * @return the <code>OneWireContainer</code> object
	 */
	@Override
	public OneWireContainer getDeviceContainer() {

		// Mask off the upper bit.
		var address = new byte[8];

		this.getAddress(address);

		return this.getDeviceContainer(address);
	}

	/**
	 * Checks to see if the family found is in the desired include group.
	 *
	 * @return <code>true</code> if in include group
	 */
	@Override
	protected boolean isValidFamily(byte[] address) {
		var familyCode = address[0];

		if (this.exclude != null) {
			for (byte element : this.exclude) {
				if (familyCode == element) {
					return false;
				}
			}
		}

		if (this.include != null) {
			for (byte element : this.include) {
				if (familyCode == element) {
					return true;
				}
			}

			return false;
		}

		return true;
	}
}
// CHECKSTYLE:ON
