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

import java.util.Hashtable;
import java.util.Vector;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.container.SwitchContainer;
import com.dalsemi.onewire.utils.OWPath;

/**
 * Class NetworkDeviceMonitor represents the monitor that searches the 1-Wire
 * net, including the traversal of branches, looing for new arrivals and
 * departures.
 *
 * @author SH
 * @version 1.00
 */
public class NetworkDeviceMonitor extends AbstractDeviceMonitor {
	/** hashtable for holding the OWPath objects for each device container. */
	protected final Hashtable<Long, OWPath> devicePathHash = new Hashtable<>();
	/** A vector of paths, or branches, to search */
	protected Vector<OWPath> paths = null;
	/** indicates whether or not branches are automatically traversed */
	protected boolean branchAutoSearching = true;

	/**
	 * Create a complex monitor that does search branches
	 *
	 * @param adapter the DSPortAdapter this monitor should search
	 */
	public NetworkDeviceMonitor(DSPortAdapter adapter) {
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

			if (this.paths == null) {
				this.paths = new Vector<>();
			} else {
				this.paths.setSize(0);
			}
			this.paths.addElement(new OWPath(adapter));

			this.resetSearch();
		}
	}

	/**
	 * Indicates whether or not branches are automatically traversed. If false, new
	 * branches must be indicated using the "addBranch" method.
	 *
	 * @param enabled if true, all branches are automatically traversed during a
	 *                search operation.
	 */
	public void setBranchAutoSearching(boolean enabled) {
		this.branchAutoSearching = enabled;
	}

	/**
	 * Indicates whether or not branches are automatically traversed. If false, new
	 * branches must be indicated using the "addBranch" method.
	 *
	 * @return true if all branches are automatically traversed during a search
	 *         operation.
	 */
	public boolean getBranchAutoSearching() {
		return this.branchAutoSearching;
	}

	/**
	 * Adds a branch for searching. Must be used to traverse branches if
	 * auto-searching is disabled.
	 *
	 * @param path A branch to be searched during the next search routine
	 */
	public void addBranch(OWPath path) {
		this.paths.addElement(path);
	}

	/**
	 * Returns the OWPath of the device with the given address.
	 *
	 * @param address a Long object representing the address of the device
	 * @return The OWPath representing the network path to the device.
	 */
	@Override
	public OWPath getDevicePath(Long address) {
		synchronized (this.devicePathHash) {
			return this.devicePathHash.get(address);
		}
	}

	/**
	 * The device monitor will internally cache OWPath objects for each 1-Wire
	 * device. Use this method to clean up all stale OWPath objects. A stale path
	 * object is a OWPath which references a branching path to a 1-Wire device
	 * address which has not been seen by a recent search. This will be essential in
	 * a touch-contact environment which could run for some time and needs to
	 * conserve memory.
	 */
	@Override
	public void cleanUpStalePathReferences() {
		synchronized (this.devicePathHash) {
			var e = this.devicePathHash.keys();
			while (e.hasMoreElements()) {
				Object o = e.nextElement();
				if (!this.deviceAddressHash.containsKey(o)) {
					this.devicePathHash.remove(o);
				}
			}
		}
	}

	/**
	 * Performs a search of the 1-Wire network, with branch searching
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

				// close any opened branches
				for (var j = 0; j < this.paths.size(); j++) {
					try {
						this.paths.elementAt(j).close();
					} catch (Exception e) {

					}
				}

				// search through all of the paths
				for (var i = 0; i < this.paths.size(); i++) {
					// set searches to not use reset
					this.adapter.setNoResetSearch();

					// find the first device on this branch
					var search_result = false;
					var path = this.paths.elementAt(i);
					try {
						// try to open the current path
						path.open();
					} catch (Exception e) {
						// if opening the path failed, continue on to the next path
						continue;
					}

					search_result = this.adapter.findFirstDevice();

					// loop while devices found
					while (search_result) {
						// get the 1-Wire address
						var longAddress = Long.valueOf(this.adapter.getAddressAsLong());
						// check if the device already exists in our hashtable
						if (!this.deviceAddressHash.containsKey(longAddress)) {
							var owc = getDeviceContainer(this.adapter, longAddress);
							// check to see if it's a switch and if we are supposed
							// to automatically search down branches
							if (this.branchAutoSearching && owc instanceof SwitchContainer) {
								var sc = (SwitchContainer) owc;
								var state = sc.readDevice();
								for (var j = 0; j < sc.getNumberChannels(state); j++) {
									var tmp = new OWPath(this.adapter, path);
									tmp.add(owc, j);
									if (!this.paths.contains(tmp)) {
										this.paths.addElement(tmp);
									}
								}
							}

							synchronized (this.devicePathHash) {
								this.devicePathHash.put(longAddress, path);
							}
							if (arrivals != null) {
								arrivals.addElement(longAddress);
							}
						}
						// check if the existing device moved
						else if (!path.equals(this.devicePathHash.get(longAddress))) {
							synchronized (this.devicePathHash) {
								this.devicePathHash.put(longAddress, path);
							}
							if (departures != null) {
								departures.addElement(longAddress);
							}
							if (arrivals != null) {
								arrivals.addElement(longAddress);
							}
						}

						// update count
						this.deviceAddressHash.put(longAddress, Integer.valueOf(this.max_state_count));

						// find the next device on this branch
						path.open();
						search_result = this.adapter.findNextDevice();
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
					// device entry is stale, should be removed
					this.deviceAddressHash.remove(longAddress);
					if (departures != null) {
						departures.addElement(longAddress);
					}
				} else {
					// device entry isn't stale, it stays
					this.deviceAddressHash.put(longAddress, Integer.valueOf(cnt - 1));
				}
			}

			// fire notification events
			if (departures != null && departures.size() > 0) {
				this.fireDepartureEvent(this.adapter, departures);
			}
			if (arrivals != null && arrivals.size() > 0) {
				this.fireArrivalEvent(this.adapter, arrivals);
			}
		}
	}

}
// CHECKSTYLE:ON
