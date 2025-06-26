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

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;

/**
 * This class provides a default object for the Contact type of a tagged 1-Wire
 * device.
 */
public class Contact extends TaggedDevice implements TaggedSensor {

	/**
	 * Creates an object for the device.
	 */
	public Contact() {
	}

	/**
	 * Creates an object for the device with the supplied address and device type
	 * connected to the supplied port adapter.
	 *
	 * @param adapter    The adapter serving the sensor.
	 * @param netAddress The 1-Wire network address of the sensor.
	 */
	public Contact(DSPortAdapter adapter, String netAddress) {
		super(adapter, netAddress);
	}

	/**
	 * The readSensor method returns the "max" string if the Sensor is present or
	 * the "min" string if the Sensor is not present.
	 *
	 * @return The "max" string if sensor is present or "min" string if not.
	 */
	@Override
	public String readSensor() throws OneWireException {
		var returnString = "";

		if (this.DeviceContainer.isPresent()) {
			returnString = this.max;
		} else {
			returnString = this.min;
		}
		return returnString;
	}
}
// CHECKSTYLE:ON
