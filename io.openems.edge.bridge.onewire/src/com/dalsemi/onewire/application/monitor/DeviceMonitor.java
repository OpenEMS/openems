// CHECKSTYLE:OFF
/*---------------------------------------------------------------------------
 * Copyright (C) 2002 Maxim Integrated Products, All Rights Reserved.
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
import com.dalsemi.onewire.utils.OWPath;

/**
 * <P>
 * Class DeviceMonitor represents the monitor that searches the 1-Wire net for
 * new arrivals. This monitor performs a simple search, meaning that no branches
 * are explicitly traversed. If a branch is activated/deactivated between search
 * cycles, this monitor will see the arrival/departure of new devices without
 * reference to the branch which they lie on.
 * </P>
 *
 * @author SH
 * @version 1.01
 */
public class DeviceMonitor extends AbstractDeviceMonitor {
	private OWPath defaultPath = null;

	private boolean doAlarmSearch = false;

	/**
	 * Create a simple monitor that does not search branches
	 *
	 * @param adapter the DSPortAdapter this monitor should search
	 */
	public DeviceMonitor(DSPortAdapter adapter) {
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
	 * Sets this monitor to search for alarming parts.
	 *
	 * @param findAlarmingParts
	 */
	public void setDoAlarmSearch(boolean findAlarmingParts) {
		synchronized (this.sync_flag) {
			this.doAlarmSearch = findAlarmingParts;
		}
	}

	/**
	 * See if Gets this monitor to search for alarming parts.
	 */
	public boolean getDoAlarmSearch() {
		return this.doAlarmSearch;
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
				// acquire the adapter
				this.adapter.beginExclusive(true);

				// setup the search
				this.adapter.setSearchAllDevices();
				this.adapter.targetAllFamilies();
				this.adapter.setSpeed(DSPortAdapter.SPEED_REGULAR);

				var search_result = this.adapter.findFirstDevice();

				// loop while devices found
				while (search_result) {
					// get the 1-Wire address
					var longAddress = Long.valueOf(this.adapter.getAddressAsLong());
					// if requested to do an alarm search, then check device for an alarm condition
					// and save in list
					if (this.doAlarmSearch) {
						if (this.adapter.isAlarming(longAddress.longValue())) {
							if (!this.deviceAddressHash.containsKey(longAddress) && arrivals != null) {
								arrivals.addElement(longAddress);
							}

							this.deviceAddressHash.put(longAddress, Integer.valueOf(this.max_state_count));
						}
					} else {
						if (!this.deviceAddressHash.containsKey(longAddress) && arrivals != null) {
							arrivals.addElement(longAddress);
						}

						this.deviceAddressHash.put(longAddress, Integer.valueOf(this.max_state_count));
					}

					// search for the next device
					search_result = this.adapter.findNextDevice();
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
}
// CHECKSTYLE:ON
