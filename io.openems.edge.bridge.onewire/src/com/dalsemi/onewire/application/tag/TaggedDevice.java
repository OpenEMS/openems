// CHECKSTYLE:OFF

/*---------------------------------------------------------------------------
 * Copyright (C) 1999-2001 Maxim Integrated Products, All Rights Reserved.
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

package com.dalsemi.onewire.application.tag;

import java.util.Vector;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.utils.OWPath;

/**
 * This class provides a default object for a tagged 1-Wire device.
 */
public class TaggedDevice {

	/**
	 * Creates an object for the device with the supplied address and device type
	 * connected to the supplied port adapter.
	 *
	 * @param adapter    The adapter serving the sensor.
	 * @param netAddress The 1-Wire network address of the sensor.
	 */
	public TaggedDevice(DSPortAdapter adapter, String netAddress) {
		this.DeviceContainer = adapter.getDeviceContainer(netAddress);
	}

	/**
	 * Creates an object for the device.
	 */
	public TaggedDevice() {
	}

	/* ********* Setters for this object *********** */

	/**
	 * Sets the 1-Wire Container for the tagged device.
	 */
	public void setDeviceContainer(DSPortAdapter adapter, String netAddress) {
		this.DeviceContainer = adapter.getDeviceContainer(netAddress);
	}

	/**
	 * Sets the device type for the tagged device.
	 *
	 * @param tType
	 */
	public void setDeviceType(String tType) {
		this.DeviceType = tType;
	}

	/**
	 * Sets the label for the tagged device.
	 *
	 * @param Label
	 */
	public void setLabel(String Label) {
		this.label = Label;
	}

	/**
	 * Sets the channel for the tagged device from a String.
	 *
	 * @param Channel
	 */
	public void setChannelFromString(String Channel) {
		this.channel = Integer.valueOf(Channel);
	}

	/**
	 * Sets the channel for the tagged device from an int.
	 *
	 * @param Channel
	 */
	public void setChannel(int channel) {
		this.channel = Integer.valueOf(channel);
	}

	/**
	 * Sets the init (initialization String) for the tagged device.
	 *
	 * @param init
	 */
	public void setInit(String init) {
		this.init = init;
	}

	/**
	 * Sets the cluster name for the tagged device.
	 *
	 * @param cluster
	 */
	public void setClusterName(String cluster) {
		this.clusterName = cluster;
	}

	/**
	 * Sets the vector of branches to get to the tagged device.
	 *
	 * @param branches
	 */
	public void setBranches(Vector<TaggedDevice> branches) {
		this.branchVector = branches;
	}

	/**
	 * Sets the OWPath for the tagged device. An OWPath is a description of how to
	 * physically get to a 1-Wire device through a set of nested 1-Wire switches.
	 *
	 * @param branchOWPath
	 */
	public void setOWPath(OWPath branchOWPath) {
		this.branchPath = branchOWPath;
	}

	/**
	 * Sets the OWPath for the tagged device. An OWPath is a description of how to
	 * physically get to a 1-Wire device through a set of nested 1-Wire switches.
	 *
	 * @param adapter
	 * @param Branches
	 */
	public void setOWPath(DSPortAdapter adapter, Vector<TaggedDevice> Branches) {
		this.branchPath = new OWPath(adapter);

		TaggedDevice TDevice;

		for (var i = 0; i < Branches.size(); i++) {
			TDevice = Branches.elementAt(i);

			this.branchPath.add(TDevice.getDeviceContainer(), TDevice.getChannel());
		}
	}

	/* ********* Getters for this object *********** */

	/**
	 * Gets the 1-Wire Container for the tagged device.
	 *
	 * @return The 1-Wire container for the tagged device.
	 */
	public OneWireContainer getDeviceContainer() {
		return this.DeviceContainer;
	}

	/**
	 * Gets the device type for the tagged device.
	 *
	 * @return The device type for the tagged device.
	 */
	public String getDeviceType() {
		return this.DeviceType;
	}

	/**
	 * Gets the label for the tagged device.
	 *
	 * @return The label for the tagged device.
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * Gets the channel for the tagged device as a String.
	 *
	 * @return The channel for the tagged device as a String.
	 */
	public String getChannelAsString() {
		return this.channel.toString();
	}

	/**
	 * Gets the channel for the tagged device as an int.
	 *
	 * @return The channel for the tagged device as an int.
	 */
	public int getChannel() {
		return this.channel.intValue();
	}

	/**
	 * Gets the init (Initialization String) for the tagged device.
	 *
	 * @return String init (Initialization String)
	 */
	public String getInit() {
		return this.init;
	}

	/**
	 * Gets the max string for the tagged device.
	 *
	 * @return String Gets the max string
	 */
	public String getMax() {
		return this.max;
	}

	/**
	 * Gets the min string for the tagged device.
	 *
	 * @return String Gets the min string
	 */
	public String getMin() {
		return this.min;
	}

	/**
	 * Gets the cluster name for the tagged device.
	 *
	 * @return The cluster name for the tagged device.
	 */
	public String getClusterName() {
		return this.clusterName;
	}

	/**
	 * Gets a vector of branches (to get to) the tagged device.
	 *
	 * @return The vector of branches to get to the tagged device.
	 */
	public Vector<TaggedDevice> getBranches() {
		return this.branchVector;
	}

	/**
	 * Gets the OWPath for the tagged device. An OWPath is a description of how to
	 * physically get to a 1-Wire device through a set of nested 1-Wire switches.
	 *
	 * @return The OWPath for the tagged device.
	 */
	public OWPath getOWPath() {
		return this.branchPath;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (o instanceof TaggedDevice) {
			var td = (TaggedDevice) o;
			return td.DeviceContainer.equals(this.DeviceContainer) && td.DeviceType.equals(this.DeviceType)
					&& td.min.equals(this.min) && td.max.equals(this.max) && td.init.equals(this.init)
					&& td.clusterName.equals(this.clusterName) && td.label.equals(this.label);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (this.getDeviceContainer().toString() + this.getLabel()).hashCode();
	}

	@Override
	public String toString() {
		return this.getLabel();
	}

	/** ********* Properties (fields) for this object ********** */

	/**
	 * 1-Wire Container for the tagged device.
	 */
	public OneWireContainer DeviceContainer;

	/**
	 * Device type for the device (i.e., contact, switch, d2a, etc.).
	 */
	public String DeviceType;

	/**
	 * Label for the "name" of the device.
	 */
	public String label;

	/**
	 * The channel on which to probe for info.
	 */
	public Integer channel;

	/**
	 * A string message representing a high or maximum value.
	 */
	public String max;

	/**
	 * A string message representing a low or minimum value.
	 */
	public String min;

	/**
	 * A true or false describing the state of the tagged device.
	 */
	public Boolean state;

	/**
	 * An initialization parameter for the tagged device.
	 */
	public String init;

	/**
	 * The name of the cluster to which the tagged device is associated. Nested
	 * clusters will have a forward slash ("/") between each cluster, much like a
	 * path.
	 */
	public String clusterName;

	/**
	 * A Vector of branches describing how to physically get to the tagged device
	 * through a set of 1-Wire switches.
	 */
	public Vector<TaggedDevice> branchVector;

	/**
	 * This is an OWPath describing how to physically get to the tagged device
	 * through a set of nested 1-Wire branches (switches).
	 */
	private OWPath branchPath;
}
// CHECKSTYLE:ON
