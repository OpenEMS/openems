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

import java.io.IOException;
// imports
import java.util.Enumeration;

import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.utils.Bit;
import com.dalsemi.onewire.utils.CRC8;

/**
 * The USerialAdapter class implements the DSPortAdapter interface for a DS2480
 * based serial adapter such as the DS9097U-009 or DS9097U-S09.
 * <p>
 *
 * Instances of valid USerialAdapter's are retrieved from methods in
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
 * @see com.dalsemi.onewire.OneWireAccessProvider
 * @see com.dalsemi.onewire.container.OneWireContainer
 *
 * @version 0.10, 24 Aug 2001
 * @author DS
 *
 */
@SuppressWarnings({ "unused" })
public class USerialAdapter extends DSPortAdapter {

	// --------
	// -------- Finals
	// --------

	/** Family code for the EPROM iButton DS1982 */
	private static final int ADAPTER_ID_FAMILY = 0x09;

	/** Extended read page command for DS1982 */
	private static final int EXTENDED_READ_PAGE = 0xC3;

	/** Normal Search, all devices participate */
	private static final char NORMAL_SEARCH_CMD = 0xF0;

	/** Conditional Search, only 'alarming' devices participate */
	private static final char ALARM_SEARCH_CMD = 0xEC;

	// --------
	// -------- Static Variables
	// --------

	/** Version string for this adapter class */
	private static String classVersion = "0.10";

	/** Hashtable to contain SerialService instances */
	// private static Hashtable serailServiceHash = new Hashtable(4);

	/** Max baud rate supported by DS9097U */
	private static int maxBaud;

	// --------
	// -------- Variables
	// --------

	/** Reference to the current SerialService */
	private SerialService serial;

	/** String name of the current opened port */
	private boolean adapterPresent;

	/** U Adapter packet builder */
	UPacketBuilder uBuild;

	/** State of the OneWire */
	private OneWireState owState;

	/** U Adapter state */
	private final UAdapterState uState;

	/** Input buffer to hold received data */
	private final StringBuilder inBuffer;

	/** Flag to indicate have a local begin/end Exclusive use of serial */
	private boolean haveLocalUse;
	private final Object syncObject;

	/** Enable/disable debug messages */
	private static boolean doDebugMessages = false;

	// --------
	// -------- Constructor
	// --------

	/**
	 * Constructs a DS9097U serial adapter class
	 *
	 */
	public USerialAdapter() {
		this.serial = null;
		this.owState = new OneWireState();
		this.uState = new UAdapterState(this.owState);
		this.uBuild = new UPacketBuilder(this.uState);
		this.inBuffer = new StringBuilder();
		this.adapterPresent = false;
		this.haveLocalUse = false;
		this.syncObject = new Object();
	}

	// --------
	// -------- Information Methods
	// --------

	@Override
	protected void finalize() {
		try {
			this.freePort();
		} catch (Exception e) {

		}
	}

	/**
	 * Cleans up the resources used by the thread argument. If another thread starts
	 * communicating with this port, and then goes away, there is no way to
	 * relinquish the port without stopping the process. This method allows other
	 * threads to clean up.
	 *
	 * @param thread that may have used a <code>USerialAdapter</code>
	 */
	public static void CleanUpByThread(Thread t) {
		if (doDebugMessages) {
			System.out.println("CleanUpByThread called: Thread=" + t);
		}
		SerialService.CleanUpByThread(t);
	}

	/**
	 * Retrieve the name of the port adapter as a string. The 'Adapter' is a device
	 * that connects to a 'port' that allows one to communicate with an iButton or
	 * other 1-Wire device. As example of this is 'DS9097U'.
	 *
	 * @return <code>String</code> representation of the port adapter.
	 */
	@Override
	public String getAdapterName() {
		return "DS9097U";
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
		// ( SerialService ) serailServiceHash.get(newPortName);

		// check if there is no such port
		if (this.serial == null) {
			throw new OneWireException("USerialAdapter: selectPort(), Not such serial port: " + newPortName);
		}

		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// attempt to open the port
			this.serial.openPort();

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
		throw new OneWireException("USerialAdapter-getPortName, port not selected");
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

			// attempt to close the port
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
		boolean rt;

		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();
			this.uAdapterPresent();

			rt = this.uVerify();
		} catch (OneWireException e) {
			rt = false;
		} finally {

			// release local exclusive use of port
			this.endLocalExclusive();
		}

		return rt;
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
		var version_string = "DS2480 based adapter";
		boolean rt;

		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// only check if the port is acquired
			if (!this.uAdapterPresent()) {
				throw new OneWireIOException("USerialAdapter-getAdapterVersion, adapter not present");
			}
			// perform a reset to read the version
			if (this.uState.revision == 0) {
				this.reset();
			}

			return version_string.concat(", version " + (this.uState.revision >> 2));
		} finally {

			// release local exclusive use of port
			this.endLocalExclusive();
		}
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

		// get a reference to the current oneWire State
		var preserved_mstate = this.owState;

		this.owState = new OneWireState();

		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// only check if the port is acquired
			if (!this.uAdapterPresent()) {
				throw new OneWireIOException("USerialAdapter-getAdapterAddress, adapter not present");
			}
			// set the search to find all of the available DS1982's
			this.setSearchAllDevices();
			this.targetAllFamilies();
			this.targetFamily(ADAPTER_ID_FAMILY);

			var adapter_id_enum = this.getAllDeviceContainers();
			var address = new byte[8];

			// loop through each of the DS1982's to find an adapter ID
			for (; adapter_id_enum.hasMoreElements();) {
				var ibutton = adapter_id_enum.nextElement();

				System.arraycopy(ibutton.getAddress(), 0, address, 0, 8);

				// select this device
				if (this.select(address)) {

					// create a buffer to read the first page
					var read_buffer = new byte[37];
					var cnt = 0;
					int i;

					// extended read memory command
					read_buffer[cnt++] = (byte) EXTENDED_READ_PAGE;

					// address of first page
					read_buffer[cnt++] = 0;
					read_buffer[cnt++] = 0;

					// CRC, data of page and CRC from device
					for (i = 0; i < 34; i++) {
						read_buffer[cnt++] = (byte) 0xFF;
					}

					// perform CRC8 of the first chunk of known data
					var crc8 = CRC8.compute(read_buffer, 0, 3, 0);

					// send/receive data to 1-Wire
					this.dataBlock(read_buffer, 0, cnt);

					// check the first CRC
					if (CRC8.compute(read_buffer, 3, 1, crc8) == 0) {

						// compute the next CRC8 with data from device
						if (CRC8.compute(read_buffer, 4, 33, 0) == 0) {

							// now loop to see if all data is 0xFF
							for (i = 4; i < 36; i++) {
								if (read_buffer[i] != (byte) 0xFF) {
									continue;
								}
							}

							// must be the one!
							if (i == 36) {
								return ibutton.getAddressAsString();
							}
						}
					}
				}
			}
		} catch (OneWireException e) {

			// Drain.
		} finally {

			// restore the old state
			this.owState = preserved_mstate;

			// release local exclusive use of port
			this.endLocalExclusive();
		}

		// don't know the ID
		return "<not available>";
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
		return true;
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
		return true;
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
		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// only check if the port is acquired
			if (!this.uAdapterPresent()) {
				throw new OneWireIOException("USerialAdapter-canProgram, adapter not present");
			}
			// perform a reset to read the program available flag
			if (this.uState.revision == 0) {
				this.reset();
			}

			// return the flag
			return this.uState.programVoltageAvailable;
		} finally {

			// release local exclusive use of port
			this.endLocalExclusive();
		}
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
		return true;
	}

	// --------
	// -------- Finding iButtons
	// --------

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

		// reset the current search
		this.owState.searchLastDiscrepancy = 0;
		this.owState.searchFamilyLastDiscrepancy = 0;
		this.owState.searchLastDevice = false;

		// search for the first device using next
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
		boolean search_result;

		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// check for previous last device
			if (this.owState.searchLastDevice) {
				this.owState.searchLastDiscrepancy = 0;
				this.owState.searchFamilyLastDiscrepancy = 0;
				this.owState.searchLastDevice = false;

				return false;
			}

			// check for 'first' and only 1 target
			if (this.owState.searchLastDiscrepancy == 0 && this.owState.searchLastDevice == false
					&& this.owState.searchIncludeFamilies.length == 1) {

				// set the search to find the 1 target first
				this.owState.searchLastDiscrepancy = 64;

				// create an id to set
				var new_id = new byte[8];

				// set the family code
				new_id[0] = this.owState.searchIncludeFamilies[0];

				// clear the rest
				for (var i = 1; i < 8; i++) {
					new_id[i] = 0;
				}

				// set this new ID
				System.arraycopy(new_id, 0, this.owState.ID, 0, 8);
			}

			// loop until the correct type is found or no more devices
			do {

				// perform a search and keep the result
				search_result = this.search(this.owState);

				if (search_result) {

					// check if not in exclude list
					var is_excluded = false;

					for (byte element : this.owState.searchExcludeFamilies) {
						if (this.owState.ID[0] == element) {
							is_excluded = true;

							break;
						}
					}

					// if not in exclude list then check for include list
					if (!is_excluded) {

						// loop through the include list
						var is_included = false;

						for (byte element : this.owState.searchIncludeFamilies) {
							if (this.owState.ID[0] == element) {
								is_included = true;

								break;
							}
						}

						// check if include list or there is no include list
						if (is_included || this.owState.searchIncludeFamilies.length == 0) {
							return true;
						}
					}
				}

				// skip the current type if not last device
				if (!this.owState.searchLastDevice && this.owState.searchFamilyLastDiscrepancy != 0) {
					this.owState.searchLastDiscrepancy = this.owState.searchFamilyLastDiscrepancy;
					this.owState.searchFamilyLastDiscrepancy = 0;
					this.owState.searchLastDevice = false;
				}

				// end of search so reset and return
				else {
					this.owState.searchLastDiscrepancy = 0;
					this.owState.searchFamilyLastDiscrepancy = 0;
					this.owState.searchLastDevice = false;
					search_result = false;
				}
			} while (search_result);

			// device not found
			return false;
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
		System.arraycopy(this.owState.ID, 0, address, 0, 8);
	}

	/**
	 * Copies the provided 1-Wire device address into the 'current' array. This
	 * address will then be used in the getDeviceContainer() method. Permits the
	 * adapter instance to create containers of devices it did not find in a search.
	 *
	 * @param address An array to be copied into the current iButton address.
	 */
	public void setAddress(byte[] address) {
		System.arraycopy(address, 0, this.owState.ID, 0, 8);
	}

	/**
	 * Verifies that the iButton or 1-Wire device specified is present on the 1-Wire
	 * Network. This does not affect the 'current' device state information used in
	 * searches (findNextDevice...).
	 *
	 * @param address device address to verify is present
	 *
	 * @return <code>true</code> if device is present else <code>false</code>.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public boolean isPresent(byte[] address) throws OneWireIOException, OneWireException {
		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// make sure adapter is present
			if (!this.uAdapterPresent()) {
				throw new OneWireIOException("Error communicating with adapter");
			}
			// check for pending power conditions
			if (this.owState.oneWireLevel != LEVEL_NORMAL) {
				this.setPowerNormal();
			}

			// if in overdrive, then use the block method in super
			if (this.owState.oneWireSpeed == SPEED_OVERDRIVE) {
				return this.blockIsPresent(address, false);
			}

			// create a private OneWireState
			var onewire_state = new OneWireState();

			// set the ID to the ID of the iButton passes to this method
			System.arraycopy(address, 0, onewire_state.ID, 0, 8);

			// set the state to find the specified device
			onewire_state.searchLastDiscrepancy = 64;
			onewire_state.searchFamilyLastDiscrepancy = 0;
			onewire_state.searchLastDevice = false;
			onewire_state.searchOnlyAlarmingButtons = false;

			// perform a search
			if (this.search(onewire_state)) {

				// compare the found device with the desired device
				for (var i = 0; i < 8; i++) {
					if (address[i] != onewire_state.ID[i]) {
						return false;
					}
				}

				// must be the correct device
				return true;
			}

			// failed to find device
			return false;
		} finally {

			// release local exclusive use of port
			this.endLocalExclusive();
		}
	}

	/**
	 * Verifies that the iButton or 1-Wire device specified is present on the 1-Wire
	 * Network and in an alarm state. This does not affect the 'current' device
	 * state information used in searches (findNextDevice...).
	 *
	 * @param address device address to verify is present and alarming
	 *
	 * @return <code>true</code> if device is present and alarming else
	 *         <code>false</code>.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 *
	 * @see com.dalsemi.onewire.utils.Address
	 */
	@Override
	public boolean isAlarming(byte[] address) throws OneWireIOException, OneWireException {
		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// make sure adapter is present
			if (!this.uAdapterPresent()) {
				throw new OneWireIOException("Error communicating with adapter");
			}
			// check for pending power conditions
			if (this.owState.oneWireLevel != LEVEL_NORMAL) {
				this.setPowerNormal();
			}

			// if in overdrive, then use the block method in super
			if (this.owState.oneWireSpeed == SPEED_OVERDRIVE) {
				return this.blockIsPresent(address, true);
			}

			// create a private OneWireState
			var onewire_state = new OneWireState();

			// set the ID to the ID of the iButton passes to this method
			System.arraycopy(address, 0, onewire_state.ID, 0, 8);

			// set the state to find the specified device (alarming)
			onewire_state.searchLastDiscrepancy = 64;
			onewire_state.searchFamilyLastDiscrepancy = 0;
			onewire_state.searchLastDevice = false;
			onewire_state.searchOnlyAlarmingButtons = true;

			// perform a search
			if (this.search(onewire_state)) {

				// compare the found device with the desired device
				for (var i = 0; i < 8; i++) {
					if (address[i] != onewire_state.ID[i]) {
						return false;
					}
				}

				// must be the correct device
				return true;
			}

			// failed to find any alarming device
			return false;
		} finally {

			// release local exclusive use of port
			this.endLocalExclusive();
		}
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
		this.owState.searchOnlyAlarmingButtons = true;
	}

	/**
	 * Set the 1-Wire Network search to not perform a 1-Wire reset before a search.
	 * This feature is chiefly used with the DS2409 1-Wire coupler. The normal reset
	 * before each search can be restored with the 'setSearchAllDevices()' method.
	 */
	@Override
	public void setNoResetSearch() {
		this.owState.skipResetOnSearch = true;
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
		this.owState.searchOnlyAlarmingButtons = false;
		this.owState.skipResetOnSearch = false;
	}

	/**
	 * Removes any selectivity during a search for iButtons or 1-Wire devices by
	 * family type. The unique address for each iButton and 1-Wire device contains a
	 * family descriptor that indicates the capabilities of the device.
	 *
	 * @see #targetFamily
	 * @see #targetFamily(byte[])
	 * @see #excludeFamily
	 * @see #excludeFamily(byte[])
	 */
	@Override
	public void targetAllFamilies() {

		// clear the include and exclude family search lists
		this.owState.searchIncludeFamilies = new byte[0];
		this.owState.searchExcludeFamilies = new byte[0];
	}

	/**
	 * Takes an integer to selectively search for this desired family type. If this
	 * method is used, then no devices of other families will be found by
	 * getFirstButton() & getNextButton().
	 *
	 * @param family the code of the family type to target for searches
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #targetAllFamilies
	 */
	@Override
	public void targetFamily(int familyID) {

		// replace include family array with 1 element array
		this.owState.searchIncludeFamilies = new byte[1];
		this.owState.searchIncludeFamilies[0] = (byte) familyID;
	}

	/**
	 * Takes an array of bytes to use for selectively searching for acceptable
	 * family codes. If used, only devices with family codes in this array will be
	 * found by any of the search methods.
	 *
	 * @param family array of the family types to target for searches
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #targetAllFamilies
	 */
	@Override
	public void targetFamily(byte familyID[]) {

		// replace include family array with new array
		this.owState.searchIncludeFamilies = new byte[familyID.length];

		System.arraycopy(familyID, 0, this.owState.searchIncludeFamilies, 0, familyID.length);
	}

	/**
	 * Takes an integer family code to avoid when searching for iButtons. or 1-Wire
	 * devices. If this method is used, then no devices of this family will be found
	 * by any of the search methods.
	 *
	 * @param family the code of the family type NOT to target in searches
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #targetAllFamilies
	 */
	@Override
	public void excludeFamily(int familyID) {

		// replace exclude family array with 1 element array
		this.owState.searchExcludeFamilies = new byte[1];
		this.owState.searchExcludeFamilies[0] = (byte) familyID;
	}

	/**
	 * Takes an array of bytes containing family codes to avoid when finding
	 * iButtons or 1-Wire devices. If used, then no devices with family codes in
	 * this array will be found by any of the search methods.
	 *
	 * @param family array of family cods NOT to target for searches
	 * @see com.dalsemi.onewire.utils.Address
	 * @see #targetAllFamilies
	 */
	@Override
	public void excludeFamily(byte familyID[]) {

		// replace exclude family array with new array
		this.owState.searchExcludeFamilies = new byte[familyID.length];

		System.arraycopy(familyID, 0, this.owState.searchExcludeFamilies, 0, familyID.length);
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
			throw new OneWireException("USerialAdapter: port not selected ");
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
		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// make sure adapter is present
			if (!this.uAdapterPresent()) {
				throw new OneWireIOException("Error communicating with adapter");
			}
			// check for pending power conditions
			if (this.owState.oneWireLevel != LEVEL_NORMAL) {
				this.setPowerNormal();
			}

			// flush out the com buffer
			this.serial.flush();

			// build a message to send bit to the U brick
			this.uBuild.restart();

			var bit_offset = this.uBuild.dataBit(bitValue, this.owState.levelChangeOnNextBit);

			// check if just started power delivery
			if (this.owState.levelChangeOnNextBit) {

				// clear the primed condition
				this.owState.levelChangeOnNextBit = false;

				// set new level state
				this.owState.oneWireLevel = LEVEL_POWER_DELIVERY;
			}

			// send and receive
			var result_array = this.uTransaction(this.uBuild);

			// check for echo
			if (bitValue != this.uBuild.interpretOneWireBit(result_array[bit_offset])) {
				throw new OneWireIOException("1-Wire communication error, echo was incorrect");
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
			if (!this.uAdapterPresent()) {
				throw new OneWireIOException("Error communicating with adapter");
			}
			// check for pending power conditions
			if (this.owState.oneWireLevel != LEVEL_NORMAL) {
				this.setPowerNormal();
			}

			// flush out the com buffer
			this.serial.flush();

			// build a message to send bit to the U brick
			this.uBuild.restart();

			var bit_offset = this.uBuild.dataBit(true, this.owState.levelChangeOnNextBit);

			// check if just started power delivery
			if (this.owState.levelChangeOnNextBit) {

				// clear the primed condition
				this.owState.levelChangeOnNextBit = false;

				// set new level state
				this.owState.oneWireLevel = LEVEL_POWER_DELIVERY;
			}

			// send and receive
			var result_array = this.uTransaction(this.uBuild);

			// check the result
			if (result_array.length == bit_offset + 1) {
				return this.uBuild.interpretOneWireBit(result_array[bit_offset]);
			} else {
				return false;
			}
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

		// check to make sure echo was what was sent
		if (temp_block[0] != (byte) byteValue) {
			throw new OneWireIOException("Error short on 1-Wire during putByte");
		}
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
		int data_offset;
		char[] ret_data;

		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// make sure adapter is present
			if (!this.uAdapterPresent()) {
				throw new OneWireIOException("Error communicating with adapter");
			}
			// check for pending power conditions
			if (this.owState.oneWireLevel != LEVEL_NORMAL) {
				this.setPowerNormal();
			}

			// set the correct baud rate to stream this operation
			this.setStreamingSpeed(UPacketBuilder.OPERATION_BYTE);

			// flush out the com buffer
			this.serial.flush();

			// build a message to write/read data bytes to the U brick
			this.uBuild.restart();

			// check for primed byte
			if (len == 1 && this.owState.levelChangeOnNextByte) {
				data_offset = this.uBuild.primedDataByte(dataBlock[off]);
				this.owState.levelChangeOnNextByte = false;

				// send and receive
				ret_data = this.uTransaction(this.uBuild);

				// set new level state
				this.owState.oneWireLevel = LEVEL_POWER_DELIVERY;

				// extract the result byte
				dataBlock[off] = this.uBuild.interpretPrimedByte(ret_data, data_offset);
			} else {
				data_offset = this.uBuild.dataBytes(dataBlock, off, len);

				// send and receive
				ret_data = this.uTransaction(this.uBuild);

				// extract the result byte(s)
				this.uBuild.interpretDataBytes(ret_data, data_offset, dataBlock, off, len);
			}
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
			if (!this.uAdapterPresent()) {
				throw new OneWireIOException("Error communicating with adapter");
			}
			// check for pending power conditions
			if (this.owState.oneWireLevel != LEVEL_NORMAL) {
				this.setPowerNormal();
			}

			// flush out the com buffer
			this.serial.flush();

			// build a message to read the baud rate from the U brick
			this.uBuild.restart();

			var reset_offset = this.uBuild.oneWireReset();

			// send and receive
			var result_array = this.uTransaction(this.uBuild);

			// check the result
			if (result_array.length == reset_offset + 1) {
				return this.uBuild.interpretOneWireReset(result_array[reset_offset]);
			} else {
				throw new OneWireIOException("USerialAdapter-reset: no return byte form 1-Wire reset");
			}
		} catch (IOException ioe) {
			throw new OneWireIOException(ioe.toString());
		} finally {

			// release local exclusive use of port
			this.endLocalExclusive();
		}
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
		this.owState.levelTimeFactor = DELIVERY_INFINITE;
	}

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
		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			switch (changeCondition) {
			case CONDITION_AFTER_BIT:
				this.owState.levelChangeOnNextBit = true;
				this.owState.primedLevelValue = LEVEL_POWER_DELIVERY;
				break;
			case CONDITION_AFTER_BYTE:
				this.owState.levelChangeOnNextByte = true;
				this.owState.primedLevelValue = LEVEL_POWER_DELIVERY;
				break;
			case CONDITION_NOW:
				// make sure adapter is present
				if (!this.uAdapterPresent()) {
					throw new OneWireIOException("Error communicating with adapter");
				}
				// check for pending power conditions
				if (this.owState.oneWireLevel != LEVEL_NORMAL) {
					this.setPowerNormal();
				}

				// flush out the com buffer
				this.serial.flush();

				// build a message to read the baud rate from the U brick
				this.uBuild.restart();

				// set the SPUD time value
				var set_SPUD_offset = this.uBuild.setParameter(UParameterSettings.PARAMETER_5VPULSE,
						UParameterSettings.TIME5V_infinite);

				// add the command to begin the pulse
				this.uBuild.sendCommand(UPacketBuilder.FUNCTION_5VPULSE_NOW, false);

				// send and receive
				var result_array = this.uTransaction(this.uBuild);

				// check the result
				if (result_array.length == set_SPUD_offset + 1) {
					this.owState.oneWireLevel = LEVEL_POWER_DELIVERY;

					return true;
				}
				break;
			default:
				throw new OneWireException("Invalid power delivery condition");
			}

			return false;
		} catch (IOException ioe) {
			throw new OneWireIOException(ioe.toString());
		} finally {

			// release local exclusive use of port
			this.endLocalExclusive();
		}
	}

	/**
	 * Sets the duration for providing a program pulse on the 1-Wire Network. This
	 * method takes a time parameter that indicates the program pulse length when
	 * the method startProgramPulse().
	 * <p>
	 *
	 * Note: to avoid getting an exception, use the canDeliverPower() method to
	 * check it's availability.
	 * <p>
	 *
	 * @param timeFactor
	 *                   <ul>
	 *                   <li>6 (DELIVERY_EPROM) provide program pulse for 480
	 *                   microseconds
	 *                   <li>5 (DELIVERY_INFINITE) provide power until the
	 *                   setBusNormal() method is called.
	 *                   </ul>
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	@Override
	public void setProgramPulseDuration(int timeFactor) throws OneWireIOException, OneWireException {
		if (timeFactor != DELIVERY_EPROM) {
			throw new OneWireException("Only support EPROM length program pulse duration");
		}
	}

	/**
	 * Sets the 1-Wire Network voltage to eprom programming level. This method takes
	 * a time parameter that indicates whether the power delivery should be done
	 * immediately, or after certain conditions have been met.
	 * <p>
	 *
	 * Note: to avoid getting an exception, use the canProgram() method to check
	 * it's availability.
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
	 * @throws OneWireException   on a setup error with the 1-Wire adapter or the
	 *                            adapter does not support this operation
	 */
	@Override
	public boolean startProgramPulse(int changeCondition) throws OneWireIOException, OneWireException {

		// check if adapter supports program
		if (!this.uState.programVoltageAvailable) {
			throw new OneWireException("USerialAdapter: startProgramPulse, program voltage not available");
		}

		// check if correct change condition
		if (changeCondition != CONDITION_NOW) {
			throw new OneWireException("USerialAdapter: startProgramPulse, CONDITION_NOW only currently supported");
		}

		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// build a message to read the baud rate from the U brick
			this.uBuild.restart();

			// int set_SPUD_offset =
			this.uBuild.setParameter(UParameterSettings.PARAMETER_12VPULSE, UParameterSettings.TIME12V_512us);

			// add the command to begin the pulse
			// int pulse_offset =
			this.uBuild.sendCommand(UPacketBuilder.FUNCTION_12VPULSE_NOW, true);

			// send the command
			// char[] result_array =
			this.uTransaction(this.uBuild);

			// check the result ??
			return true;
		} finally {

			// release local exclusive use of port
			this.endLocalExclusive();
		}
	}

	/**
	 * Sets the 1-Wire Network voltage to 0 volts. This method is used rob all
	 * 1-Wire Network devices of parasite power delivery to force them into a hard
	 * reset.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter or the
	 *                            adapter does not support this operation
	 */
	@Override
	public void startBreak() throws OneWireIOException, OneWireException {
		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// power down the 2480 (dropping the 1-Wire)
			this.serial.setDTR(false);
			this.serial.setRTS(false);

			// wait for power to drop
			this.sleep(200);

			// set the level state
			this.owState.oneWireLevel = LEVEL_BREAK;
		} finally {

			// release local exclusive use of port
			this.endLocalExclusive();
		}
	}

	/**
	 * Sets the 1-Wire Network voltage to normal level. This method is used to
	 * disable 1-Wire conditions created by startPowerDelivery and
	 * startProgramPulse. This method will automatically be called if a
	 * communication method is called while an outstanding power command is taking
	 * place.
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter or the
	 *                            adapter does not support this operation
	 */
	@Override
	public void setPowerNormal() throws OneWireIOException, OneWireException {
		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			if (this.owState.oneWireLevel == LEVEL_POWER_DELIVERY) {

				// make sure adapter is present
				if (this.uAdapterPresent()) {

					// flush out the com buffer
					this.serial.flush();

					// build a message to read the baud rate from the U brick
					this.uBuild.restart();

					// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
					// shughes - 8-28-2003
					// Fixed the Set Power Level Normal problem where adapter
					// is left in a bad state. Removed bad fix: extra getBit()
					// SEE BELOW!
					// stop pulse command
					this.uBuild.sendCommand(UPacketBuilder.FUNCTION_STOP_PULSE, true);

					// start pulse with no prime
					this.uBuild.sendCommand(UPacketBuilder.FUNCTION_5VPULSE_NOW, false);
					// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//

					// add the command to stop the pulse
					var pulse_response_offset = this.uBuild.sendCommand(UPacketBuilder.FUNCTION_STOP_PULSE, true);

					// send and receive
					var result_array = this.uTransaction(this.uBuild);

					// check the result
					if (result_array.length != pulse_response_offset + 1) {
						throw new OneWireIOException("Did not get a response back from stop power delivery");
					}
					this.owState.oneWireLevel = LEVEL_NORMAL;
				}
			} else if (this.owState.oneWireLevel == LEVEL_BREAK) {

				// restore power
				this.serial.setDTR(true);
				this.serial.setRTS(true);

				// wait for power to come up
				this.sleep(300);

				// set the level state
				this.owState.oneWireLevel = LEVEL_NORMAL;

				// set the DS2480 to the correct mode and verify
				this.adapterPresent = false;

				if (!this.uAdapterPresent()) {
					throw new OneWireIOException("Did not get a response back from adapter after break");
				}
			}
		} catch (IOException ioe) {
			throw new OneWireIOException(ioe.toString());
		} finally {

			// release local exclusive use of port
			this.endLocalExclusive();
		}
	}

	// --------
	// -------- OneWire bus speed methods
	// --------

	/**
	 * This method takes an int representing the new speed of data transfer on the
	 * 1-Wire Network.
	 * <p>
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
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter or the
	 *                            adapter does not support this operation
	 */
	@Override
	public void setSpeed(int speed) throws OneWireIOException, OneWireException {
		try {

			// acquire exclusive use of the port
			this.beginLocalExclusive();

			// check for valid speed
			if (speed != SPEED_REGULAR && speed != SPEED_OVERDRIVE && speed != SPEED_FLEX) {
				throw new OneWireException("Requested speed is not supported by this adapter");
			}
			// change 1-Wire speed
			this.owState.oneWireSpeed = (char) speed;

			// set adapter to communicate at this new speed (regular == flex for now)
			if (speed == SPEED_OVERDRIVE) {
				this.uState.uSpeedMode = UAdapterState.USPEED_OVERDRIVE;
			} else {
				this.uState.uSpeedMode = UAdapterState.USPEED_FLEX;
			}
		} finally {

			// release local exclusive use of port
			this.endLocalExclusive();
		}
	}

	/**
	 * This method returns the current data transfer speed through a port to a
	 * 1-Wire Network.
	 * <p>
	 *
	 * @return
	 *         <ul>
	 *         <li>0 (SPEED_REGULAR) set to normal communication speed
	 *         <li>1 (SPEED_FLEX) set to flexible communication speed used for long
	 *         lines
	 *         <li>2 (SPEED_OVERDRIVE) set to normal communication speed to
	 *         overdrive
	 *         <li>3 (SPEED_HYPERDRIVE) set to normal communication speed to
	 *         hyperdrive
	 *         <li>>3 future speeds
	 *         </ul>
	 */
	@Override
	public int getSpeed() {
		return this.owState.oneWireSpeed;
	}

	// --------
	// -------- Support methods
	// --------

	/**
	 * Perform a search using the oneWire state provided
	 *
	 * @param mState current OneWire state used to do the search
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 * @throws OneWireException   on a setup error with the 1-Wire adapter
	 */
	private boolean search(OneWireState mState) throws OneWireIOException, OneWireException {
		var reset_offset = 0;

		// make sure adapter is present
		if (!this.uAdapterPresent()) {
			throw new OneWireIOException("Error communicating with adapter");
		}
		// check for pending power conditions
		if (this.owState.oneWireLevel != LEVEL_NORMAL) {
			this.setPowerNormal();
		}

		// set the correct baud rate to stream this operation
		this.setStreamingSpeed(UPacketBuilder.OPERATION_SEARCH);

		// reset the packet
		this.uBuild.restart();

		// add a reset/ search command
		if (!mState.skipResetOnSearch) {
			reset_offset = this.uBuild.oneWireReset();
		}

		if (mState.searchOnlyAlarmingButtons) {
			this.uBuild.dataByte(ALARM_SEARCH_CMD);
		} else {
			this.uBuild.dataByte(NORMAL_SEARCH_CMD);
		}

		// add search sequence based on mState
		var search_offset = this.uBuild.search(mState);

		// send/receive the search
		var result_array = this.uTransaction(this.uBuild);

		// interpret search result and return
		if (!mState.skipResetOnSearch) {
			this.uBuild.interpretOneWireReset(result_array[reset_offset]);
		}

		return this.uBuild.interpretSearch(mState, result_array, search_offset);
	}

	/**
	 * Perform a 'strongAccess' with the provided 1-Wire address. 1-Wire Network has
	 * already been reset and the 'search' command sent before this is called.
	 *
	 * @param address   device address to do strongAccess on
	 * @param alarmOnly verify device is present and alarming if true
	 *
	 * @return true if device participated and was present in the strongAccess
	 *         search
	 */
	private boolean blockIsPresent(byte[] address, boolean alarmOnly) throws OneWireIOException, OneWireException {
		var send_packet = new byte[24];
		int i;

		// reset the 1-Wire
		this.reset();

		// send search command
		if (alarmOnly) {
			this.putByte(ALARM_SEARCH_CMD);
		} else {
			this.putByte(NORMAL_SEARCH_CMD);
		}

		// set all bits at first
		for (i = 0; i < 24; i++) {
			send_packet[i] = (byte) 0xFF;
		}

		// now set or clear appropriate bits for search
		for (i = 0; i < 64; i++) {
			Bit.arrayWriteBit(Bit.arrayReadBit(i, 0, address), (i + 1) * 3 - 1, 0, send_packet);
		}

		// send to 1-Wire Net
		this.dataBlock(send_packet, 0, 24);

		// check the results of last 8 triplets (should be no conflicts)
		int cnt = 56, goodbits = 0, tst, s;

		for (i = 168; i < 192; i += 3) {
			tst = Bit.arrayReadBit(i, 0, send_packet) << 1 | Bit.arrayReadBit(i + 1, 0, send_packet);
			s = Bit.arrayReadBit(cnt++, 0, address);

			if (tst == 0x03) // no device on line
			{
				goodbits = 0; // number of good bits set to zero

				break; // quit
			}

			if (s == 0x01 && tst == 0x02 || s == 0x00 && tst == 0x01) {
				goodbits++; // count as a good bit
			}
		}

		// check too see if there were enough good bits to be successful
		return goodbits >= 8;
	}

	// --------
	// -------- U Adapter Methods
	// --------

	/**
	 * set the correct baud rate to stream this operation
	 */
	private void setStreamingSpeed(int operation) throws OneWireIOException {

		// get the desired baud rate for this operation
		var baud = UPacketBuilder.getDesiredBaud(operation, this.owState.oneWireSpeed, maxBaud);

		// check if already at the correct speed
		if (baud == this.serial.getBaudRate()) {
			return;
		}

		if (doDebugMessages) {
			System.out.println("Changing baud rate from " + this.serial.getBaudRate() + " to " + baud);
		}

		// convert this baud to 'u' baud
		char ubaud;

		switch (baud) {

		case 115200:
			ubaud = UAdapterState.BAUD_115200;
			break;
		case 57600:
			ubaud = UAdapterState.BAUD_57600;
			break;
		case 19200:
			ubaud = UAdapterState.BAUD_19200;
			break;
		case 9600:
		default:
			ubaud = UAdapterState.BAUD_9600;
			break;
		}

		// see if this is a new baud
		if (ubaud == this.uState.ubaud) {
			return;
		}

		// default, loose communication with adapter
		this.adapterPresent = false;

		// build a message to read the baud rate from the U brick
		this.uBuild.restart();

		var baud_offset = this.uBuild.setParameter(UParameterSettings.PARAMETER_BAUDRATE, ubaud);

		try {
			// send command, no response at this baud rate
			this.serial.flush();

			var pkt = this.uBuild.getPackets().nextElement();
			var temp_buf = new char[pkt.buffer.length()];

			pkt.buffer.getChars(0, pkt.buffer.length(), temp_buf, 0);
			this.serial.write(temp_buf);

			// delay to let things settle
			this.sleep(5);
			this.serial.flush();

			// set the baud rate
			this.sleep(5); // solaris hack!!!
			this.serial.setBaudRate(baud);
		} catch (IOException ioe) {
			throw new OneWireIOException(ioe.toString());
		}

		this.uState.ubaud = ubaud;

		// delay to let things settle
		this.sleep(5);

		// verify adapter is at new baud rate
		this.uBuild.restart();

		baud_offset = this.uBuild.getParameter(UParameterSettings.PARAMETER_BAUDRATE);

		// set the DS2480 communication speed for subsequent blocks
		this.uBuild.setSpeed();

		try {

			// send and receive
			this.serial.flush();

			var result_array = this.uTransaction(this.uBuild);

			// check the result
			if (result_array.length == 1) {
				if ((result_array[baud_offset] & 0xF1) == 0
						&& (result_array[baud_offset] & 0x0E) == this.uState.ubaud) {
					if (doDebugMessages) {
						System.out.println("Success, baud changed and DS2480 is there");
					}

					// adapter still with us
					this.adapterPresent = true;

					// flush any garbage characters
					this.sleep(150);
					this.serial.flush();

					return;
				}
			}
		} catch (IOException ioe) {
			if (doDebugMessages) {
				System.err.println("USerialAdapter-setStreamingSpeed: " + ioe);
			}
		} catch (OneWireIOException e) {
			if (doDebugMessages) {
				System.err.println("USerialAdapter-setStreamingSpeed: " + e);
			}
		}

		if (doDebugMessages) {
			System.out.println("Failed to change baud of DS2480");
		}
	}

	/**
	 * Verify that the DS2480 based adapter is present on the open port.
	 *
	 * @return 'true' if adapter present
	 *
	 * @throws OneWireException - if port not selected
	 */
	private boolean uAdapterPresent() throws OneWireException {
		var rt = true;

		// check if adapter has already be verified to be present
		if (!this.adapterPresent) {

			// do a master reset
			this.uMasterReset();

			// attempt to verify
			if (!this.uVerify()) {

				// do a master reset and try again
				this.uMasterReset();

				if (!this.uVerify()) {

					// do a power reset and try again
					this.uPowerReset();

					if (!this.uVerify()) {
						rt = false;
					}
				}
			}
		}

		this.adapterPresent = rt;

		if (doDebugMessages) {
			System.out.println("DEBUG: AdapterPresent result: " + rt);
		}

		return rt;
	}

	/**
	 * Do a master reset on the DS2480. This reduces the baud rate to 9600 and
	 * peforms a break. A single timing byte is then sent.
	 */
	private void uMasterReset() {
		if (doDebugMessages) {
			System.out.println("DEBUG: uMasterReset");
		}

		// try to acquire the port
		try {

			// set the baud rate
			this.serial.setBaudRate(9600);

			this.uState.ubaud = UAdapterState.BAUD_9600;

			// put back to standard speed
			this.owState.oneWireSpeed = SPEED_REGULAR;
			this.uState.uSpeedMode = UAdapterState.USPEED_FLEX;
			this.uState.ubaud = UAdapterState.BAUD_9600;

			// send a break to reset DS2480
			this.serial.sendBreak(10);
			this.sleep(5);

			// send the timing byte
			this.serial.flush();
			this.serial.write(UPacketBuilder.FUNCTION_RESET);
			this.serial.flush();
		} catch (IOException e) {
			if (doDebugMessages) {
				System.err.println("USerialAdapter-uMasterReset: " + e);
			}
		}
	}

	/**
	 * Do a power reset on the DS2480. This reduces the baud rate to 9600 and powers
	 * down the DS2480. A single timing byte is then sent.
	 */
	private void uPowerReset() {
		if (doDebugMessages) {
			System.out.println("DEBUG: uPowerReset");
		}

		// try to acquire the port
		try {

			// set the baud rate
			this.serial.setBaudRate(9600);

			this.uState.ubaud = UAdapterState.BAUD_9600;

			// put back to standard speed
			this.owState.oneWireSpeed = SPEED_REGULAR;
			this.uState.uSpeedMode = UAdapterState.USPEED_FLEX;
			this.uState.ubaud = UAdapterState.BAUD_9600;

			// power down DS2480
			this.serial.setDTR(false);
			this.serial.setRTS(false);
			this.sleep(300);
			this.serial.setDTR(true);
			this.serial.setRTS(true);
			this.sleep(1);

			// send the timing byte
			this.serial.flush();
			this.serial.write(UPacketBuilder.FUNCTION_RESET);
			this.serial.flush();
		} catch (IOException e) {
			if (doDebugMessages) {
				System.err.println("USerialAdapter-uPowerReset: " + e);
			}
		}
	}

	/**
	 * Read and verify the baud rate with the DS2480 chip and perform a single bit
	 * MicroLAN operation. This is used as a DS2480 detect.
	 *
	 * @return 'true' if the correct baud rate and bit operation was read from the
	 *         DS2480
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 */
	private boolean uVerify() {
		try {
			this.serial.flush();

			// build a message to read the baud rate from the U brick
			this.uBuild.restart();

			this.uBuild.setParameter(UParameterSettings.PARAMETER_SLEW,
					this.uState.uParameters[this.owState.oneWireSpeed].pullDownSlewRate);
			this.uBuild.setParameter(UParameterSettings.PARAMETER_WRITE1LOW,
					this.uState.uParameters[this.owState.oneWireSpeed].write1LowTime);
			this.uBuild.setParameter(UParameterSettings.PARAMETER_SAMPLEOFFSET,
					this.uState.uParameters[this.owState.oneWireSpeed].sampleOffsetTime);
			this.uBuild.setParameter(UParameterSettings.PARAMETER_5VPULSE, UParameterSettings.TIME5V_infinite);
			var baud_offset = this.uBuild.getParameter(UParameterSettings.PARAMETER_BAUDRATE);
			var bit_offset = this.uBuild.dataBit(true, false);

			// send and receive
			var result_array = this.uTransaction(this.uBuild);

			// check the result
			if (result_array.length == bit_offset + 1) {
				if ((result_array[baud_offset] & 0xF1) == 0 && (result_array[baud_offset] & 0x0E) == this.uState.ubaud
						&& (result_array[bit_offset] & 0xF0) == 0x90
						&& (result_array[bit_offset] & 0x0C) == this.uState.uSpeedMode) {
					return true;
				}
			}
		} catch (IOException ioe) {
			if (doDebugMessages) {
				System.err.println("USerialAdapter-uVerify: " + ioe);
			}
		} catch (OneWireIOException e) {
			if (doDebugMessages) {
				System.err.println("USerialAdapter-uVerify: " + e);
			}
		}

		return false;
	}

	/**
	 * Write the raw U packet and then read the result.
	 *
	 * @param tempBuild the U Packet Build where the packet to send resides
	 *
	 * @return the result array
	 *
	 * @throws OneWireIOException on a 1-Wire communication error
	 */
	private char[] uTransaction(UPacketBuilder tempBuild) throws OneWireIOException {
		int offset;

		try {
			// clear the buffers
			this.serial.flush();
			this.inBuffer.setLength(0);

			// loop to send all of the packets
			for (var packet_enum = tempBuild.getPackets(); packet_enum.hasMoreElements();) {

				// get the next packet
				var pkt = packet_enum.nextElement();

				// bogus packet to indicate need to wait for long DS2480 alarm reset
				if (pkt.buffer.length() == 0 && pkt.returnLength == 0) {
					this.sleep(6);
					this.serial.flush();

					continue;
				}

				// get the data
				var temp_buf = new char[pkt.buffer.length()];

				pkt.buffer.getChars(0, pkt.buffer.length(), temp_buf, 0);

				// remember number of bytes in input
				offset = this.inBuffer.length();

				// send the packet
				this.serial.write(temp_buf);

				// wait on returnLength bytes in inBound
				this.inBuffer.append(this.serial.readWithTimeout(pkt.returnLength));
			}

			// read the return packet
			var ret_buffer = new char[this.inBuffer.length()];

			this.inBuffer.getChars(0, this.inBuffer.length(), ret_buffer, 0);

			// check for extra bytes in inBuffer
			var extraBytesReceived = this.inBuffer.length() > tempBuild.totalReturnLength;

			// clear the inbuffer
			this.inBuffer.setLength(0);

			return ret_buffer;
		} catch (IOException e) {

			// need to check on adapter
			this.adapterPresent = false;

			// pass it on
			throw new OneWireIOException(e.toString());
		}
	}

	/**
	 * Sleep for the specified number of milliseconds
	 */
	private void sleep(long msTime) {

		// provided debug on standard out
		if (doDebugMessages) {
			System.out.println("DEBUG: sleep(" + msTime + ")");
		}

		try {
			Thread.sleep(msTime);
		} catch (InterruptedException e) {
		}
	}

	// --------
	// -------- Static
	// --------

	/**
	 * Static method called before instance is created. Attempt to create a hash of
	 * SerialService's and get the max baud rate.
	 */
	static {

		/*
		 * // create a SerialServices instance for each port available and put in hash
		 * Enumeration com_enum = CommPortIdentifier.getPortIdentifiers();
		 * CommPortIdentifier port_id; SerialService serial_instance;
		 *
		 * // loop through all of the serial port elements while
		 * (com_enum.hasMoreElements()) {
		 *
		 * // get the next com port port_id = ( CommPortIdentifier )
		 * com_enum.nextElement();
		 *
		 * // only collect the names of the serial ports if (port_id.getPortType() ==
		 * CommPortIdentifier.PORT_SERIAL) { serial_instance = new
		 * SerialService(port_id.getName());
		 *
		 * serailServiceHash.put(port_id.getName(), serial_instance);
		 *
		 * if (doDebugMessages) System.out.println("DEBUG: Serial port: " +
		 * port_id.getName()); } }
		 */

		// check properties to see if max baud set manually
		maxBaud = 115200;

		var max_baud_str = OneWireAccessProvider.getProperty("onewire.serial.maxbaud");

		if (max_baud_str != null) {
			try {
				maxBaud = Integer.parseInt(max_baud_str);
			} catch (NumberFormatException e) {
				maxBaud = 0;
			}
		}

		// provided debug on standard out
		if (doDebugMessages) {
			System.out.println("DEBUG: getMaxBaud from properties: " + maxBaud);
		}

		// if not valid then use fastest
		if (maxBaud != 115200 && maxBaud != 57600 && maxBaud != 19200 && maxBaud != 9600) {
			maxBaud = 115200;
		}
	}
}
// CHECKSTYLE:ON
