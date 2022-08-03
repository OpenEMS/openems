// CHECKSTYLE:OFF
/*---------------------------------------------------------------------------
 * Copyright (C) 2006 Maxim Integrated Products, All Rights Reserved.
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
package com.dalsemi.onewire.application.monitor;

import java.util.Vector;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.utils.Address;
import com.dalsemi.onewire.utils.OWPath;

/**
 * <P>
 * Class ChainMonitor represents the monitor that searches the 1-Wire net for
 * Chain Mode devices, otherwise known as "Sequence Detect". Chain Mode devices
 * have two PIO pins (PIOA and PIOB). You can chain multiple devices together by
 * connecting PIOA of one part to PIOB of the next device. The first device in
 * the chain has its PIOB grounded.
 *
 * This monitor performs a simple search. If a branch is activated/deactivated
 * between search cycles, this monitor will see the arrival/departure of new
 * devices without reference to the branch which they lie on.
 * </P>
 *
 * @author Maxim Integrated Products
 * @version 1.00
 */
public class ChainMonitor extends AbstractDeviceMonitor {
	private OWPath defaultPath = null;

	/**
	 * Create a simple monitor that does not search branches
	 *
	 * @param adapter the DSPortAdapter this monitor should search
	 */
	public ChainMonitor(DSPortAdapter adapter) {
		this.setAdapter(adapter);
	}

	/**
	 * Sets this monitor to search a new DSPortAdapter
	 *
	 * @param adapter the DSPortAdapter this monitor should search
	 */
	@Override
	public void setAdapter(DSPortAdapter adapter) {
		if (adapter == null) {
			throw new IllegalArgumentException("Adapter cannot be null");
		}

		synchronized (this.sync_flag) {
			this.adapter = adapter;
			this.defaultPath = new OWPath(adapter);

			this.resetSearch();
		}
	}

	/**
	 * Returns the OWPath of the device with the given address.
	 *
	 * @param address a Long object representing the address of the device
	 * @return The OWPath representing the network path to the device.
	 */
	@Override
	public OWPath getDevicePath(Long address) {
		return this.defaultPath;
	}

	/**
	 * chainOn sends the chain mode "ON" command sequence to all chain devices.
	 *
	 * @return true if successful, false otherwise
	 */
	public boolean chainOn() throws OneWireException, OneWireIOException {
		//
		// send chain on command
		//

		var returnResult = false;
		this.adapter.beginExclusive(true);
		// master issues a reset
		this.adapter.reset();
		// master issues a Skip Rom [0xCC]
		this.adapter.putByte(0xCC);
		// master issues Chain Mode command [0x99]
		this.adapter.putByte(0x99);
		// master issues chain control byte ON [0x5A]
		this.adapter.putByte(0x5A);
		// master issues inverse of chain control byte ON
		this.adapter.putByte(~0x5A);
		// master receives response: 0xAA success, or 0x00 error
		if (this.adapter.getByte() != 0xAA) {
			this.adapter.endExclusive();
			return returnResult;
		}
		returnResult = true; // successful transmission of chain "ON" command
		this.adapter.endExclusive();
		return returnResult;
	}

	/**
	 * chainConditionalReadRom sends the chain mode "DONE" command sequence to
	 * current chain device.
	 *
	 * @param chainDeviceAddress 8-byte array for chain 1-Wire net address
	 * @return true if successful, false otherwise
	 */
	public boolean chainConditionalReadRom(byte[] chainDeviceAddress) throws OneWireException, OneWireIOException {
		//
		// send chain Conditional Read ROM command sequence
		//

		// initialize 1-Wire net address buffer to zero
		for (var i = 0; i < 8; i++) {
			chainDeviceAddress[i] = 0;
		}

		var returnResult = false;
		// acquire adapter
		this.adapter.beginExclusive(true);
		// master transmits a reset
		this.adapter.reset();
		// master issues a Conditional Read ROM [0x0F]
		this.adapter.putByte(0x0F);
		// master receives 8 bytes of 1-Wire network address
		for (var i = 0; i < 8; i++) {
			chainDeviceAddress[i] = (byte) this.adapter.getByte();
		}
		this.adapter.endExclusive();
		if (chainDeviceAddress[0] == (byte) 0x81) {
			throw new OneWireException(
					"This adapter's DS2401 ID chip interferes with chain mode.  Try another adapter.");
		}

		// check for error (if serial number is not a bunch of 0xFFs)
		if (chainDeviceAddress[0] == (byte) 0xFF && chainDeviceAddress[7] == (byte) 0xFF) {
			// error, so return false
			return returnResult;
		}
		return true;
	}

	/**
	 * chainDone sends the chain mode "DONE" command sequence to current chain
	 * device.
	 *
	 * @return true if successful, false otherwise
	 */
	public boolean chainDone() throws OneWireException, OneWireIOException {
		//
		// send chain done command
		//

		var returnResult = false;
		this.adapter.beginExclusive(true);
		// master issues reset
		this.adapter.reset();
		// master issues Resume command [0xA5]
		this.adapter.putByte(0xA5);
		// master issues Chain Mode command [0x99]
		this.adapter.putByte(0x99);
		// master issues chain control byte DONE [0x96]
		this.adapter.putByte(0x96);
		// master issues inverse of chain control byte DONE
		this.adapter.putByte(~0x96);
		// master receives response: 0xAA success, or 0x00 error
		if (this.adapter.getByte() != 0xAA) {
			this.adapter.endExclusive();
			return returnResult;
		}
		returnResult = true; // successful transmission of chain "DONE" command
		this.adapter.endExclusive();
		return returnResult;
	}

	/**
	 * chainOff sends the chain mode "OFF" command sequence to all chain devices.
	 *
	 * @return true if successful, false otherwise
	 */
	public boolean chainOff() throws OneWireException, OneWireIOException {
		//
		// send chain off command
		//

		var returnResult = false;
		this.adapter.beginExclusive(true);
		// master issues reset
		this.adapter.reset();
		// master issues a skip rom command [0xCC]
		this.adapter.putByte(0xCC);
		// master issues Chain Mode command [0x99]
		this.adapter.putByte(0x99);
		// master issues chain control byte OFF [0x3C]
		this.adapter.putByte(0x3C);
		// master issues inverse of chain control byte OFF
		this.adapter.putByte(~0x3C); // bitwise complement of 0x3C
		// master receives response: 0xAA success, or 0x00 error
		if (this.adapter.getByte() != 0xAA) {
			this.adapter.endExclusive();
			return returnResult;
		}
		returnResult = true; // successful transmission of chain "OFF" command
		this.adapter.endExclusive();
		return returnResult;
	}

	/**
	 * Performs a search of the 1-Wire network without searching branches
	 *
	 * @param arrivals   A vector of Long objects, represent new arrival addresses.
	 * @param departures A vector of Long objects, represent departed addresses.
	 */
	@Override
	public void search(Vector<Long> arrivals, Vector<Long> departures) throws OneWireException, OneWireIOException {
		synchronized (this.sync_flag) {
			try {
				var chainDeviceAddress = new byte[8];
				for (var i = 0; i < 8; i++) {
					chainDeviceAddress[i] = 0;
				}

				// get exclusive use of 1-Wire network
				this.adapter.beginExclusive(true);

				// clear any previous search restrictions
				this.adapter.setSearchAllDevices();
				this.adapter.targetAllFamilies();
				this.adapter.setSpeed(DSPortAdapter.SPEED_REGULAR);
				this.adapter.endExclusive();

				// perform chain mode search
				// send chain mode "ON" sequence
				if (this.chainOn()) {
					// loop on sending chain mode "Conditional Read ROM" until method returns false
					while (this.chainConditionalReadRom(chainDeviceAddress)) {
						var longAddress = Long.valueOf(Address.toLong(chainDeviceAddress));
						if (!this.deviceAddressHash.containsKey(longAddress) && arrivals != null) {
							arrivals.addElement(longAddress);
						}

						this.deviceAddressHash.put(longAddress, Integer.valueOf(this.max_state_count));
						// send chain mode "DONE" sequence
						if (!this.chainDone()) {
							throw new OneWireIOException(
									"Transmission error occurred attempting to send chain control byte DONE.");
						}
					}
					// send chain mode "OFF" sequence
					if (!this.chainOff()) {
						throw new OneWireIOException(
								"Transmission error occurred attempting to send chain control byte OFF.");
					}
				}
			} finally {
				this.adapter.endExclusive();
			}

			// remove any devices that have not been seen
			for (var device_enum = this.deviceAddressHash.keys(); device_enum.hasMoreElements();) {
				var longAddress = device_enum.nextElement();

				// check for removal by looking at state counter
				var cnt = this.deviceAddressHash.get(longAddress).intValue();
				if (cnt <= 0) {
					this.deviceAddressHash.remove(longAddress);
					if (departures != null) {
						departures.addElement(longAddress);
					}

					synchronized (deviceContainerHash) {
						deviceContainerHash.remove(longAddress);
					}
				} else {
					// it stays
					this.deviceAddressHash.put(longAddress, Integer.valueOf(cnt - 1));
				}
			}

			// fire notification events
			if (arrivals != null && arrivals.size() > 0) {
				this.fireArrivalEvent(this.adapter, arrivals);
			}
			if (departures != null && departures.size() > 0) {
				this.fireDepartureEvent(this.adapter, departures);
			}
		}
	}

	/**
	 * A helper method that takes the arrivals Vector from the search method and
	 * returns a Vector of OneWireContainers
	 *
	 * @param arrivals A vector of Long objects, represent new arrival addresses.
	 * @return A vector of OneWireContainer objects
	 */
	public Vector<OneWireContainer> toContainerVector(Vector<Long> arrivals)
			throws OneWireException, OneWireIOException {
		OneWireContainer owd;
		Long OneWireAddress;
		var containerVector = new Vector<OneWireContainer>();

		// loop through all the 1-Wire devices found in arrivals list
		this.adapter.beginExclusive(true);
		for (var i = 0; i < arrivals.size(); i++) {
			OneWireAddress = arrivals.elementAt(i);
			owd = this.adapter.getDeviceContainer(OneWireAddress.longValue());
			containerVector.addElement(owd);
		}
		this.adapter.endExclusive();
		return containerVector;
	}
}
// CHECKSTYLE:ON
