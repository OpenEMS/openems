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
import com.dalsemi.onewire.container.PotentiometerContainer;

/**
 * This class provides a default object for the D2A type of a tagged 1-Wire
 * device.
 */
public class D2A extends TaggedDevice implements TaggedActuator {
	/**
	 * Creates an object for the device.
	 */
	public D2A() {
		this.ActuatorSelections = new Vector<>();
	}

	/**
	 * Creates an object for the device with the supplied address connected to the
	 * supplied port adapter.
	 *
	 * @param adapter    The adapter serving the actuator.
	 * @param netAddress The 1-Wire network address of the actuator.
	 */
	public D2A(DSPortAdapter adapter, String netAddress) {
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
	 * @param selection The selection string.
	 * @throws OneWireException
	 */
	@Override
	public void setSelection(String selection) throws OneWireException {
		var pc = (PotentiometerContainer) this.getDeviceContainer();
		var Index = 0;
		Index = this.ActuatorSelections.indexOf(selection);
		// must first read the device
		var state = pc.readDevice();
		// set current wiper number from xml tag "channel"
		pc.setCurrentWiperNumber(this.getChannel(), state);
		// now, write to device to set the wiper number
		pc.writeDevice(state);

		if (Index > -1) // means selection is in the vector
		{
			// write wiper position to part
			state = pc.readDevice(); // read it first
			pc.setWiperPosition(Index); // set wiper position in state variable
			pc.writeDevice(state);
		}
	}

	// Selections for the D2A actuator:
	// element 0 -> Means change to the first wiper position.
	//
	// element 1 -> Means change to the second wiper position.
	//
	// .
	// .
	// .
	// last element 255? -> Means change to the last wiper position.

	/**
	 * Initializes the actuator
	 *
	 * @throws OneWireException
	 */
	@Override
	public void initActuator() throws OneWireException {
		var pc = (PotentiometerContainer) this.getDeviceContainer();
		int numOfWiperSettings;
		int resistance;
		var offset = 0.6; // this seems about right...
		double wiperResistance;
		String selectionString;
		// initialize the ActuatorSelections Vector
		// must first read the device
		var state = pc.readDevice();
		// set current wiper number from xml tag "channel"
		pc.setCurrentWiperNumber(this.getChannel(), state);
		// now, write to device to set the wiper number
		pc.writeDevice(state);
		// now, extract some values to initialize the ActuatorSelections
		// get the number of wiper positions
		numOfWiperSettings = pc.numberOfWiperSettings(state);
		// get the resistance value in k-Ohms
		resistance = pc.potentiometerResistance(state);
		// calculate wiper resistance
		wiperResistance = (resistance - offset) / numOfWiperSettings;
		// add the values to the ActuatorSelections Vector
		selectionString = resistance + " k-Ohms"; // make sure the first
		this.ActuatorSelections.addElement(selectionString); // element is the entire resistance
		for (var i = numOfWiperSettings - 2; i > -1; i--) {
			var newWiperResistance = wiperResistance * i;
			// round the values before putting them in drop-down list
			var roundedWiperResistance = (int) ((newWiperResistance + offset) * 10000);
			selectionString = roundedWiperResistance / 10000.0 + " k-Ohms";
			this.ActuatorSelections.addElement(selectionString);
		}
	}

	/**
	 * Keeps the selections of this actuator
	 */
	private final Vector<String> ActuatorSelections;
}
// CHECKSTYLE:ON
