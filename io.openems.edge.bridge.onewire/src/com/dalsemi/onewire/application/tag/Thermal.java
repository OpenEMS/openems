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

package com.dalsemi.onewire.application.tag;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.TemperatureContainer;

/**
 * This class provides a default object for the Thermal type of a tagged 1-Wire
 * device.
 */
public class Thermal extends TaggedDevice implements TaggedSensor {

	/**
	 * Creates an object for the device.
	 */
	public Thermal() {
	}

	/**
	 * Creates an object for the device with the supplied address and device type
	 * connected to the supplied port adapter.
	 *
	 * @param adapter    The adapter serving the sensor.
	 * @param netAddress The 1-Wire network address of the sensor.
	 */
	public Thermal(DSPortAdapter adapter, String netAddress) {
		super(adapter, netAddress);
	}

	/**
	 * The readSensor method returns a temperature in degrees Celsius
	 *
	 * @param--none.
	 *
	 *               @return String temperature in degrees Celsius
	 */
	@Override
	public String readSensor() throws OneWireException {
		double theTemperature;
		var tc = (TemperatureContainer) this.DeviceContainer;

		// read the device first before getting the temperature
		var state = tc.readDevice();

		// perform a temperature conversion
		tc.doTemperatureConvert(state);

		// read the result of the conversion
		state = tc.readDevice();

		// extract the result out of state
		theTemperature = tc.getTemperature(state);
		// theTemperature = (double)(Math.round(theTemperature * 100))/100; // avoid
		// Math for TINI?
		theTemperature = this.roundDouble(theTemperature * 100) / 100;
		return theTemperature + " C";
	}

	/**
	 * The roundDouble method returns a double rounded to the nearest digit in the
	 * "ones" position.
	 *
	 * @param--double
	 *
	 * @return double rounded to the nearest digit in the "ones" position.
	 */
	private double roundDouble(double d) {
		return (int) (d + (d > 0 ? 0.5 : -0.5));
	}
}
// CHECKSTYLE:ON
