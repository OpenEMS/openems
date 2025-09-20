// CHECKSTYLE:OFF
/*---------------------------------------------------------------------------
 * Copyright (C) 2001 Maxim Integrated Products, All Rights Reserved.
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

import java.io.IOException;
// imports
import java.util.Enumeration;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.utils.CRC8;

/**
 * The LSerialAdapter class implements the DSPortAdapter interface for a legacy
 * 1-Wire serial interface adapters such as the DS9097.
 * <p>
 *
 * Instances of valid LSerialdapter's are retrieved from methods in
 * {@link com.dalsemi.onewire.OneWireAccessProvider OneWireAccessProvider}.
 *
 * <P>
 * The DSPortAdapter methods can be organized into the following categories:
 * </P>
 * <UL>
 * <LI><B> Information </B>
 * <UL>
 * <LI>{@link #getAdapterName() getAdapterName}
 * <LI>{@link #getPortTypeDescription() getPortTypeDescription}
 * <LI>{@link #getClassVersion() getClassVersion}
 * <LI>{@link #adapterDetected() adapterDetected}
 * <LI>{@link #getAdapterVersion() getAdapterVersion}
 * <LI>{@link #getAdapterAddress() getAdapterAddress}
 * </UL>
 * <LI><B> Port Selection </B>
 * <UL>
 * <LI>{@link #getPortNames() getPortNames}
 * <LI>{@link #selectPort(String) selectPort}
 * <LI>{@link #getPortName() getPortName}
 * <LI>{@link #freePort() freePort}
 * </UL>
 * <LI><B> Adapter Capabilities </B>
 * <UL>
 * <LI>{@link #canOverdrive() canOverdrive}
 * <LI>{@link #canHyperdrive() canHyperdrive}
 * <LI>{@link #canFlex() canFlex}
 * <LI>{@link #canProgram() canProgram}
 * <LI>{@link #canDeliverPower() canDeliverPower}
 * <LI>{@link #canDeliverSmartPower() canDeliverSmartPower}
 * <LI>{@link #canBreak() canBreak}
 * </UL>
 * <LI><B> 1-Wire Network Semaphore </B>
 * <UL>
 * <LI>{@link #beginExclusive(boolean) beginExclusive}
 * <LI>{@link #endExclusive() endExclusive}
 * </UL>
 * <LI><B> 1-Wire Device Discovery </B>
 * <UL>
 * <LI>Selective Search Options
 * <UL>
 * <LI>{@link #targetAllFamilies() targetAllFamilies}
 * <LI>{@link #targetFamily(int) targetFamily(int)}
 * <LI>{@link #targetFamily(byte[]) targetFamily(byte[])}
 * <LI>{@link #excludeFamily(int) excludeFamily(int)}
 * <LI>{@link #excludeFamily(byte[]) excludeFamily(byte[])}
 * <LI>{@link #setSearchOnlyAlarmingDevices() setSearchOnlyAlarmingDevices}
 * <LI>{@link #setNoResetSearch() setNoResetSearch}
 * <LI>{@link #setSearchAllDevices() setSearchAllDevices}
 * </UL>
 * <LI>Search With Automatic 1-Wire Container creation
 * <UL>
 * <LI>{@link #getAllDeviceContainers() getAllDeviceContainers}
 * <LI>{@link #getFirstDeviceContainer() getFirstDeviceContainer}
 * <LI>{@link #getNextDeviceContainer() getNextDeviceContainer}
 * </UL>
 * <LI>Search With NO 1-Wire Container creation
 * <UL>
 * <LI>{@link #findFirstDevice() findFirstDevice}
 * <LI>{@link #findNextDevice() findNextDevice}
 * <LI>{@link #getAddress(byte[]) getAddress(byte[])}
 * <LI>{@link #getAddressAsLong() getAddressAsLong}
 * <LI>{@link #getAddressAsString() getAddressAsString}
 * </UL>
 * <LI>Manual 1-Wire Container creation
 * <UL>
 * <LI>{@link #getDeviceContainer(byte[]) getDeviceContainer(byte[])}
 * <LI>{@link #getDeviceContainer(long) getDeviceContainer(long)}
 * <LI>{@link #getDeviceContainer(String) getDeviceContainer(String)}
 * <LI>{@link #getDeviceContainer() getDeviceContainer()}
 * </UL>
 * </UL>
 * <LI><B> 1-Wire Network low level access (usually not called directly) </B>
 * <UL>
 * <LI>Device Selection and Presence Detect
 * <UL>
 * <LI>{@link #isPresent(byte[]) isPresent(byte[])}
 * <LI>{@link #isPresent(long) isPresent(long)}
 * <LI>{@link #isPresent(String) isPresent(String)}
 * <LI>{@link #isAlarming(byte[]) isAlarming(byte[])}
 * <LI>{@link #isAlarming(long) isAlarming(long)}
 * <LI>{@link #isAlarming(String) isAlarming(String)}
 * <LI>{@link #select(byte[]) select(byte[])}
 * <LI>{@link #select(long) select(long)}
 * <LI>{@link #select(String) select(String)}
 * </UL>
 * <LI>Raw 1-Wire IO
 * <UL>
 * <LI>{@link #reset() reset}
 * <LI>{@link #putBit(boolean) putBit}
 * <LI>{@link #getBit() getBit}
 * <LI>{@link #putByte(int) putByte}
 * <LI>{@link #getByte() getByte}
 * <LI>{@link #getBlock(int) getBlock(int)}
 * <LI>{@link #getBlock(byte[], int) getBlock(byte[], int)}
 * <LI>{@link #getBlock(byte[], int, int) getBlock(byte[], int, int)}
 * <LI>{@link #dataBlock(byte[], int, int) dataBlock(byte[], int, int)}
 * </UL>
 * <LI>1-Wire Speed and Power Selection
 * <UL>
 * <LI>{@link #setPowerDuration(int) setPowerDuration}
 * <LI>{@link #startPowerDelivery(int) startPowerDelivery}
 * <LI>{@link #setProgramPulseDuration(int) setProgramPulseDuration}
 * <LI>{@link #startProgramPulse(int) startProgramPulse}
 * <LI>{@link #startBreak() startBreak}
 * <LI>{@link #setPowerNormal() setPowerNormal}
 * <LI>{@link #setSpeed(int) setSpeed}
 * <LI>{@link #getSpeed() getSpeed}
 * </UL>
 * </UL>
 * <LI><B> Advanced </B>
 * <UL>
 * <LI>{@link #registerOneWireContainerClass(int, Class)
 * registerOneWireContainerClass}
 * </UL>
 * </UL>
 *
 * 8/11/2003 - shughes - modified to support RXTX instead of javax.comm
 *
 * @see com.dalsemi.onewire.OneWireAccessProvider
 * @see com.dalsemi.onewire.container.OneWireContainer
 *
 * @version 0.00, 4 Dec 2001
 * @author DS
 *
 *
 */
@SuppressWarnings({ "unused" })
public class LSerialAdapter extends DSPortAdapter {

	// --------
	// -------- Finals
	// --------

	/** Normal Search, all devices participate */
	private static final int NORMAL_SEARCH_CMD = 0xF0;

	/** Conditional Search, only 'alarming' devices participate */
	private static final int ALARM_SEARCH_CMD = 0xEC;

	// --------
	// -------- Static Variables
	// --------

	/** Version string for this adapter class */
	private static String classVersion = "0.00";

	/** Hashtable to contain SerialService instances */
	// private static Hashtable serialServiceHash = new Hashtable(4);

	// --------
	// -------- Variables
	// --------

	/** Reference to the current SerialService */
	private SerialService serial;

	/** String name of the current opened port */
	private boolean adapterPresent;

	/** flag to indicate the last discrepancy */
	private int LastDiscrepancy;

	/** true if device found is the last device */
	private boolean LastDevice;

	/** current device */
	private final byte[] CurrentDevice = new byte[8];

	/**
	 * Whether we are searching only alarming iButtons. This is currently ignored.
	 */
	boolean searchOnlyAlarmingButtons;

	/** Flag to indicate next search will not be preceded by a 1-Wire reset */
	private boolean skipResetOnSearch = false;

	/** Flag to indicate next search will be a 'first' */
	private boolean resetSearch = true;

	/** Flag to indicate have a local begin/end Exclusive use of serial */
	private boolean haveLocalUse;

	/** Field syncObject */
	private final Object syncObject;

	/** Enable/disable debug messages */
	private static boolean doDebugMessages = false;

	// --------
	// -------- Constructor
	// --------

	/**
	 * Constructs a legacy serial adapter class
	 *
	 */
	public LSerialAdapter() {
		this.serial = null;
		this.adapterPresent = false;
		this.haveLocalUse = false;
		this.syncObject = new Object();
	}

	// --------
	// -------- Information Methods
	// --------

	/**
	 * Retrieve the name of the port adapter as a string. The 'Adapter' is a device
	 * that connects to a 'port' that allows one to communicate with an iButton or
	 * other 1-Wire device. As example of this is 'DS9097E'.
	 *
	 * @return <code>String</code> representation of the port adapter.
	 */
	@Override
	public String getAdapterName() {
		return "DS9097";
	}

	/**
	 * Retrieve a description of the port required by this port adapter. An example
	 * of a 'Port' would 'serial communication port'.
	 *
	 * @return <code>String</code> description of the port type required.
	 */
	@Override
	public String getPortTypeDescription() {
		return "serial communication port";
	}

	/**
	 * Retrieve a version string for this class.
	 *
	 * @return version string
	 */
	@Override
	public String getClassVersion() {
		return classVersion;
	}

	// --------
	// -------- Port Selection
	// --------

	/**
	 * Retrieve a list of the platform appropriate port names for this adapter. A
	 * port must be selected with the method 'selectPort' before any other
	 * communication methods can be used. Using a communication method before
	 * 'selectPort' will result in a <code>OneWireException</code> exception.
	 *
	 * @return enumeration of type <code>String</code> that contains the port names
	 */
	@Override
	public Enumeration<String> getPortNames() {
		return SerialService.getSerialPortIdentifiers();
	}

	/**
	 * Specify a platform appropriate port name for this adapter. Note that even
	 * though the port has been selected, it's ownership may be relinquished if it
	 * is not currently held in a 'exclusive' block. This class will then try to
	 * re-acquire the port when needed. If the port cannot be re-acquired when the
	 * exception <code>PortInUseException</code> will be thrown.
	 *
	 * @param newPortName name of the target port, retrieved from getPortNames()
	 *
	 * @return <code>true</code> if the port was acquired, <code>false</code> if the
	 *         port is not available.
	 *
	 * @throws OneWireIOException If port does not exist, or unable to communicate
	 *                            with port.
	 * @throws OneWireException   If port does not exist
	 */
	@Override
	public boolean selectPort(String newPortName) throws OneWireIOException, OneWireException {

		// find the port reference
		this.serial = SerialService.getSerialService(newPortName);

		// check if there is no such port
		if (this.serial == null) {
			throw new OneWireException("DS9097EAdapter: selectPort(), Not such serial port: " + newPortName);
		}

		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// attempt to open the port
			this.serial.openPort();
			this.serial.setBaudRate(115200);

			return true;
		} catch (IOException ioe) {
			throw new OneWireIOException(ioe.toString());
		} finally {

			// release local exclusive use of port
			this.endLocalExclusive();
		}
	}

	/**
	 * Retrieve the name of the selected port as a <code>String</code>.
	 *
	 * @return <code>String</code> of selected port
	 *
	 * @throws OneWireException if valid port not yet selected
	 */
	@Override
	public String getPortName() throws OneWireException {
		if (this.serial != null) {
			return this.serial.getPortName();
		}
		throw new OneWireException("DS9097EAdapter-getPortName, port not selected");
	}

	/**
	 * Free ownership of the selected port if it is currently owned back to the
	 * system. This should only be called if the recently selected port does not
	 * have an adapter or at the end of your application's use of the port.
	 *
	 * @throws OneWireException If port does not exist
	 */
	@Override
	public void freePort() throws OneWireException {
		try {
			// acquire exclusive use of the port
			this.beginLocalExclusive();

			this.adapterPresent = false;

			// attempt to open the port
			this.serial.closePort();
		} catch (IOException ioe) {
			throw new OneWireException("Error closing serial port");
		} finally {
			// release local exclusive use of port
			this.endLocalExclusive();
		}
	}

	// --------
	// -------- Adapter detection
	// --------

	/**
	 * Detect adapter presence on the selected port.
	 *
	 * @return <code>true</code> if the adapter is confirmed to be connected to the
	 *         selected port, <code>false</code> if the adapter is not connected.
	 *
	 * @throws OneWireIOException
	 * @throws OneWireException
	 */
	@Override
	public boolean adapterDetected() throws OneWireIOException, OneWireException {
		if (!this.adapterPresent) {
			try {
				// acquire exclusive use of the port
				this.beginLocalExclusive();

				this.adapterPresent();
			} catch (OneWireIOException e) {
				System.err.println("DS9097EAdapter: Not detected " + e);
			} finally {
				// release local exclusive use of port
				this.endLocalExclusive();
			}
		}

		return this.adapterPresent;
	}

	/**
	 * Retrieve the version of the adapter.
	 *
	 * @return <code>String</code> of the adapter version. It will return "<na>" if
	 *         the adapter version is not or cannot be known.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as no device
	 *                            present. This could be caused by a physical
	 *                            interruption in the 1-Wire Network due to shorts
	 *                            or a newly arriving 1-Wire device issuing a
	 *                            'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 */
	@Override
	public String getAdapterVersion() throws OneWireIOException, OneWireException {
		return "DS9097 adapter";
	}

	/**
	 * Retrieve the address of the adapter if it has one.
	 *
	 * @return <code>String</code> of the adapter address. It will return "<na>" if
	 *         the adapter does not have an address. The address is a string
	 *         representation of an 1-Wire address.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error such as no device
	 *                            present. This could be caused by a physical
	 *                            interruption in the 1-Wire Network due to shorts
	 *                            or a newly arriving 1-Wire device issuing a
	 *                            'presence pulse'.
	 * @throws OneWireException   on a communication or setup error with the 1-Wire
	 *                            adapter
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public String getAdapterAddress() throws OneWireIOException, OneWireException {
		// there is no ID
		return "<no adapter address>";
	}

	// --------
	// -------- Finding iButtons
	// --------

	/** Field currentPosition */
	int currentPosition; // the current position in the list of all devices.

	/**
	 * Returns <code>true</code> if the first iButton or 1-Wire device is found on
	 * the 1-Wire Network. If no devices are found, then <code>false</code> will be
	 * returned.
	 *
	 * @return <code>true</code> if an iButton or 1-Wire device is found.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public boolean findFirstDevice() throws OneWireIOException, OneWireException {
		// reset the internal rom buffer
		this.resetSearch = true;

		return this.findNextDevice();
	}

	/**
	 * Returns <code>true</code> if the next iButton or 1-Wire device is found. The
	 * previous 1-Wire device found is used as a starting point in the search. If no
	 * more devices are found then <code>false</code> will be returned.
	 *
	 * @return <code>true</code> if an iButton or 1-Wire device is found.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public boolean findNextDevice() throws OneWireIOException, OneWireException {
		boolean retval;

		try {
			// acquire exclusive use of the port
			this.beginLocalExclusive();

			while (true) {
				retval = this.search(this.resetSearch);

				if (!retval) {
					this.resetSearch = true;

					return false;
				}
				this.resetSearch = false;

				// check if this is an OK family type
				if (this.isValidFamily(this.CurrentDevice)) {
					return true;
				}
			}
		} finally {
			// release local exclusive use of port
			this.endLocalExclusive();
		}
	}

	/**
	 * Copies the 'current' iButton address being used by the adapter into the
	 * array. This address is the last iButton or 1-Wire device found in a search
	 * (findNextDevice()...).
	 *
	 * @param address An array to be filled with the current iButton address.
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public void getAddress(byte[] address) {
		System.arraycopy(this.CurrentDevice, 0, address, 0, 8);
	}

	/**
	 * Copies the provided 1-Wire device address into the 'current' array. This
	 * address will then be used in the getDeviceContainer() method. Permits the
	 * adapter instance to create containers of devices it did not find in a search.
	 *
	 * @param address An array to be copied into the current iButton address.
	 */
	public void setAddress(byte[] address) {
		System.arraycopy(address, 0, this.CurrentDevice, 0, 8);
	}

	// --------
	// -------- Finding iButton options
	// --------

	/**
	 * Set the 1-Wire Network search to find only iButtons and 1-Wire devices that
	 * are in an 'Alarm' state that signals a need for attention. Not all iButton
	 * types have this feature. Some that do: DS1994, DS1920, DS2407. This selective
	 * searching can be canceled with the 'setSearchAllDevices()' method.
	 *
	 * @see #setNoResetSearch
	 */
	@Override
	public void setSearchOnlyAlarmingDevices() {
		this.searchOnlyAlarmingButtons = true;
	}

	/**
	 * Set the 1-Wire Network search to not perform a 1-Wire reset before a search.
	 * This feature is chiefly used with the DS2409 1-Wire coupler. The normal reset
	 * before each search can be restored with the 'setSearchAllDevices()' method.
	 */
	@Override
	public void setNoResetSearch() {
		this.skipResetOnSearch = true;
	}

	/**
	 * Set the 1-Wire Network search to find all iButtons and 1-Wire devices whether
	 * they are in an 'Alarm' state or not and restores the default setting of
	 * providing a 1-Wire reset command before each search. (see setNoResetSearch()
	 * method).
	 *
	 * @see #setNoResetSearch
	 */
	@Override
	public void setSearchAllDevices() {
		this.searchOnlyAlarmingButtons = false;
		this.skipResetOnSearch = false;
	}

	// --------
	// -------- 1-Wire Network Semaphore methods
	// --------

	/**
	 * Gets exclusive use of the 1-Wire to communicate with an iButton or 1-Wire
	 * Device. This method should be used for critical sections of code where a
	 * sequence of commands must not be interrupted by communication of threads with
	 * other iButtons, and it is permissible to sustain a delay in the special case
	 * that another thread has already been granted exclusive access and this access
	 * has not yet been relinquished.
	 * <p>
	 *
	 * @param blocking <code>true</code> if want to block waiting for exclusive
	 *                 access to the adapter
	 * @return <code>true</code> if blocking was false and a exclusive session with
	 *         the adapter was acquired
	 *
	 * @throws OneWireException on a setup error with the 1-Wire adapter
	 */
	@Override
	public boolean beginExclusive(boolean blocking) throws OneWireException {
		return this.serial.beginExclusive(blocking);
	}

	/**
	 * Relinquishes exclusive control of the 1-Wire Network. This command
	 * dynamically marks the end of a critical section and should be used when
	 * exclusive control is no longer needed.
	 */
	@Override
	public void endExclusive() {
		this.serial.endExclusive();
	}

	/**
	 * Gets exclusive use of the 1-Wire to communicate with an iButton or 1-Wire
	 * Device if it is not already done. Used to make methods thread safe.
	 *
	 * @throws OneWireException on a setup error with the 1-Wire adapter
	 */
	private void beginLocalExclusive() throws OneWireException {

		// check if there is no such port
		if (this.serial == null) {
			throw new OneWireException("DS9097EAdapter: port not selected ");
		}

		// check if already have exclusive use
		if (this.serial.haveExclusive()) {
		} else {
			while (!this.haveLocalUse) {
				synchronized (this.syncObject) {
					this.haveLocalUse = this.serial.beginExclusive(false);
				}
				if (!this.haveLocalUse) {
					try {
						Thread.sleep(50);
					} catch (Exception e) {
					}
				}
			}
		}
	}

	/**
	 * Relinquishes local exclusive control of the 1-Wire Network. This just checks
	 * if we did our own 'beginExclusive' block and frees it.
	 */
	private void endLocalExclusive() {
		synchronized (this.syncObject) {
			if (this.haveLocalUse) {
				this.haveLocalUse = false;

				this.serial.endExclusive();
			}
		}
	}

	// --------
	// -------- Primitive 1-Wire Network data methods
	// --------

	/**
	 * Sends a bit to the 1-Wire Network.
	 *
	 * @param bitValue the bit value to send to the 1-Wire Network.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public void putBit(boolean bitValue) throws OneWireIOException, OneWireException {
		char send_byte;

		try {
			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// make sure adapter is present
			if (!this.adapterDetected()) {
				throw new OneWireIOException("Error communicating with adapter");
			}
			if (bitValue) {
				send_byte = (char) 0xFF;
			} else {
				send_byte = (char) 0x00;
			}

			this.serial.flush();

			this.serial.write(send_byte);
			var result = this.serial.readWithTimeout(1);

			if (result[0] != send_byte) {
				throw new OneWireIOException("Error during putBit(), echo was incorrect");
			}
		} catch (IOException ioe) {
			throw new OneWireIOException(ioe.toString());
		} finally {
			// release local exclusive use of port
			this.endLocalExclusive();
		}
	}

	/**
	 * Gets a bit from the 1-Wire Network.
	 *
	 * @return the bit value received from the the 1-Wire Network.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public boolean getBit() throws OneWireIOException, OneWireException {
		try {
			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// make sure adapter is present
			if (this.adapterDetected()) {
				this.serial.flush();

				this.serial.write((char) 0x00FF);
				var result = this.serial.readWithTimeout(1);

				return result[0] == 0xFF;
			}
			throw new OneWireIOException("Error communicating with adapter");
		} catch (IOException ioe) {
			throw new OneWireIOException(ioe.toString());
		} finally {
			// release local exclusive use of port
			this.endLocalExclusive();
		}
	}

	/**
	 * Sends a byte to the 1-Wire Network.
	 *
	 * @param byteValue the byte value to send to the 1-Wire Network.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public void putByte(int byteValue) throws OneWireIOException, OneWireException {
		var temp_block = new byte[1];

		temp_block[0] = (byte) byteValue;

		this.dataBlock(temp_block, 0, 1);
	}

	/**
	 * Gets a byte from the 1-Wire Network.
	 *
	 * @return the byte value received from the the 1-Wire Network.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public int getByte() throws OneWireIOException, OneWireException {
		var temp_block = new byte[1];

		temp_block[0] = (byte) 0xFF;

		this.dataBlock(temp_block, 0, 1);

		if (temp_block.length == 1) {
			return temp_block[0] & 0xFF;
		}
		throw new OneWireIOException("Error communicating with adapter");
	}

	/**
	 * Get a block of data from the 1-Wire Network.
	 *
	 * @param len length of data bytes to receive
	 *
	 * @return the data received from the 1-Wire Network.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public byte[] getBlock(int len) throws OneWireIOException, OneWireException {
		var temp_block = new byte[len];

		// set block to read 0xFF
		for (var i = 0; i < len; i++) {
			temp_block[i] = (byte) 0xFF;
		}

		this.getBlock(temp_block, len);

		return temp_block;
	}

	/**
	 * Get a block of data from the 1-Wire Network and write it into the provided
	 * array.
	 *
	 * @param arr array in which to write the received bytes
	 * @param len length of data bytes to receive
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public void getBlock(byte[] arr, int len) throws OneWireIOException, OneWireException {
		this.getBlock(arr, 0, len);
	}

	/**
	 * Get a block of data from the 1-Wire Network and write it into the provided
	 * array.
	 *
	 * @param arr array in which to write the received bytes
	 * @param off offset into the array to start
	 * @param len length of data bytes to receive
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public void getBlock(byte[] arr, int off, int len) throws OneWireIOException, OneWireException {
		// set block to read 0xFF
		for (var i = off; i < len; i++) {
			arr[i] = (byte) 0xFF;
		}

		this.dataBlock(arr, off, len);
	}

	/**
	 * Sends a block of data and returns the data received in the same array. This
	 * method is used when sending a block that contains reads and writes. The
	 * 'read' portions of the data block need to be pre-loaded with 0xFF's. It
	 * starts sending data from the index at offset 'off' for length 'len'.
	 *
	 * @param dataBlock array of data to transfer to and from the 1-Wire Network.
	 * @param off       offset into the array of data to start
	 * @param len       length of data to send / receive starting at 'off'
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public void dataBlock(byte dataBlock[], int off, int len) throws OneWireIOException, OneWireException {
		try {
			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// make sure adapter is present
			if (!this.adapterDetected()) {
				throw new OneWireIOException("Error communicating with adapter");
			}
			int t_off, t_len;
			t_off = off;
			t_len = len;

			// break up large blocks to not exceed 128 bytes at a time
			do {
				if (t_len > 128) {
					t_len = 128;
				}

				var send_block = this.constructSendBlock(dataBlock, t_off, t_len);

				this.serial.flush();

				this.serial.write(send_block);
				var raw_recv = this.serial.readWithTimeout(send_block.length);

				var recv = this.interpretRecvBlock(raw_recv);

				System.arraycopy(recv, 0, dataBlock, t_off, t_len);

				t_off += t_len;
				t_len = off + len - t_off;
			} while (t_len > 0);
		} catch (IOException ioe) {
			throw new OneWireIOException(ioe.toString());
		} finally {
			// release local exclusive use of port
			this.endLocalExclusive();
		}
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
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public int reset() throws OneWireIOException, OneWireException {
		try {
			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// make sure adapter is present
			if (this.adapterDetected()) {
				this.serial.flush();

				// send a break to reset 1-Wire
				this.serial.sendBreak(1);

				// get the result
				var c = this.serial.readWithTimeout(1);

				// does not work: return ((c.length > 1) ? RESET_PRESENCE : RESET_NOPRESENCE);

				return RESET_PRESENCE;
			}
			return RESET_NOPRESENCE;
		} catch (IOException ioe) {
			return RESET_NOPRESENCE;
		} catch (OneWireIOException e) {
			if (doDebugMessages) {
				System.err.println("DS9097EAdapter: Not detected " + e);
			}

			return RESET_NOPRESENCE;
		} finally {
			// release local exclusive use of port
			this.endLocalExclusive();
		}
	}

	// --------
	// -------- Support methods
	// --------

	/**
	 * Single search. Take into account reset and alarm options.
	 *
	 * @param resetSearch - true to start search over (like first)
	 *
	 * @return true if device found of false if end of search
	 *
	 * @throws OneWireException
	 * @throws OneWireIOException
	 */
	private boolean search(boolean resetSearch) throws OneWireIOException, OneWireException {
		int bit_test, bit_number;
		int last_zero, serial_byte_number;
		int serial_byte_mask;
		int lastcrc8;
		boolean next_result, search_direction;

		// initialize for search
		bit_number = 1;
		last_zero = 0;
		serial_byte_number = 0;
		serial_byte_mask = 1;
		next_result = false;
		lastcrc8 = 0;

		// check for a force reset of the search
		if (resetSearch) {
			this.LastDiscrepancy = 0;
			this.LastDevice = false;
		}

		// if the last call was not the last one
		if (!this.LastDevice) {
			// check if reset first is requested
			if (!this.skipResetOnSearch) {
				// reset the 1-wire
				// if there are no parts on 1-wire, return false
				if (this.reset() != RESET_PRESENCE) {
					// reset the search
					this.LastDiscrepancy = 0;
					return false;
				}
			}

			// If finding alarming devices issue a different command
			if (this.searchOnlyAlarmingButtons) {
				this.putByte(ALARM_SEARCH_CMD); // issue the alarming search command
			} else {
				this.putByte(NORMAL_SEARCH_CMD); // issue the search command
			}

			// loop to do the search
			do {
				// read a bit and its compliment
				bit_test = (this.getBit() ? 1 : 0) << 1;
				bit_test |= this.getBit() ? 1 : 0;

				// check for no devices on 1-wire
				if (bit_test == 3) {
					break;
				}
				// all devices coupled have 0 or 1
				if (bit_test > 0) {
					search_direction = !((bit_test & 0x01) == 0x01); // bit write value for search
				} else {
					// if this discrepancy if before the Last Discrepancy
					// on a previous next then pick the same as last time
					if (bit_number < this.LastDiscrepancy) {
						search_direction = (this.CurrentDevice[serial_byte_number] & serial_byte_mask) > 0;
					} else {
						// if equal to last pick 1, if not then pick 0
						search_direction = bit_number == this.LastDiscrepancy;
					}

					// if 0 was picked then record its position in LastZero
					if (!search_direction) {
						last_zero = bit_number;
					}

					// check for Last discrepancy in family
					if (last_zero < 9) {
					}
				}

				// set or clear the bit in the CurrentDevice byte serial_byte_number
				// with mask serial_byte_mask
				if (search_direction) {
					this.CurrentDevice[serial_byte_number] |= serial_byte_mask;
				} else {
					this.CurrentDevice[serial_byte_number] &= ~serial_byte_mask;
				}

				// serial number search direction write bit
				this.putBit(search_direction);

				// increment the byte counter bit_number
				// and shift the mask serial_byte_mask
				bit_number++;
				serial_byte_mask = (serial_byte_mask <<= 1) & 0x00FF;

				// if the mask is 0 then go to new CurrentDevice byte serial_byte_number
				// and reset mask
				if (serial_byte_mask == 0) {
					// accumulate the CRC
					lastcrc8 = CRC8.compute(this.CurrentDevice[serial_byte_number], lastcrc8);
					serial_byte_number++;
					serial_byte_mask = 1;
				}
			} while (serial_byte_number < 8); // loop until through all CurrentDevice bytes 0-7

			// if the search was successful then
			if (!(bit_number < 65 || lastcrc8 != 0)) {
				// search successful so set LastDiscrepancy,LastDevice,next_result
				this.LastDiscrepancy = last_zero;
				this.LastDevice = this.LastDiscrepancy == 0;
				next_result = true;
			}
		}

		// if no device found then reset counters so next 'next' will be
		// like a first
		if (!next_result || this.CurrentDevice[0] == 0) {
			this.LastDiscrepancy = 0;
			this.LastDevice = false;
			next_result = false;
		}

		return next_result;
	}

	/**
	 * Attempt to detect prense of DS9097 style adapter. Mostly just checks to make
	 * sure it is NOT a DS2480 or an AT modem.
	 *
	 * @return true if adapter likely present
	 */
	private boolean adapterPresent() {
		if (!this.adapterPresent) {
			char[] test_buf = { 0xFF, 0x00, 0xFF, 0x00, 0xFF, 0x00, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x00, 0x00, 0xE3, 0xC1, 'A', 'T', 'E', '0', 0x0D, 'A' };

			try {
				// do reset
				this.serial.flush();

				// send a break to reset 1-Wire
				this.serial.sendBreak(1);

				// get the result
				var c = this.serial.readWithTimeout(1);

				// send the test message
				this.serial.flush();
				this.serial.write(test_buf);

				// get echo
				var result = this.serial.readWithTimeout(test_buf.length);

				this.serial.flush();

				// if get entire echo then must be OK
				this.adapterPresent = true;
			} catch (IOException ioe) {
				// DRAIN
			}
		}

		return this.adapterPresent;
	}

	/**
	 * Translate a data block to a DS9097 style block where every byte represent one
	 * bit timeslot.
	 *
	 * @param data byte array
	 * @param off  offset into data array
	 * @param len  length of data array to send
	 *
	 * @return character array send block
	 */
	private char[] constructSendBlock(byte[] data, int off, int len) {
		int shift_byte, cnt = 0;
		var send_block = new char[len * 8];

		for (var i = 0; i < len; i++) {
			shift_byte = data[i + off];

			for (var j = 0; j < 8; j++) {
				if ((shift_byte & 0x01) == 0x01) {
					send_block[cnt++] = 0x00FF;
				} else {
					send_block[cnt++] = 0x00;
				}

				shift_byte >>>= 1;
			}
		}

		return send_block;
	}

	/**
	 * Inerpret a response communication block from a DS9097 adapter and translate
	 * it into a byte array of data.
	 *
	 * @param rawBlock character array of raw communication
	 *
	 * @return byte array of data received
	 */
	private byte[] interpretRecvBlock(char[] rawBlock) {
		int shift_byte = 0, bit_cnt = 0, byte_cnt = 0;
		var recv_block = new byte[rawBlock.length / 8];

		for (char element : rawBlock) {
			shift_byte >>>= 1;

			if (element == 0x00FF) {
				shift_byte |= 0x80;
			}

			bit_cnt++;

			if (bit_cnt == 8) {
				bit_cnt = 0;
				recv_block[byte_cnt++] = (byte) shift_byte;
				shift_byte = 0;
			}
		}

		return recv_block;
	}

	// --------
	// -------- Static
	// --------

	/**
	 * Static method called before instance is created. Attempt to create a hash of
	 * SerialService's and get the max baud rate.
	 */
	/*
	 * static {
	 *
	 * // create a SerialServices instance for each port available and put in hash
	 * Enumeration com_enum = CommPortIdentifier.getPortIdentifiers();
	 * CommPortIdentifier port_id; SerialService serial_instance;
	 *
	 * // loop through all of the serial port elements while
	 * (com_enum.hasMoreElements()) {
	 *
	 * // get the next com port port_id = (CommPortIdentifier)
	 * com_enum.nextElement();
	 *
	 * // only collect the names of the serial ports if (port_id.getPortType() ==
	 * CommPortIdentifier.PORT_SERIAL) { serial_instance = new
	 * SerialService(port_id.getName());
	 *
	 * serialServiceHash.put(port_id.getName(), serial_instance);
	 *
	 * if (doDebugMessages) System.out.println("DEBUG: Serial port: " +
	 * port_id.getName()); } } }
	 */
}
// CHECKSTYLE:ON
