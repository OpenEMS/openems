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

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.SwitchContainer;

/**
 * This class provides a default object for the Switch type of a tagged 1-Wire
 * device.
 */
public class Switch extends TaggedDevice implements TaggedActuator {
	/**
	 * Creates an object for the device.
	 */
	public Switch() {
		this.ActuatorSelections = new Vector<>();
	}

	/**
	 * Creates an object for the device with the supplied address connected to the
	 * supplied port adapter.
	 *
	 * @param adapter    The adapter serving the actuator.
	 * @param netAddress The 1-Wire network address of the actuator.
	 */
	public Switch(DSPortAdapter adapter, String netAddress) {
		super(adapter, netAddress);
		this.ActuatorSelections = new Vector<>();
	}

	/**
	 * Get the possible selection states of this actuator
	 *
	 * @return Vector of Strings representing selection states.
	 */
	@Override
	public Vector<String> getSelections() {
		return this.ActuatorSelections;
	}

	/**
	 * Set the selection of this actuator
	 *
	 * @throws OneWireException
	 *
	 */
	@Override
	public void setSelection(String selection) throws OneWireException {
		var switchcontainer = (SwitchContainer) this.getDeviceContainer();
		var Index = 0;
		var channelValue = this.getChannel();
		Index = this.ActuatorSelections.indexOf(selection);
		var switch_state = false;

		if (Index > -1) // means selection is in the vector
		{
			// initialize switch-state variable
			if (Index > 0) {
				switch_state = true;
			}
			// write to the device (but, read it first to get state)
			var state = switchcontainer.readDevice();
			// set the switch's state to the value specified
			switchcontainer.setLatchState(channelValue, switch_state, false, state);
			switchcontainer.writeDevice(state);
		}
	}

	// Selections for the Switch actuator:
	// element 0 -> Means "disconnected" or "open circuit" (init = 0) and is
	// associated with the "min" message.
	// element 1 -> Means "connect" or "close the circuit", (init = 1) and is
	// associated with the "max" message.

	/**
	 * Initializes the actuator
	 *
	 * @throws OneWireException
	 *
	 */
	@Override
	public void initActuator() throws OneWireException {
		var switchcontainer = (SwitchContainer) this.getDeviceContainer();
		// initialize the ActuatorSelections Vector
		this.ActuatorSelections.addElement(this.getMin()); // for switch, use min and max
		this.ActuatorSelections.addElement(this.getMax());
		// Now, initialize the switch to the desired condition.
		// This condition is in the <init> tag and, of course, the
		// <channel> tag is also needed to know which channel to
		// to open or close.
		int initValue;
		int channelValue;
		var switchStateIntValue = 0;
		Integer init = Integer.parseInt(this.getInit());
		initValue = init.intValue();
		channelValue = this.getChannel();

		var state = switchcontainer.readDevice();
		var switch_state = switchcontainer.getLatchState(channelValue, state);
		if (switch_state) {
			switchStateIntValue = 1;
		} else {
			switchStateIntValue = 0;
		}
		if (initValue != switchStateIntValue) {
			// set the switch's state to the value specified in XML file
			switchcontainer.setLatchState(channelValue, !switch_state, false, state);
			switchcontainer.writeDevice(state);
		}
	}

	/**
	 * Keeps the selections of this actuator
	 */
	private final Vector<String> ActuatorSelections;
}
// CHECKSTYLE:ON
